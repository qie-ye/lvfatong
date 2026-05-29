package com.lvatong.lft.messaging;

import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.entity.Message;
import com.lvatong.lft.model.entity.MessageReadStatus;
import com.lvatong.lft.repository.MessageReadStatusRepository;
import com.lvatong.lft.repository.MessageRepository;
import com.lvatong.lft.team.TeamMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageReadStatusRepository messageReadStatusRepository;
    private final TeamMemberService teamMemberService;

    /**
     * 发送团队消息
     */
    @Transactional
    public Message sendTeamMessage(Long teamId, Long senderId, String content) {
        // 检查是否是团队成员
        if (!teamMemberService.isTeamMember(teamId, senderId)) {
            throw new BusinessException("您不是该团队成员");
        }

        Message message = new Message();
        message.setTeamId(teamId);
        message.setSenderId(senderId);
        message.setChannelType(Message.ChannelType.TEAM);
        message.setContent(content);
        message.setMessageType(Message.MessageType.TEXT);

        message = messageRepository.save(message);
        log.info("团队消息发送成功: teamId={}, senderId={}", teamId, senderId);
        return message;
    }

    /**
     * 发送私信
     */
    @Transactional
    public Message sendPrivateMessage(Long senderId, Long receiverId, String content) {
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setChannelType(Message.ChannelType.PRIVATE);
        message.setContent(content);
        message.setMessageType(Message.MessageType.TEXT);

        message = messageRepository.save(message);
        log.info("私信发送成功: senderId={}, receiverId={}", senderId, receiverId);
        return message;
    }

    /**
     * 发送系统消息
     */
    @Transactional
    public Message sendSystemMessage(Long teamId, String content, Long channelId, Message.ChannelType channelType) {
        Message message = new Message();
        message.setTeamId(teamId);
        message.setSenderId(0L); // 系统用户ID
        message.setChannelType(channelType);
        message.setChannelId(channelId);
        message.setContent(content);
        message.setMessageType(Message.MessageType.SYSTEM);

        message = messageRepository.save(message);
        log.info("系统消息发送成功: teamId={}, channelType={}", teamId, channelType);
        return message;
    }

    /**
     * 获取团队消息列表
     */
    public Page<Message> getTeamMessages(Long teamId, int page, int size) {
        return messageRepository.findTeamMessages(teamId, PageRequest.of(page, size));
    }

    /**
     * 获取频道消息列表
     */
    public Page<Message> getChannelMessages(Message.ChannelType channelType, Long channelId, int page, int size) {
        return messageRepository.findByChannelTypeAndChannelIdOrderByCreatedAtDesc(channelType, channelId, PageRequest.of(page, size));
    }

    /**
     * 获取私信列表
     */
    public Page<Message> getPrivateMessages(Long userId, int page, int size) {
        return messageRepository.findPrivateMessages(userId, PageRequest.of(page, size));
    }

    /**
     * 标记消息已读
     */
    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        if (!messageReadStatusRepository.existsByMessageIdAndUserId(messageId, userId)) {
            MessageReadStatus status = new MessageReadStatus();
            status.setMessageId(messageId);
            status.setUserId(userId);
            status.setReadAt(LocalDateTime.now());
            messageReadStatusRepository.save(status);
        }
    }

    /**
     * 获取未读消息数量
     */
    public long getUnreadCount(Long teamId, Long userId) {
        // 简化实现：返回最近的消息数量
        return 0;
    }
}