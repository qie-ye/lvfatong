package com.lvatong.lft.model.graph;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GraphQueryResult {

    private List<LegalEntity> entities;
    private List<LegalRelation> relations;
    private Map<String, Object> metadata;

    public GraphQueryResult() {
    }

    public GraphQueryResult(List<LegalEntity> entities, List<LegalRelation> relations) {
        this.entities = entities;
        this.relations = relations;
    }

    public GraphQueryResult(List<LegalEntity> entities, List<LegalRelation> relations, 
                           Map<String, Object> metadata) {
        this.entities = entities;
        this.relations = relations;
        this.metadata = metadata;
    }
}