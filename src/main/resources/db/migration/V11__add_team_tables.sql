-- V11: 添加团队管理相关表

-- 团队表
CREATE TABLE IF NOT EXISTS `teams` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL COMMENT '团队名称',
    `description` TEXT COMMENT '团队描述',
    `logo_url` VARCHAR(500) COMMENT '团队Logo',
    `owner_id` BIGINT NOT NULL COMMENT '创建者ID',
    `invite_code` VARCHAR(20) UNIQUE COMMENT '邀请码',
    `max_members` INT DEFAULT 50 COMMENT '最大成员数',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISBANDED',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_teams_owner_id` (`owner_id`),
    INDEX `idx_teams_invite_code` (`invite_code`),
    INDEX `idx_teams_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队表';

-- 团队成员表
CREATE TABLE IF NOT EXISTS `team_members` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `team_id` BIGINT NOT NULL COMMENT '团队ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role` VARCHAR(20) DEFAULT 'MEMBER' COMMENT '角色：OWNER/ADMIN/MEMBER',
    `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_team_user` (`team_id`, `user_id`),
    INDEX `idx_team_members_team_id` (`team_id`),
    INDEX `idx_team_members_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队成员表';

-- 团队邀请表
CREATE TABLE IF NOT EXISTS `team_invitations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `team_id` BIGINT NOT NULL COMMENT '团队ID',
    `inviter_id` BIGINT NOT NULL COMMENT '邀请人ID',
    `invitee_id` BIGINT COMMENT '被邀请人ID',
    `invitee_phone` VARCHAR(20) COMMENT '被邀请人手机号',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING/ACCEPTED/REJECTED/EXPIRED',
    `expires_at` DATETIME COMMENT '过期时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_team_invitations_team_id` (`team_id`),
    INDEX `idx_team_invitations_invitee_id` (`invitee_id`),
    INDEX `idx_team_invitations_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队邀请表';