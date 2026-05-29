package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_messages_team_id", columnList = "teamId"),
        @Index(name = "idx_messages_sender_id", columnList = "senderId"),
        @Index(name = "idx_messages_receiver_id", columnList = "receiverId"),
        @Index(name = "idx_messages_created_at", columnList = "createdAt")
})
public class Message extends BaseEntity {

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id")
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ChannelType channelType;

    @Column(name = "channel_id")
    private Long channelId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType = MessageType.TEXT;

    public enum ChannelType {
        TEAM,   // 团队频道
        TASK,   // 任务评论
        CASE,   // 案件讨论
        PRIVATE // 私信
    }

    public enum MessageType {
        TEXT,    // 文本消息
        SYSTEM,  // 系统消息
        FILE     // 文件消息
    }
}