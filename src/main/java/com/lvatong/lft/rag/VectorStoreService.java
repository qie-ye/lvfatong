package com.lvatong.lft.rag;

import com.lvatong.lft.config.MilvusConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final MilvusClientV2 milvusClient;
    private final MilvusConfig milvusConfig;

    /**
     * 确保Collection存在，不存在则创建
     */
    public void ensureCollection() {
        String name = milvusConfig.getCollectionName();
        boolean exists = milvusClient.hasCollection(HasCollectionReq.builder()
            .collectionName(name).build());
        if (!exists) {
            CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder()
                .build();
            schema.addField(CreateCollectionReq.FieldSchema.builder()
                .name("id").dataType(DataType.Int64)
                .isPrimaryKey(true).autoID(true).build());
            schema.addField(CreateCollectionReq.FieldSchema.builder()
                .name("document_id").dataType(DataType.Int64).build());
            schema.addField(CreateCollectionReq.FieldSchema.builder()
                .name("content").dataType(DataType.VarChar)
                .maxLength(2000).build());
            schema.addField(CreateCollectionReq.FieldSchema.builder()
                .name("doc_type").dataType(DataType.VarChar)
                .maxLength(50).build());
            schema.addField(CreateCollectionReq.FieldSchema.builder()
                .name("law_domain").dataType(DataType.VarChar)
                .maxLength(50).build());
            schema.addField(CreateCollectionReq.FieldSchema.builder()
                .name("embedding").dataType(DataType.FloatVector)
                .dimension(milvusConfig.getEmbeddingDim()).build());

            List<IndexParam> indexParams = List.of(
                IndexParam.builder()
                    .fieldName("embedding")
                    .indexType(IndexParam.IndexType.valueOf(milvusConfig.getIndexType()))
                    .metricType(IndexParam.MetricType.valueOf(milvusConfig.getMetricType()))
                    .extraParams(milvusConfig.getIndexParams())
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
        ensureCollection();
        Map<String, Object> row = new HashMap<>();
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
    public void batchInsert(List<Map<String, Object>> rows) {
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
        ensureCollection();
        SearchReq searchReq = SearchReq.builder()
            .collectionName(milvusConfig.getCollectionName())
            .data(List.of(new FloatVec(queryVector)))
            .topK(topK)
            .filter(filterExpr)
            .searchParams(milvusConfig.getSearchParams())
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
