package com.lvatong.lft.async;

import com.lvatong.lft.mq.ContractAnalysisProducer;
import com.lvatong.lft.mq.dto.AnalysisTaskMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 统一异步任务服务
 *
 * 根据配置选择使用 Redis Stream 或 RabbitMQ 发送异步任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskService {

    private final AsyncTaskProducer redisTaskProducer;
    private final ContractAnalysisProducer rabbitProducer;

    @Value("${lvatong.mq.type:redis}")
    private String mqType;

    /**
     * 发布异步任务
     */
    public void publishTask(AsyncTaskMessage message) {
        if ("rabbitmq".equalsIgnoreCase(mqType)) {
            publishToRabbitMQ(message);
        } else {
            publishToRedis(message);
        }
    }

    /**
     * 发布合同分析任务到 RabbitMQ
     */
    public void publishContractAnalysis(Long documentId, Long userId, String filePath, String fileName) {
        if ("rabbitmq".equalsIgnoreCase(mqType)) {
            AnalysisTaskMessage message = AnalysisTaskMessage.builder()
                    .documentId(documentId)
                    .userId(userId)
                    .filePath(filePath)
                    .fileName(fileName)
                    .taskType(AnalysisTaskMessage.TaskType.CONTRACT_ANALYSIS)
                    .createdAt(LocalDateTime.now())
                    .build();

            rabbitProducer.sendAnalysisTask(message);
            log.info("Published contract analysis to RabbitMQ: documentId={}", documentId);
        } else {
            // 使用 Redis Stream
            redisTaskProducer.publish(new AsyncTaskMessage(
                    AsyncTaskMessage.TaskType.CONTRACT, documentId, userId));
            log.info("Published contract analysis to Redis: documentId={}", documentId);
        }
    }

    private void publishToRedis(AsyncTaskMessage message) {
        redisTaskProducer.publish(message);
    }

    private void publishToRabbitMQ(AsyncTaskMessage message) {
        // 根据任务类型路由到不同的 RabbitMQ 队列
        switch (message.type()) {
            case CONTRACT -> {
                AnalysisTaskMessage analysisMessage = AnalysisTaskMessage.builder()
                        .documentId(message.entityId())
                        .userId(message.userId())
                        .taskType(AnalysisTaskMessage.TaskType.CONTRACT_ANALYSIS)
                        .createdAt(LocalDateTime.now())
                        .build();
                rabbitProducer.sendAnalysisTask(analysisMessage);
            }
            default -> {
                // 其他类型暂时使用 Redis
                redisTaskProducer.publish(message);
                log.debug("Task type {} not supported in RabbitMQ, using Redis", message.type());
            }
        }
    }
}
