package com.lvatong.lft.async;

/**
 * Redis Streams 异步任务消息体
 */
public record AsyncTaskMessage(
        TaskType type,
        Long entityId,
        Long userId
) {
    public enum TaskType {
        CONTRACT,
        OPINION,
        DOCUMENT
    }
}
