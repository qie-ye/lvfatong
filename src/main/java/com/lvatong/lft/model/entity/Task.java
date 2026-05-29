package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_case_id", columnList = "caseId"),
        @Index(name = "idx_tasks_team_id", columnList = "teamId"),
        @Index(name = "idx_tasks_assignee_id", columnList = "assigneeId"),
        @Index(name = "idx_tasks_status", columnList = "status"),
        @Index(name = "idx_tasks_due_date", columnList = "dueDate")
})
public class Task extends BaseEntity {

    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "assigner_id", nullable = false)
    private Long assignerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum TaskPriority {
        LOW,        // 低
        MEDIUM,     // 中
        HIGH,       // 高
        URGENT      // 紧急
    }

    public enum TaskStatus {
        TODO,           // 待办
        IN_PROGRESS,    // 进行中
        REVIEW,         // 审核中
        DONE            // 已完成
    }
}