-- V14: 添加评论和文件共享相关表

-- 评论表
CREATE TABLE IF NOT EXISTS `comments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `target_type` VARCHAR(50) NOT NULL COMMENT '目标类型：CASE/TASK/DOCUMENT',
    `target_id` BIGINT NOT NULL COMMENT '目标ID',
    `user_id` BIGINT NOT NULL COMMENT '评论人ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `parent_id` BIGINT COMMENT '父评论ID（回复）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_comments_target` (`target_type`, `target_id`),
    INDEX `idx_comments_user_id` (`user_id`),
    INDEX `idx_comments_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 团队文件表
CREATE TABLE IF NOT EXISTS `team_files` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `team_id` BIGINT NOT NULL COMMENT '团队ID',
    `case_id` BIGINT COMMENT '关联案件ID',
    `name` VARCHAR(200) NOT NULL COMMENT '文件名',
    `file_url` VARCHAR(500) NOT NULL COMMENT '文件URL',
    `file_size` BIGINT COMMENT '文件大小（字节）',
    `file_type` VARCHAR(50) COMMENT '文件类型',
    `uploader_id` BIGINT NOT NULL COMMENT '上传者ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_team_files_team_id` (`team_id`),
    INDEX `idx_team_files_case_id` (`case_id`),
    INDEX `idx_team_files_uploader_id` (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队文件表';