package com.lvatong.lft.controller;

import com.lvatong.lft.collaboration.ActivityLogService;
import com.lvatong.lft.collaboration.TaskService;
import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.model.entity.Task;
import com.lvatong.lft.model.entity.TaskComment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "任务管理", description = "任务创建、分配、状态流转、评论")
public class TaskController {

    private final TaskService taskService;
    private final ActivityLogService activityLogService;

    @PostMapping
    @Operation(summary = "创建任务")
    public ApiResult<Task> createTask(@RequestBody CreateTaskRequest request) {
        Long userId = 1L; // 临时硬编码
        Task task = taskService.createTask(userId, request.getTeamId(), request.getCaseId(),
                request.getTitle(), request.getDescription(), request.getAssigneeId(),
                request.getPriority(), request.getDueDate());
        
        // 记录日志
        if (request.getCaseId() != null) {
            activityLogService.logActivity(request.getCaseId(), userId, "TASK_CREATED", 
                    "创建任务: " + task.getTitle());
        }
        
        return ApiResult.success(task);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取任务详情")
    public ApiResult<Task> getTask(@PathVariable Long id) {
        return ApiResult.success(taskService.getTask(id));
    }

    @GetMapping("/team/{teamId}")
    @Operation(summary = "获取团队任务列表")
    public ApiResult<List<Task>> getTeamTasks(@PathVariable Long teamId,
                                               @RequestParam(required = false) Task.TaskStatus status) {
        return ApiResult.success(taskService.getTeamTasks(teamId, status));
    }

    @GetMapping("/team/{teamId}/kanban")
    @Operation(summary = "获取看板数据")
    public ApiResult<Map<String, List<Task>>> getKanbanData(@PathVariable Long teamId) {
        return ApiResult.success(taskService.getKanbanData(teamId));
    }

    @GetMapping("/my")
    @Operation(summary = "获取我的任务")
    public ApiResult<List<Task>> getMyTasks(@RequestParam(required = false) Task.TaskStatus status) {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(taskService.getUserTasks(userId, status));
    }

    @GetMapping("/case/{caseId}")
    @Operation(summary = "获取案件任务")
    public ApiResult<List<Task>> getCaseTasks(@PathVariable Long caseId) {
        return ApiResult.success(taskService.getCaseTasks(caseId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新任务")
    public ApiResult<Task> updateTask(@PathVariable Long id, @RequestBody UpdateTaskRequest request) {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(taskService.updateTask(id, userId, request.getTitle(),
                request.getDescription(), request.getAssigneeId(), request.getPriority(),
                request.getDueDate()));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新任务状态")
    public ApiResult<Task> updateTaskStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        Long userId = 1L; // 临时硬编码
        Task task = taskService.updateTaskStatus(id, userId, request.getStatus());
        
        // 记录日志
        if (task.getCaseId() != null) {
            activityLogService.logActivity(task.getCaseId(), userId, "TASK_STATUS_CHANGED", 
                    "任务状态变更: " + task.getTitle() + " -> " + request.getStatus());
        }
        
        return ApiResult.success(task);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务")
    public ApiResult<Void> deleteTask(@PathVariable Long id) {
        Long userId = 1L; // 临时硬编码
        Task task = taskService.getTask(id);
        
        taskService.deleteTask(id, userId);
        
        // 记录日志
        if (task.getCaseId() != null) {
            activityLogService.logActivity(task.getCaseId(), userId, "TASK_DELETED", 
                    "删除任务: " + task.getTitle());
        }
        
        return ApiResult.success(null);
    }

    // ==================== 评论操作 ====================

    @PostMapping("/{id}/comments")
    @Operation(summary = "添加评论")
    public ApiResult<TaskComment> addComment(@PathVariable Long id, @RequestBody AddCommentRequest request) {
        Long userId = 1L; // 临时硬编码
        return ApiResult.success(taskService.addComment(id, userId, request.getContent(), request.getParentId()));
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "获取任务评论")
    public ApiResult<List<TaskComment>> getTaskComments(@PathVariable Long id) {
        return ApiResult.success(taskService.getTaskComments(id));
    }

    @GetMapping("/comments/{id}/replies")
    @Operation(summary = "获取评论回复")
    public ApiResult<List<TaskComment>> getCommentReplies(@PathVariable Long id) {
        return ApiResult.success(taskService.getCommentReplies(id));
    }

    @DeleteMapping("/comments/{id}")
    @Operation(summary = "删除评论")
    public ApiResult<Void> deleteComment(@PathVariable Long id) {
        Long userId = 1L; // 临时硬编码
        taskService.deleteComment(id, userId);
        return ApiResult.success(null);
    }

    // ==================== 请求类 ====================

    @Data
    public static class CreateTaskRequest {
        private Long teamId;
        private Long caseId;
        private String title;
        private String description;
        private Long assigneeId;
        private Task.TaskPriority priority;
        private LocalDate dueDate;
    }

    @Data
    public static class UpdateTaskRequest {
        private String title;
        private String description;
        private Long assigneeId;
        private Task.TaskPriority priority;
        private LocalDate dueDate;
    }

    @Data
    public static class UpdateStatusRequest {
        private Task.TaskStatus status;
    }

    @Data
    public static class AddCommentRequest {
        private String content;
        private Long parentId;
    }
}