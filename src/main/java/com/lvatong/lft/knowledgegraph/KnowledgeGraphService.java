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
public class KnowledgeGraphService {

    private final Driver neo4jDriver;
    private final EntityExtractor entityExtractor;
    private final RelationExtractor relationExtractor;

    /**
     * 从文本构建知识图谱
     */
    public void buildGraphFromText(String text, String sourceId) {
        log.info("开始从文本构建知识图谱: sourceId={}", sourceId);

        // 提取实体
        List<LegalEntity> entities = entityExtractor.extractEntities(text);
        entities = entityExtractor.deduplicateEntities(entities);

        // 提取关系
        List<LegalRelation> relations = relationExtractor.extractRelations(text, entities);
        relations = relationExtractor.deduplicateRelations(relations);

        // 存储到Neo4j
        saveEntities(entities, sourceId);
        saveRelations(relations, sourceId);

        log.info("知识图谱构建完成: 实体数={}, 关系数={}", entities.size(), relations.size());
    }

    /**
     * 保存实体到Neo4j
     */
    private void saveEntities(List<LegalEntity> entities, String sourceId) {
        try (Session session = neo4jDriver.session()) {
            for (LegalEntity entity : entities) {
                entity.setSourceId(sourceId);
                String cypher = """
                        MERGE (e:LegalEntity {name: $name, type: $type})
                        SET e.description = $description,
                            e.source = $source,
                            e.sourceId = $sourceId,
                            e.updatedAt = datetime()
                        ON CREATE SET e.createdAt = datetime()
                        """;
                session.run(cypher, Map.of(
                        "name", entity.getName(),
                        "type", entity.getType().name(),
                        "description", entity.getDescription() != null ? entity.getDescription() : "",
                        "source", entity.getSource() != null ? entity.getSource() : "",
                        "sourceId", sourceId != null ? sourceId : ""
                ));
            }
        }
    }

    /**
     * 保存关系到Neo4j
     */
    private void saveRelations(List<LegalRelation> relations, String sourceId) {
        try (Session session = neo4jDriver.session()) {
            for (LegalRelation relation : relations) {
                String cypher = """
                        MATCH (source:LegalEntity {sourceId: $sourceId})
                        MATCH (target:LegalEntity {name: $targetName, type: $targetType})
                        MERGE (source)-[r:RELATES_TO {type: $relationType}]->(target)
                        SET r.description = $description,
                            r.weight = $weight,
                            r.source = $source,
                            r.updatedAt = datetime()
                        ON CREATE SET r.createdAt = datetime()
                        """;
                session.run(cypher, Map.of(
                        "sourceId", sourceId,
                        "targetName", relation.getTarget().getName(),
                        "targetType", relation.getTarget().getType().name(),
                        "relationType", relation.getType().name(),
                        "description", relation.getDescription() != null ? relation.getDescription() : "",
                        "weight", relation.getWeight(),
                        "source", relation.getSource() != null ? relation.getSource() : ""
                ));
            }
        }
    }

    /**
     * 查询实体
     */
    public List<LegalEntity> findEntities(String name, LegalEntity.EntityType type, int limit) {
        List<LegalEntity> entities = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            String cypher = """
                    MATCH (e:LegalEntity)
                    WHERE ($name IS NULL OR e.name CONTAINS $name)
                      AND ($type IS NULL OR e.type = $type)
                    RETURN e
                    LIMIT $limit
                    """;
            Result result = session.run(cypher, Map.of(
                    "name", name != null ? name : "",
                    "type", type != null ? type.name() : "",
                    "limit", limit
            ));

            while (result.hasNext()) {
                Record record = result.next();
                entities.add(mapToEntity(record.get("e").asMap()));
            }
        }
        return entities;
    }

    /**
     * 查询实体关系
     */
    public List<LegalRelation> findEntityRelations(String entityId) {
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
     * 图谱搜索
     */
    public GraphQueryResult search(String query, int limit) {
        List<LegalEntity> entities = new ArrayList<>();
        List<LegalRelation> relations = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            // 搜索实体
            String entityCypher = """
                    MATCH (e:LegalEntity)
                    WHERE e.name CONTAINS $query
                    RETURN e
                    LIMIT $limit
                    """;
            Result entityResult = session.run(entityCypher, Map.of("query", query, "limit", limit));

            while (entityResult.hasNext()) {
                Record record = entityResult.next();
                entities.add(mapToEntity(record.get("e").asMap()));
            }

            // 搜索关系
            for (LegalEntity entity : entities) {
                relations.addAll(findEntityRelations(entity.getId()));
            }
        }

        return new GraphQueryResult(entities, relations);
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