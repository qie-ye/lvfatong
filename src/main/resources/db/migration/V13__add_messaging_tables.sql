-- V13: 添加消息通知相关表

-- 消息表
CREATE TABLE IF NOT EXISTS `messages` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `team_id` BIGINT COMMENT '团队ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `receiver_id` BIGINT COMMENT '接收者ID（私信）',
    `channel_type` VARCHAR(20) NOT NULL COMMENT '渠道类型：TEAM/TASK/CASE/PRIVATE',
    `channel_id` BIGINT COMMENT '关联ID（任务ID/案件ID）',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `message_type` VARCHAR(20) DEFAULT 'TEXT' COMMENT '消息类型：TEXT/SYSTEM/FILE',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_messages_team_id` (`team_id`),
    INDEX `idx_messages_sender_id` (`sender_id`),
    INDEX `idx_messages_receiver_id` (`receiver_id`),
    INDEX `idx_messages_channel` (`channel_type`, `channel_id`),
    INDEX `idx_messages_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 消息已读状态表
CREATE TABLE IF NOT EXISTS `message_read_status` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `message_id` BIGINT NOT NULL COMMENT '消息ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `read_at` DATETIME COMMENT '已读时间',
    UNIQUE KEY `uk_message_user` (`message_id`, `user_id`),
    INDEX `idx_message_read_status_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息已读状态表';

-- 使用存储过程安全添加通知表字段
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS add_column_if_not_exists(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_def VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = p_table_name
        AND COLUMN_NAME = p_column_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

-- 扩展已有的通知表（添加缺失字段）
CALL add_column_if_not_exists('notifications', 'related_type', 'VARCHAR(50) COMMENT ''关联类型''');
CALL add_column_if_not_exists('notifications', 'related_id', 'BIGINT COMMENT ''关联ID''');

-- 清理存储过程
DROP PROCEDURE IF EXISTS add_column_if_not_exists;