package com.lvatong.lft.messaging;

import com.lvatong.lft.model.entity.Notification;
import com.lvatong.lft.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 创建通知
     */
    @Async
    public void createNotification(Long userId, String type, String title, String content,
                                    String relatedType, Long relatedId) {
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(type);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setRelatedType(relatedType);
            notification.setRelatedId(relatedId);
            notification.setIsRead(false);

            notificationRepository.save(notification);
            log.debug("通知创建成功: userId={}, type={}", userId, type);
        } catch (Exception e) {
            log.error("创建通知失败", e);
        }
    }

    /**
     * 获取用户通知列表
     */
    public Page<Notification> getUserNotifications(Long userId, Boolean isRead, int page, int size) {
        if (isRead != null) {
            return notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, isRead, PageRequest.of(page, size));
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    /**
     * 获取未读通知列表
     */
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findTop10ByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取未读通知数量
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    /**
     * 标记通知已读
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.markAsRead(notificationId, userId);
    }

    /**
     * 标记所有通知已读
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    /**
     * 创建任务分配通知
     */
    @Async
    public void createTaskAssignedNotification(Long assigneeId, Long taskId, String taskTitle) {
        createNotification(assigneeId, "TASK_ASSIGNED", "新任务分配",
                "您有新的任务: " + taskTitle, "TASK", taskId);
    }

    /**
     * 创建任务状态变更通知
     */
    @Async
    public void createTaskStatusChangedNotification(Long assigneeId, Long taskId, String taskTitle, String newStatus) {
        createNotification(assigneeId, "TASK_STATUS_CHANGED", "任务状态变更",
                "任务 \"" + taskTitle + "\" 状态已变更为: " + newStatus, "TASK", taskId);
    }

    /**
     * 创建团队邀请通知
     */
    @Async
    public void createTeamInviteNotification(Long inviteeId, Long teamId, String teamName) {
        createNotification(inviteeId, "TEAM_INVITE", "团队邀请",
                "您被邀请加入团队: " + teamName, "TEAM", teamId);
    }

    /**
     * 创建评论通知
     */
    @Async
    public void createCommentNotification(Long userId, Long taskId, String taskTitle, String commenterName) {
        createNotification(userId, "TASK_COMMENT", "新评论",
                commenterName + " 在任务 \"" + taskTitle + "\" 中添加了评论", "TASK", taskId);
    }

    /**
     * 发送通知（兼容旧接口）
     */
    @Transactional
    public Notification send(Long userId, String type, String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(false);
        return notificationRepository.save(notification);
    }
}