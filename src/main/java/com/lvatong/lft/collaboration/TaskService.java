package com.lvatong.lft.collaboration;

import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.entity.Task;
import com.lvatong.lft.model.entity.TaskComment;
import com.lvatong.lft.repository.TaskCommentRepository;
import com.lvatong.lft.repository.TaskRepository;
import com.lvatong.lft.team.TeamMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TeamMemberService teamMemberService;

    /**
     * 创建任务
     */
    @Transactional
    public Task createTask(Long userId, Long teamId, Long caseId, String title, String description,
                           Long assigneeId, Task.TaskPriority priority, LocalDate dueDate) {
        // 检查是否是团队成员
        if (!teamMemberService.isTeamMember(teamId, userId)) {
            throw new BusinessException("您不是该团队成员");
        }

        // 如果指定了负责人，检查负责人是否是团队成员
        if (assigneeId != null && !teamMemberService.isTeamMember(teamId, assigneeId)) {
            throw new BusinessException("负责人不是该团队成员");
        }

        Task task = new Task();
        task.setTeamId(teamId);
        task.setCaseId(caseId);
        task.setTitle(title);
        task.setDescription(description);
        task.setAssigneeId(assigneeId);
        task.setAssignerId(userId);
        task.setPriority(priority != null ? priority : Task.TaskPriority.MEDIUM);
        task.setStatus(Task.TaskStatus.TODO);
        task.setDueDate(dueDate);

        task = taskRepository.save(task);
        log.info("任务创建成功: taskId={}, teamId={}", task.getId(), teamId);
        return task;
    }

    /**
     * 获取任务详情
     */
    public Task getTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));
    }

    /**
     * 获取团队任务列表
     */
    public List<Task> getTeamTasks(Long teamId, Task.TaskStatus status) {
        if (status != null) {
            return taskRepository.findByTeamIdAndStatus(teamId, status);
        }
        return taskRepository.findByTeamIdOrderByCreatedAtDesc(teamId);
    }

    /**
     * 获取看板数据
     */
    public Map<String, List<Task>> getKanbanData(Long teamId) {
        Map<String, List<Task>> kanban = new HashMap<>();
        kanban.put("TODO", taskRepository.findByTeamIdAndStatus(teamId, Task.TaskStatus.TODO));
        kanban.put("IN_PROGRESS", taskRepository.findByTeamIdAndStatus(teamId, Task.TaskStatus.IN_PROGRESS));
        kanban.put("REVIEW", taskRepository.findByTeamIdAndStatus(teamId, Task.TaskStatus.REVIEW));
        kanban.put("DONE", taskRepository.findByTeamIdAndStatus(teamId, Task.TaskStatus.DONE));
        return kanban;
    }

    /**
     * 获取用户任务列表
     */
    public List<Task> getUserTasks(Long userId, Task.TaskStatus status) {
        if (status != null) {
            return taskRepository.findByAssigneeIdAndStatus(userId, status);
        }
        return taskRepository.findByAssigneeIdAndStatus(userId, Task.TaskStatus.TODO);
    }

    /**
     * 获取案件任务列表
     */
    public List<Task> getCaseTasks(Long caseId) {
        return taskRepository.findByCaseId(caseId);
    }

    /**
     * 更新任务
     */
    @Transactional
    public Task updateTask(Long taskId, Long userId, String title, String description,
                           Long assigneeId, Task.TaskPriority priority, LocalDate dueDate) {
        Task task = getTask(taskId);

        // 检查权限
        if (!teamMemberService.isTeamMember(task.getTeamId(), userId)) {
            throw new BusinessException("您不是该团队成员");
        }

        if (title != null && !title.isEmpty()) {
            task.setTitle(title);
        }
        if (description != null) {
            task.setDescription(description);
        }
        if (assigneeId != null) {
            if (!teamMemberService.isTeamMember(task.getTeamId(), assigneeId)) {
                throw new BusinessException("负责人不是该团队成员");
            }
            task.setAssigneeId(assigneeId);
        }
        if (priority != null) {
            task.setPriority(priority);
        }
        if (dueDate != null) {
            task.setDueDate(dueDate);
        }

        task = taskRepository.save(task);
        log.info("任务更新成功: taskId={}", taskId);
        return task;
    }

    /**
     * 更新任务状态
     */
    @Transactional
    public Task updateTaskStatus(Long taskId, Long userId, Task.TaskStatus newStatus) {
        Task task = getTask(taskId);

        // 检查权限
        if (!teamMemberService.isTeamMember(task.getTeamId(), userId)) {
            throw new BusinessException("您不是该团队成员");
        }

        task.setStatus(newStatus);
        if (newStatus == Task.TaskStatus.DONE) {
            task.setCompletedAt(LocalDateTime.now());
        }

        task = taskRepository.save(task);
        log.info("任务状态更新: taskId={}, newStatus={}", taskId, newStatus);
        return task;
    }

    /**
     * 删除任务
     */
    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        Task task = getTask(taskId);

        // 检查权限（分配人或团队管理员可以删除）
        if (!task.getAssignerId().equals(userId)) {
            var role = teamMemberService.getMemberRole(task.getTeamId(), userId);
            if (role == null || role.name().equals("MEMBER")) {
                throw new BusinessException("只有分配人或管理员可以删除任务");
            }
        }

        // 删除相关评论
        List<TaskComment> comments = taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        taskCommentRepository.deleteAll(comments);

        taskRepository.delete(task);
        log.info("任务删除成功: taskId={}", taskId);
    }

    /**
     * 添加评论
     */
    @Transactional
    public TaskComment addComment(Long taskId, Long userId, String content, Long parentId) {
        Task task = getTask(taskId);

        // 检查是否是团队成员
        if (!teamMemberService.isTeamMember(task.getTeamId(), userId)) {
            throw new BusinessException("您不是该团队成员");
        }

        TaskComment comment = new TaskComment();
        comment.setTaskId(taskId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);

        comment = taskCommentRepository.save(comment);
        log.info("评论添加成功: taskId={}, userId={}", taskId, userId);
        return comment;
    }

    /**
     * 获取任务评论
     */
    public List<TaskComment> getTaskComments(Long taskId) {
        return taskCommentRepository.findByTaskIdAndParentIdIsNullOrderByCreatedAtDesc(taskId);
    }

    /**
     * 获取评论回复
     */
    public List<TaskComment> getCommentReplies(Long commentId) {
        return taskCommentRepository.findByParentIdOrderByCreatedAtAsc(commentId);
    }

    /**
     * 删除评论
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        TaskComment comment = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("评论不存在"));

        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的评论");
        }

        taskCommentRepository.delete(comment);
        log.info("评论删除成功: commentId={}", commentId);
    }
}