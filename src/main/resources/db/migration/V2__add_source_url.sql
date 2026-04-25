-- V2: add source_url column to knowledge_documents
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'knowledge_documents' AND column_name = 'source_url');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE knowledge_documents ADD COLUMN source_url VARCHAR(500) DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
