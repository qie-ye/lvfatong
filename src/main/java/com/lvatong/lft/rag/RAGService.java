package com.lvatong.lft.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RAGService {

    // TODO: v0.2 实现RAG完整链路
    // - 文档分块（512 tokens, 128 overlap）
    // - 向量入库（BGE-M3 -> Milvus）
    // - 混合检索（Milvus向量搜索 + MySQL FULLTEXT + RRF融合）
    // - 上下文构建（max 4096 tokens）
}
