-- V7: 扩展 answer_feedback 表，支持反馈分析和微调数据导出
-- 新增字段：question, answer, intent_type, model_used, context_used, bad_reason, issue_tags

ALTER TABLE answer_feedback
    ADD COLUMN question TEXT COMMENT '用户问题（用于微调数据）' AFTER message_index,
    ADD COLUMN answer TEXT COMMENT 'AI回答（用于微调数据）' AFTER question,
    ADD COLUMN intent_type VARCHAR(50) COMMENT '意图类型（LEGAL_QA, LAW_QUERY等）' AFTER answer,
    ADD COLUMN model_used VARCHAR(50) COMMENT '使用的模型（glm-4-flash等）' AFTER intent_type,
    ADD COLUMN context_used TEXT COMMENT '检索的上下文（用于分析检索质量）' AFTER model_used,
    ADD COLUMN bad_reason VARCHAR(500) COMMENT '点踩原因（可选）' AFTER context_used,
    ADD COLUMN issue_tags VARCHAR(200) COMMENT '问题分类标签（法条错误、逻辑混乱等）' AFTER bad_reason;

-- 添加索引，支持按意图类型和问题标签查询
CREATE INDEX idx_feedback_intent_type ON answer_feedback(intent_type);
CREATE INDEX idx_feedback_issue_tags ON answer_feedback(issue_tags);
CREATE INDEX idx_feedback_rating_intent ON answer_feedback(rating, intent_type);

-- 添加索引，支持按创建时间范围查询（用于统计）
CREATE INDEX idx_feedback_created_at ON answer_feedback(created_at);
