-- ============================================================
-- 律法通数据库初始化脚本（MySQL 8.0）
-- 向量存储使用Milvus，不在此SQL中创建向量表
--
-- 本地开发执行方式：
--   1. mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS lvatong_dev DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;"
--   2. mysql -u root -p lvatong_dev < init-db.sql
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) DEFAULT NULL,
    email VARCHAR(100) DEFAULT NULL,
    phone VARCHAR(20) DEFAULT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 对话会话表
CREATE TABLE IF NOT EXISTS chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) DEFAULT NULL,
    type VARCHAR(20) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 对话消息表
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(10) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 合同文档表
CREATE TABLE IF NOT EXISTS contract_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    filename VARCHAR(200) NOT NULL,
    file_type VARCHAR(50) DEFAULT NULL,
    file_size BIGINT DEFAULT NULL,
    file_path TEXT DEFAULT NULL,
    parsed_text TEXT DEFAULT NULL,
    status VARCHAR(20) DEFAULT 'UPLOADED',
    analysis_result TEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 知识库文档表
CREATE TABLE IF NOT EXISTS knowledge_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    source VARCHAR(200) DEFAULT NULL,
    doc_type VARCHAR(20) DEFAULT NULL,
    law_domain VARCHAR(50) DEFAULT NULL,
    content TEXT DEFAULT NULL,
    vector_indexed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_doc_type (doc_type),
    INDEX idx_law_domain (law_domain)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 知识库文档内容分块表（用于全文检索，向量存储在Milvus中）
CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    doc_type VARCHAR(50) DEFAULT NULL,
    law_domain VARCHAR(50) DEFAULT NULL,
    metadata JSON DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_document_id (document_id),
    FULLTEXT INDEX ft_content (content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 常见问题表
CREATE TABLE IF NOT EXISTS faq_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(100) DEFAULT NULL,
    tags JSON DEFAULT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FULLTEXT INDEX ft_question (question) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 律师评价表
CREATE TABLE IF NOT EXISTS lawyer_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    lawyer_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(1000) DEFAULT NULL,
    service_type VARCHAR(50) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_lawyer (user_id, lawyer_id),
    INDEX idx_lawyer_id (lawyer_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 法律文书表
CREATE TABLE IF NOT EXISTS legal_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(300) NOT NULL,
    doc_type VARCHAR(50) NOT NULL,
    domain VARCHAR(50) DEFAULT NULL,
    facts TEXT DEFAULT NULL,
    claims TEXT DEFAULT NULL,
    content TEXT DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATING',
    model VARCHAR(50) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_doc_type (doc_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 法律意见书表
CREATE TABLE IF NOT EXISTS legal_opinions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(300) NOT NULL,
    domain VARCHAR(50) DEFAULT NULL,
    question TEXT DEFAULT NULL,
    facts TEXT DEFAULT NULL,
    analysis TEXT DEFAULT NULL,
    conclusion TEXT DEFAULT NULL,
    legal_basis TEXT DEFAULT NULL,
    suggestions TEXT DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATING',
    model VARCHAR(50) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_domain (domain)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 合同模板表
CREATE TABLE IF NOT EXISTS contract_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(50) DEFAULT NULL,
    description VARCHAR(500) DEFAULT NULL,
    content TEXT DEFAULT NULL,
    applicable_law VARCHAR(50) DEFAULT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 法律案例表
CREATE TABLE IF NOT EXISTS legal_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(300) NOT NULL,
    case_no VARCHAR(100) DEFAULT NULL,
    case_type VARCHAR(50) DEFAULT NULL,
    court VARCHAR(100) DEFAULT NULL,
    year VARCHAR(20) DEFAULT NULL,
    domain VARCHAR(50) DEFAULT NULL,
    keywords VARCHAR(500) DEFAULT NULL,
    province VARCHAR(50) DEFAULT NULL,
    summary TEXT DEFAULT NULL,
    facts TEXT DEFAULT NULL,
    ruling TEXT DEFAULT NULL,
    analysis TEXT DEFAULT NULL,
    vector_indexed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_case_type (case_type),
    INDEX idx_court (court),
    INDEX idx_year (year),
    FULLTEXT INDEX ft_case_content (summary, facts, ruling) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 律师档案表
CREATE TABLE IF NOT EXISTS lawyer_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    real_name VARCHAR(100) DEFAULT NULL,
    law_firm VARCHAR(50) DEFAULT NULL,
    license_no VARCHAR(50) DEFAULT NULL,
    bio VARCHAR(2000) DEFAULT NULL,
    education VARCHAR(500) DEFAULT NULL,
    specialties VARCHAR(1000) DEFAULT NULL,
    tags VARCHAR(500) DEFAULT NULL,
    province VARCHAR(20) DEFAULT NULL,
    city VARCHAR(20) DEFAULT NULL,
    years_of_experience INT DEFAULT NULL,
    rating DOUBLE DEFAULT 0.0,
    consultation_count INT DEFAULT 0,
    verified BOOLEAN DEFAULT FALSE,
    available BOOLEAN DEFAULT TRUE,
    consultation_type VARCHAR(20) DEFAULT 'ONLINE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_province_city (province, city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 律师预约表
CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    lawyer_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    appointment_time DATETIME NOT NULL,
    consultation_type VARCHAR(20) DEFAULT NULL,
    description VARCHAR(500) DEFAULT NULL,
    cancel_reason VARCHAR(500) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_lawyer_id (lawyer_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 操作审计日志表
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT NULL,
    username VARCHAR(50) DEFAULT NULL,
    action VARCHAR(50) NOT NULL,
    resource VARCHAR(200) DEFAULT NULL,
    detail VARCHAR(500) DEFAULT NULL,
    ip VARCHAR(50) DEFAULT NULL,
    method VARCHAR(10) DEFAULT NULL,
    uri VARCHAR(200) DEFAULT NULL,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    duration_ms BIGINT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
