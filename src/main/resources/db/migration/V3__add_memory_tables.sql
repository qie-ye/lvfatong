-- V3: Add memory tables and session status for 3-layer memory architecture

-- Add status column to chat_sessions
ALTER TABLE chat_sessions ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER type;

-- Session summaries (L2 memory)
CREATE TABLE IF NOT EXISTS session_summaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    summary TEXT DEFAULT NULL,
    topics JSON DEFAULT NULL,
    key_points JSON DEFAULT NULL,
    message_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_session_id (session_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User memories (L3 memory: preferences, topics, profile)
CREATE TABLE IF NOT EXISTS user_memories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    memory_type VARCHAR(20) NOT NULL,
    `key` VARCHAR(100) NOT NULL,
    value TEXT DEFAULT NULL,
    source_session_id BIGINT DEFAULT NULL,
    confidence DOUBLE DEFAULT 0.5,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_memory_type (memory_type),
    UNIQUE KEY uk_user_type_key (user_id, memory_type, `key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
