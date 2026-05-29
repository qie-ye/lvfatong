-- V10: 添加Dashboard数据聚合表

-- Dashboard统计数据表
CREATE TABLE IF NOT EXISTS `dashboard_stats` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `stat_type` VARCHAR(50) NOT NULL COMMENT '统计类型：USER_OVERVIEW/QUERY_STATS/HOT_QUERIES/AI_PERFORMANCE/RECOMMENDATION_STATS/CONTRACT_STATS',
    `stat_value` JSON NOT NULL COMMENT '统计数据（JSON格式）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_stat_date_type` (`stat_date`, `stat_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dashboard统计数据';

-- 添加注释说明
ALTER TABLE `dashboard_stats` COMMENT = 'Dashboard数据聚合表，用于存储预计算的统计数据';