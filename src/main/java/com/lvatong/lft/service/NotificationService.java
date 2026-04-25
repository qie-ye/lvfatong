package com.lvatong.lft.service;

import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.entity.Notification;
import com.lvatong.lft.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 持久化并通过 STOMP 推送通知到指定用户
     */
    @Transactional
    public Notification send(Long userId, String type, String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification = notificationRepository.save(notification);

        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(), "/queue/notifications", notification);
            log.info("[Notification] pushed to userId={} type={}", userId, type);
        } catch (Exception e) {
            log.warn("[Notification] STOMP push failed for userId={}: {}", userId, e.getMessage());
        }
        return notification;
    }

    public List<Notification> getUnread(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getAll(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException("通知不存在"));
        if (!n.getUserId().equals(userId)) {
            throw new BusinessException("无权操作");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }
}
