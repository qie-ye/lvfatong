package com.lvatong.lft.model.graph;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Node("LegalEntity")
public class LegalEntity {

    @Id
    @GeneratedValue
    private String id;

    @Property("name")
    private String name;

    @Property("type")
    private EntityType type;

    @Property("description")
    private String description;

    @Property("source")
    private String source;

    @Property("sourceId")
    private String sourceId;

    @Property("metadata")
    private String metadata;

    @Property("createdAt")
    private LocalDateTime createdAt;

    @Property("updatedAt")
    private LocalDateTime updatedAt;

    public enum EntityType {
        LAW,        // 法律法规
        ARTICLE,    // 法条
        CASE,       // 案例
        CONCEPT,    // 法律概念
        LAWYER,     // 律师
        USER        // 用户
    }

    public LegalEntity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public LegalEntity(String name, EntityType type) {
        this();
        this.name = name;
        this.type = type;
    }
}