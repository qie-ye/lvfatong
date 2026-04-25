package com.lvatong.lft.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncTaskConsumer {

    private static final String STREAM_KEY  = AsyncTaskProducer.STREAM_KEY;
    private static final String GROUP_NAME  = "lvatong-consumer-group";
    private static final String CONSUMER_NAME = "consumer-1";

    private final StringRedisTemplate stringRedisTemplate;
    private final AsyncTaskRouter asyncTaskRouter;

    /**
     * 应用就绪后：创建消费者组（已存在则忽略），然后回放 PEL（宕机未完成任务）
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        initConsumerGroup();
        replayPending();
    }

    private void initConsumerGroup() {
        try {
            stringRedisTemplate.opsForStream()
                    .createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP_NAME);
            log.info("[AsyncTask] Consumer group '{}' created on stream '{}'", GROUP_NAME, STREAM_KEY);
        } catch (Exception e) {
            log.debug("[AsyncTask] Consumer group '{}' already exists ({})", GROUP_NAME, e.getMessage());
        }
    }

    /**
     * 回放 PEL：将上次宕机时已投递但未 XACK 的消息重新执行
     */
    private void replayPending() {
        List<MapRecord<String, Object, Object>> pending = readRecords(ReadOffset.from("0"));
        if (pending != null && !pending.isEmpty()) {
            log.info("[AsyncTask] Replaying {} PEL message(s)", pending.size());
            processRecords(pending);
        }
    }

    /**
     * 每秒轮询新消息（XREADGROUP … > ）
     */
    @Scheduled(fixedDelay = 1000)
    public void consume() {
        List<MapRecord<String, Object, Object>> records = readRecords(ReadOffset.lastConsumed());
        processRecords(records);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<MapRecord<String, Object, Object>> readRecords(ReadOffset offset) {
        try {
            return (List<MapRecord<String, Object, Object>>) (List)
                    stringRedisTemplate.opsForStream().read(
                            Consumer.from(GROUP_NAME, CONSUMER_NAME),
                            StreamReadOptions.empty().count(10),
                            StreamOffset.create(STREAM_KEY, offset));
        } catch (Exception e) {
            log.debug("[AsyncTask] Stream read skipped: {}", e.getMessage());
            return List.of();
        }
    }

    private void processRecords(List<MapRecord<String, Object, Object>> records) {
        if (records == null || records.isEmpty()) return;
        for (MapRecord<String, Object, Object> record : records) {
            try {
                Map<Object, Object> fields = record.getValue();
                String typeStr  = fields.get("type").toString();
                Long entityId   = Long.parseLong(fields.get("entityId").toString());
                Long userId     = Long.parseLong(fields.get("userId").toString());

                AsyncTaskMessage message = new AsyncTaskMessage(
                        AsyncTaskMessage.TaskType.valueOf(typeStr), entityId, userId);

                log.info("[AsyncTask] Processing streamId={} type={} entityId={}",
                        record.getId(), typeStr, entityId);
                asyncTaskRouter.route(message);

                stringRedisTemplate.opsForStream()
                        .acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
                log.info("[AsyncTask] Acknowledged streamId={}", record.getId());
            } catch (Exception e) {
                log.error("[AsyncTask] Failed to process record {}: {}", record.getId(), e.getMessage(), e);
            }
        }
    }
}
