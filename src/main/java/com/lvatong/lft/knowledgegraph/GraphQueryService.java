package com.lvatong.lft.knowledgegraph;

import com.lvatong.lft.model.graph.LegalEntity;
import com.lvatong.lft.model.graph.LegalRelation;
import com.lvatong.lft.model.graph.GraphQueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphQueryService {

    private final Driver neo4jDriver;

    /**
     * 查询实体详情
     */
    public LegalEntity getEntityById(String entityId) {
        try (Session session = neo4jDriver.session()) {
            String cypher = "MATCH (e:LegalEntity {id: $id}) RETURN e";
            Result result = session.run(cypher, Map.of("id", entityId));

            if (result.hasNext()) {
                Record record = result.next();
                return mapToEntity(record.get("e").asMap());
            }
        }
        return null;
    }

    /**
     * 查询实体关系
     */
    public List<LegalRelation> getEntityRelations(String entityId) {
        List<LegalRelation> relations = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            String cypher = """
                    MATCH (source:LegalEntity {id: $entityId})-[r:RELATES_TO]->(target:LegalEntity)
                    RETURN target, r
                    """;
            Result result = session.run(cypher, Map.of("entityId", entityId));

            while (result.hasNext()) {
                Record record = result.next();
                LegalEntity target = mapToEntity(record.get("target").asMap());
                LegalRelation relation = mapToRelation(record.get("r").asMap(), target);
                relations.add(relation);
            }
        }
        return relations;
    }

    /**
     * 查询相关实体
     */
    public List<LegalEntity> getRelatedEntities(String entityId, int depth) {
        List<LegalEntity> entities = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            String cypher = "MATCH (start:LegalEntity {id: $entityId})-[*1.." + depth + "]->(related:LegalEntity) " +
                    "RETURN DISTINCT related LIMIT 50";
            Result result = session.run(cypher, Map.of("entityId", entityId));

            while (result.hasNext()) {
                Record record = result.next();
                entities.add(mapToEntity(record.get("related").asMap()));
            }
        }
        return entities;
    }

    /**
     * 图谱问答
     */
    public GraphQueryResult queryGraph(String question) {
        // 简单的关键词提取和匹配
        List<String> keywords = extractKeywords(question);
        List<LegalEntity> entities = new ArrayList<>();
        List<LegalRelation> relations = new ArrayList<>();

        for (String keyword : keywords) {
            try (Session session = neo4jDriver.session()) {
                String cypher = """
                        MATCH (e:LegalEntity)
                        WHERE e.name CONTAINS $keyword
                        RETURN e
                        LIMIT 5
                        """;
                Result result = session.run(cypher, Map.of("keyword", keyword));

                while (result.hasNext()) {
                    Record record = result.next();
                    LegalEntity entity = mapToEntity(record.get("e").asMap());
                    if (!entities.contains(entity)) {
                        entities.add(entity);
                    }
                }
            }
        }

        // 获取实体关系
        for (LegalEntity entity : entities) {
            List<LegalRelation> entityRelations = getEntityRelations(entity.getId());
            relations.addAll(entityRelations);
        }

        return new GraphQueryResult(entities, relations);
    }

    /**
     * 获取可视化数据
     */
    public Map<String, Object> getVisualizationData(String entityId) {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        // 获取中心实体
        LegalEntity centerEntity = getEntityById(entityId);
        if (centerEntity != null) {
            nodes.add(createNode(centerEntity, true));

            // 获取相关实体和关系
            List<LegalRelation> relations = getEntityRelations(entityId);
            for (LegalRelation relation : relations) {
                LegalEntity target = relation.getTarget();
                nodes.add(createNode(target, false));
                edges.add(createEdge(centerEntity.getId(), target.getId(), relation));
            }
        }

        data.put("nodes", nodes);
        data.put("edges", edges);
        return data;
    }

    /**
     * 创建节点数据
     */
    private Map<String, Object> createNode(LegalEntity entity, boolean isCenter) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", entity.getId());
        node.put("name", entity.getName());
        node.put("type", entity.getType().name());
        node.put("description", entity.getDescription());
        node.put("isCenter", isCenter);
        return node;
    }

    /**
     * 创建边数据
     */
    private Map<String, Object> createEdge(String sourceId, String targetId, LegalRelation relation) {
        Map<String, Object> edge = new HashMap<>();
        edge.put("source", sourceId);
        edge.put("target", targetId);
        edge.put("type", relation.getType().name());
        edge.put("description", relation.getDescription());
        edge.put("weight", relation.getWeight());
        return edge;
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        // 简单的关键词提取，实际可以使用NLP库
        String[] words = text.split("[\\s，。；！？、]+");
        for (String word : words) {
            if (word.length() >= 2) {
                keywords.add(word);
            }
        }
        return keywords;
    }

    /**
     * 将Neo4j记录映射为实体
     */
    private LegalEntity mapToEntity(Map<String, Object> map) {
        LegalEntity entity = new LegalEntity();
        entity.setId(map.get("id") != null ? map.get("id").toString() : "");
        entity.setName(map.get("name") != null ? map.get("name").toString() : "");
        entity.setType(LegalEntity.EntityType.valueOf(map.get("type").toString()));
        entity.setDescription(map.get("description") != null ? map.get("description").toString() : "");
        entity.setSource(map.get("source") != null ? map.get("source").toString() : "");
        entity.setSourceId(map.get("sourceId") != null ? map.get("sourceId").toString() : "");
        return entity;
    }

    /**
     * 将Neo4j记录映射为关系
     */
    private LegalRelation mapToRelation(Map<String, Object> map, LegalEntity target) {
        LegalRelation relation = new LegalRelation();
        relation.setId(map.get("id") != null ? map.get("id").toString() : "");
        relation.setTarget(target);
        relation.setType(LegalRelation.RelationType.valueOf(map.get("type").toString()));
        relation.setDescription(map.get("description") != null ? map.get("description").toString() : "");
        relation.setWeight(map.get("weight") != null ? Double.parseDouble(map.get("weight").toString()) : 1.0);
        relation.setSource(map.get("source") != null ? map.get("source").toString() : "");
        return relation;
    }
}