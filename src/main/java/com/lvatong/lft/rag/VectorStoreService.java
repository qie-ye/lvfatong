package com.lvatong.lft.rag;

import com.lvatong.lft.config.MilvusConfig;
import com.alibaba.fastjson.JSONObject;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class VectorStoreService {

    @Autowired(required = false)
    private MilvusClientV2 milvusClient;

    @Autowired(required = false)
    private MilvusConfig milvusConfig;

    private boolean isMilvusAvailable() {
        return milvusClient != null && milvusConfig != null;
    }

    /**
     * 确保Collection存在，不存在则创建
     */
    public void ensureCollection() {
        if (!isMilvusAvailable()) {
            log.warn("Milvus not available, skip ensureCollection");
            return;
        }
        String name = milvusConfig.getCollectionName();
        boolean exists = milvusClient.hasCollection(HasCollectionReq.builder()
            .collectionName(name).build());
        if (!exists) {
            CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder()
                .build();
            schema.addField(AddFieldReq.builder()
                .fieldName("id").dataType(DataType.Int64)
                .isPrimaryKey(true).autoID(true).build());
            schema.addField(AddFieldReq.builder()
                .fieldName("document_id").dataType(DataType.Int64).build());
            schema.addField(AddFieldReq.builder()
                .fieldName("content").dataType(DataType.VarChar)
                .maxLength(2000).build());
            schema.addField(AddFieldReq.builder()
                .fieldName("doc_type").dataType(DataType.VarChar)
                .maxLength(50).build());
            schema.addField(AddFieldReq.builder()
                .fieldName("law_domain").dataType(DataType.VarChar)
                .maxLength(50).build());
            schema.addField(AddFieldReq.builder()
                .fieldName("embedding").dataType(DataType.FloatVector)
                .dimension(milvusConfig.getEmbeddingDim()).build());

            List<IndexParam> indexParams = List.of(
                IndexParam.builder()
                    .fieldName("embedding")
                    .indexType(IndexParam.IndexType.valueOf(milvusConfig.getIndexType()))
                    .metricType(IndexParam.MetricType.valueOf(milvusConfig.getMetricType()))
                    .extraParams(Map.of("M", 16, "efConstruction", 256))
                    .build()
            );

            milvusClient.createCollection(CreateCollectionReq.builder()
                .collectionName(name)
                .collectionSchema(schema)
                .indexParams(indexParams)
                .build());
            log.info("Created Milvus collection: {}", name);
        }
    }

    /**
     * 插入向量数据
     */
    public void insert(Long documentId, String content, String docType, String lawDomain, List<Float> embedding) {
        if (!isMilvusAvailable()) {
            log.warn("Milvus not available, skip insert for document {}", documentId);
            return;
        }
        ensureCollection();
        JSONObject row = new JSONObject();
        row.put("document_id", documentId);
        row.put("content", content);
        row.put("doc_type", docType);
        row.put("law_domain", lawDomain);
        row.put("embedding", embedding);

        milvusClient.insert(InsertReq.builder()
            .collectionName(milvusConfig.getCollectionName())
            .data(List.of(row))
            .build());
    }

    /**
     * 批量插入向量数据
     */
    public void batchInsert(List<JSONObject> rows) {
        if (!isMilvusAvailable()) {
            log.warn("Milvus not available, skip batchInsert");
            return;
        }
        ensureCollection();
        milvusClient.insert(InsertReq.builder()
            .collectionName(milvusConfig.getCollectionName())
            .data(rows)
            .build());
    }

    /**
     * 向量相似度搜索（带标量过滤）
     */
    public List<SearchResp.SearchResult> search(List<Float> queryVector, int topK, String filterExpr) {
        if (!isMilvusAvailable()) {
            log.warn("Milvus not available, return empty search results");
            return Collections.emptyList();
        }
        ensureCollection();
        SearchReq searchReq = SearchReq.builder()
            .collectionName(milvusConfig.getCollectionName())
            .data(List.of(queryVector))
            .topK(topK)
            .filter(filterExpr)
            .searchParams(Map.of("ef", 128))
            .build();
        SearchResp resp = milvusClient.search(searchReq);
        return resp.getSearchResults().get(0);
    }

    /**
     * 向量相似度搜索（无过滤）
     */
    public List<SearchResp.SearchResult> search(List<Float> queryVector, int topK) {
        return search(queryVector, topK, "");
    }

}
