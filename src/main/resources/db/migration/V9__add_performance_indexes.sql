-- V9: 添加性能优化索引

-- 使用存储过程安全创建索引
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS create_index_if_not_exists(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_columns VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = p_table_name
        AND INDEX_NAME = p_index_name
    ) THEN
        SET @sql = CONCAT('CREATE INDEX `', p_index_name, '` ON `', p_table_name, '` (', p_columns, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

-- 聊天会话表索引
CALL create_index_if_not_exists('chat_sessions', 'idx_chat_sessions_user_id_created', '`user_id`, `created_at` DESC');
CALL create_index_if_not_exists('chat_sessions', 'idx_chat_sessions_user_type', '`user_id`, `type`');

-- 聊天消息表索引
CALL create_index_if_not_exists('chat_messages', 'idx_chat_messages_session_id_created', '`session_id`, `created_at`');
CALL create_index_if_not_exists('chat_messages', 'idx_chat_messages_session_role', '`session_id`, `role`');

-- 合同文档表索引
CALL create_index_if_not_exists('contract_documents', 'idx_contract_documents_user_status', '`user_id`, `status`');
CALL create_index_if_not_exists('contract_documents', 'idx_contract_documents_created', '`created_at` DESC');

-- 法律案例表索引
CALL create_index_if_not_exists('legal_cases', 'idx_legal_cases_domain', '`domain`');
CALL create_index_if_not_exists('legal_cases', 'idx_legal_cases_created', '`created_at` DESC');

-- 知识文档表索引
CALL create_index_if_not_exists('knowledge_documents', 'idx_knowledge_documents_doc_type', '`doc_type`');
CALL create_index_if_not_exists('knowledge_documents', 'idx_knowledge_documents_vector_indexed', '`vector_indexed`');

-- 知识分块表索引
CALL create_index_if_not_exists('knowledge_chunks', 'idx_knowledge_chunks_document_id', '`document_id`');
CALL create_index_if_not_exists('knowledge_chunks', 'idx_knowledge_chunks_doc_type', '`doc_type`');
CALL create_index_if_not_exists('knowledge_chunks', 'idx_knowledge_chunks_law_domain', '`law_domain`');

-- 律师档案表索引
CALL create_index_if_not_exists('lawyer_profiles', 'idx_lawyer_profiles_specialties', '`specialties`(100)');
CALL create_index_if_not_exists('lawyer_profiles', 'idx_lawyer_profiles_available', '`available`');
CALL create_index_if_not_exists('lawyer_profiles', 'idx_lawyer_profiles_rating', '`rating` DESC');

-- FAQ表索引
CALL create_index_if_not_exists('faq_entries', 'idx_faq_entries_category', '`category`');
CALL create_index_if_not_exists('faq_entries', 'idx_faq_entries_enabled', '`enabled`');

-- 反馈表索引
CALL create_index_if_not_exists('answer_feedback', 'idx_answer_feedback_session_message', '`session_id`, `message_index`');
CALL create_index_if_not_exists('answer_feedback', 'idx_answer_feedback_user_rating', '`user_id`, `rating`');

-- 通知表索引
CALL create_index_if_not_exists('notifications', 'idx_notifications_user_read', '`user_id`, `read`');
CALL create_index_if_not_exists('notifications', 'idx_notifications_created', '`created_at` DESC');

-- 审计日志表索引
CALL create_index_if_not_exists('audit_logs', 'idx_audit_logs_user_action', '`user_id`, `action`');
CALL create_index_if_not_exists('audit_logs', 'idx_audit_logs_created', '`created_at` DESC');

-- 会话摘要表索引
CALL create_index_if_not_exists('session_summaries', 'idx_session_summaries_user', '`user_id`');
CALL create_index_if_not_exists('session_summaries', 'idx_session_summaries_session', '`session_id`');

-- 用户记忆表索引
CALL create_index_if_not_exists('user_memories', 'idx_user_memories_user_type', '`user_id`, `memory_type`');

-- 清理存储过程
DROP PROCEDURE IF EXISTS create_index_if_not_exists;