package com.lvatong.lft.model.graph;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@RelationshipProperties
public class LegalRelation {

    @Id
    @GeneratedValue
    private String id;

    @TargetNode
    private LegalEntity target;

    @Property("type")
    private RelationType type;

    @Property("description")
    private String description;

    @Property("weight")
    private double weight;

    @Property("source")
    private String source;

    @Property("createdAt")
    private LocalDateTime createdAt;

    public enum RelationType {
        CITES,          // 引用
        SIMILAR_TO,     // 相似
        APPLIES_TO,     // 适用
        CONTAINS,       // 包含
        SPECIALIZES_IN, // 专长
        RELATED_TO      // 相关
    }

    public LegalRelation() {
        this.id = UUID.randomUUID().toString();
        this.weight = 1.0;
        this.createdAt = LocalDateTime.now();
    }

    public LegalRelation(LegalEntity target, RelationType type) {
        this();
        this.target = target;
        this.type = type;
    }

    public LegalRelation(LegalEntity target, RelationType type, String description) {
        this(target, type);
        this.description = description;
    }
}