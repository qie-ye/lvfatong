package com.lvatong.lft.knowledge;

import com.lvatong.lft.model.entity.KnowledgeDocument;
import com.lvatong.lft.rag.RAGService;
import com.lvatong.lft.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final RAGService ragService;

    /**
     * 导入法律条文
     */
    @Transactional
    public KnowledgeDocument importLaw(String title, String source, String lawDomain, String content) {
        return importLaw(title, source, lawDomain, content, null);
    }

    @Transactional
    public KnowledgeDocument importLaw(String title, String source, String lawDomain, String content, String sourceUrl) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(title);
        doc.setSource(source);
        doc.setDocType(KnowledgeDocument.DocType.LAW);
        doc.setLawDomain(lawDomain);
        doc.setContent(cleanContent(content));
        doc.setSourceUrl(sourceUrl);
        doc.setVectorIndexed(false);
        return knowledgeDocumentRepository.save(doc);
    }

    /**
     * 批量入库未索引的文档（分块+Embedding+向量存储）
     */
    public int ingestPendingDocuments() {
        List<KnowledgeDocument> pending = knowledgeDocumentRepository.findByVectorIndexedFalse();
        int count = 0;
        for (KnowledgeDocument doc : pending) {
            try {
                ragService.ingestDocument(doc.getId());
                count++;
                log.info("Ingested document: {} ({})", doc.getTitle(), doc.getId());
            } catch (Exception e) {
                log.error("Failed to ingest document {} ({}): {}", doc.getTitle(), doc.getId(), e.getMessage());
            }
        }
        log.info("Ingested {} out of {} pending documents", count, pending.size());
        return count;
    }

    /**
     * 获取所有法律文档
     */
    public List<KnowledgeDocument> listDocuments() {
        return knowledgeDocumentRepository.findAll();
    }

    /**
     * 按类型查询文档
     */
    public List<KnowledgeDocument> listByDocType(KnowledgeDocument.DocType docType) {
        return knowledgeDocumentRepository.findByDocType(docType);
    }

    /**
     * 数据清洗：去除多余空白、统一标点等
     */
    private String cleanContent(String content) {
        if (content == null) return "";
        return content
                .replaceAll("\\r\\n", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
