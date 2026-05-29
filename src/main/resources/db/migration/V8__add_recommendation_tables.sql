-- V8: 添加个性化推荐相关表

-- 用户行为记录表
CREATE TABLE IF NOT EXISTS `user_behaviors` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `action_type` VARCHAR(50) NOT NULL COMMENT '行为类型：SEARCH/VIEW/CLICK/FEEDBACK/SHARE',
    `target_type` VARCHAR(50) NOT NULL COMMENT '目标类型：LAW/CASE/FAQ/LAWYER/CONTRACT/CHAT',
    `target_id` BIGINT COMMENT '目标ID',
    `query_text` VARCHAR(500) COMMENT '查询文本',
    `domain` VARCHAR(100) COMMENT '法律领域',
    `duration_seconds` INT COMMENT '停留时长（秒）',
    `rating` VARCHAR(20) COMMENT '评分：GOOD/BAD',
    `metadata` JSON COMMENT '扩展元数据',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_behaviors_user_id` (`user_id`),
    INDEX `idx_user_behaviors_action_type` (`action_type`),
    INDEX `idx_user_behaviors_target_type` (`target_type`),
    INDEX `idx_user_behaviors_created_at` (`created_at`),
    INDEX `idx_user_behaviors_user_action` (`user_id`, `action_type`),
    INDEX `idx_user_behaviors_domain` (`domain`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为记录';

-- 用户偏好画像表
CREATE TABLE IF NOT EXISTS `user_preferences` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE,
    `preferred_domains` JSON COMMENT '偏好法律领域，如["劳动法","合同法"]',
    `preferred ActionTypes` JSON COMMENT '偏好行为类型',
    `query_frequency` INT DEFAULT 0 COMMENT '查询频率',
    `avg_session_duration` INT DEFAULT 0 COMMENT '平均会话时长（秒）',
    `expertise_level` VARCHAR(20) DEFAULT 'BEGINNER' COMMENT '专业水平：BEGINNER/INTERMEDIATE/EXPERT',
    `last_active_at` DATETIME COMMENT '最后活跃时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_preferences_user_id` (`user_id`),
    INDEX `idx_user_preferences_expertise` (`expertise_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好画像';

-- 推荐记录表（用于推荐效果追踪）
CREATE TABLE IF NOT EXISTS `recommendation_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `recommendation_type` VARCHAR(50) NOT NULL COMMENT '推荐类型：LAW/CASE/FAQ/LAWYER/TOPIC',
    `recommended_items` JSON NOT NULL COMMENT '推荐结果列表',
    `algorithm_version` VARCHAR(20) DEFAULT 'v1' COMMENT '算法版本',
    `clicked` BOOLEAN DEFAULT FALSE COMMENT '是否被点击',
    `clicked_item_id` BIGINT COMMENT '被点击的项目ID',
    `feedback_rating` VARCHAR(20) COMMENT '用户反馈',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_recommendation_logs_user_id` (`user_id`),
    INDEX `idx_recommendation_logs_type` (`recommendation_type`),
    INDEX `idx_recommendation_logs_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推荐记录';

-- 热门查询表（用于全局热门推荐）
CREATE TABLE IF NOT EXISTS `popular_queries` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `query_text` VARCHAR(500) NOT NULL,
    `domain` VARCHAR(100),
    `query_count` INT DEFAULT 1,
    `last_queried_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX `idx_popular_queries_text` (`query_text`),
    INDEX `idx_popular_queries_domain` (`domain`),
    INDEX `idx_popular_queries_count` (`query_count` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='热门查询';
