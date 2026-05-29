-- V12: 添加案件协作相关表

-- 案件协作表
CREATE TABLE IF NOT EXISTS `case_collaborations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `case_id` BIGINT NOT NULL COMMENT '案件ID',
    `team_id` BIGINT NOT NULL COMMENT '团队ID',
    `shared_by` BIGINT NOT NULL COMMENT '分享人ID',
    `permission` VARCHAR(20) DEFAULT 'VIEW' COMMENT '权限：VIEW/EDIT/ADMIN',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_case_team` (`case_id`, `team_id`),
    INDEX `idx_case_collaborations_case_id` (`case_id`),
    INDEX `idx_case_collaborations_team_id` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='案件协作表';

-- 任务表
CREATE TABLE IF NOT EXISTS `tasks` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `case_id` BIGINT COMMENT '关联案件ID',
    `team_id` BIGINT NOT NULL COMMENT '所属团队ID',
    `title` VARCHAR(200) NOT NULL COMMENT '任务标题',
    `description` TEXT COMMENT '任务描述',
    `assignee_id` BIGINT COMMENT '负责人ID',
    `assigner_id` BIGINT NOT NULL COMMENT '分配人ID',
    `priority` VARCHAR(20) DEFAULT 'MEDIUM' COMMENT '优先级：LOW/MEDIUM/HIGH/URGENT',
    `status` VARCHAR(20) DEFAULT 'TODO' COMMENT '状态：TODO/IN_PROGRESS/REVIEW/DONE',
    `due_date` DATE COMMENT '截止日期',
    `completed_at` DATETIME COMMENT '完成时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_tasks_case_id` (`case_id`),
    INDEX `idx_tasks_team_id` (`team_id`),
    INDEX `idx_tasks_assignee_id` (`assignee_id`),
    INDEX `idx_tasks_status` (`status`),
    INDEX `idx_tasks_due_date` (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- 任务评论表
CREATE TABLE IF NOT EXISTS `task_comments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `user_id` BIGINT NOT NULL COMMENT '评论人ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `parent_id` BIGINT COMMENT '父评论ID（回复）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_task_comments_task_id` (`task_id`),
    INDEX `idx_task_comments_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务评论表';

-- 案件日志表
CREATE TABLE IF NOT EXISTS `case_activity_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `case_id` BIGINT NOT NULL COMMENT '案件ID',
    `user_id` BIGINT NOT NULL COMMENT '操作人ID',
    `action` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `details` JSON COMMENT '操作详情',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_case_activity_logs_case_id` (`case_id`),
    INDEX `idx_case_activity_logs_user_id` (`user_id`),
    INDEX `idx_case_activity_logs_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='案件日志表';