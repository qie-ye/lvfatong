package com.lvatong.lft.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncTaskProducer {

    static final String STREAM_KEY = "lvatong:tasks";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 发布任务到 Redis Stream（XADD lvatong:tasks）
     * 使用 MapRecord<String,String,String> 与 StringRedisTemplate 兼容
     */
    public RecordId publish(AsyncTaskMessage message) {
        Map<String, String> fields = Map.of(
                "type", message.type().name(),
                "entityId", message.entityId().toString(),
                "userId", message.userId().toString()
        );
        MapRecord<String, String, String> record = MapRecord.create(STREAM_KEY, fields);
        RecordId id = stringRedisTemplate.opsForStream().add(record);
        log.info("[AsyncTask] published type={} entityId={} userId={} -> streamId={}",
                message.type(), message.entityId(), message.userId(), id);
        return id;
    }
}
