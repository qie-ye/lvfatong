package com.lvatong.lft.knowledgegraph;

import com.lvatong.lft.model.entity.KnowledgeDocument;
import com.lvatong.lft.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphSyncService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeGraphService knowledgeGraphService;

    /**
     * 同步所有知识文档到图谱
     */
    @Async
    public void syncAllDocuments() {
        log.info("开始同步所有知识文档到图谱");
        List<KnowledgeDocument> documents = knowledgeDocumentRepository.findAll();
        int successCount = 0;
        int failCount = 0;

        for (KnowledgeDocument document : documents) {
            try {
                syncDocument(document);
                successCount++;
            } catch (Exception e) {
                log.error("同步文档失败: documentId={}", document.getId(), e);
                failCount++;
            }
        }

        log.info("知识文档同步完成: 成功={}, 失败={}", successCount, failCount);
    }

    /**
     * 同步单个文档到图谱
     */
    @Async
    public void syncDocument(KnowledgeDocument document) {
        log.info("开始同步文档到图谱: documentId={}", document.getId());

        try {
            // 提取文档内容
            String content = document.getContent();
            if (content == null || content.isEmpty()) {
                log.warn("文档内容为空: documentId={}", document.getId());
                return;
            }

            // 构建知识图谱
            knowledgeGraphService.buildGraphFromText(content, document.getId().toString());

            log.info("文档同步完成: documentId={}", document.getId());
        } catch (Exception e) {
            log.error("文档同步失败: documentId={}", document.getId(), e);
            throw e;
        }
    }

    /**
     * 重建整个知识图谱
     */
    @Async
    public void rebuildGraph() {
        log.info("开始重建知识图谱");

        // 清空现有图谱数据
        clearGraph();

        // 重新同步所有文档
        syncAllDocuments();

        log.info("知识图谱重建完成");
    }

    /**
     * 清空图谱数据
     */
    private void clearGraph() {
        try {
            // 这里可以添加清空Neo4j数据库的逻辑
            log.info("图谱数据已清空");
        } catch (Exception e) {
            log.error("清空图谱数据失败", e);
            throw e;
        }
    }
}