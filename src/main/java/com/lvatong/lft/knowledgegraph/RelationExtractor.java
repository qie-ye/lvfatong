package com.lvatong.lft.knowledgegraph;

import com.lvatong.lft.model.graph.LegalEntity;
import com.lvatong.lft.model.graph.LegalRelation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class RelationExtractor {

    // 引用关系模式
    private static final Pattern CITE_PATTERN = Pattern.compile(
            "根据《[^》]+》第[一二三四五六七八九十百千零\\d]+条|依据《[^》]+》第[一二三四五六七八九十百千零\\d]+条");

    // 适用关系模式
    private static final Pattern APPLY_PATTERN = Pattern.compile(
            "适用《[^》]+》|按照《[^》]+》|参照《[^》]+》");

    // 相似关系模式
    private static final Pattern SIMILAR_PATTERN = Pattern.compile(
            "类似|相似|相同|相同|近似|类似");

    /**
     * 从文本中提取关系
     */
    public List<LegalRelation> extractRelations(String text, List<LegalEntity> entities) {
        List<LegalRelation> relations = new ArrayList<>();

        // 提取引用关系
        relations.addAll(extractCiteRelations(text, entities));

        // 提取适用关系
        relations.addAll(extractApplyRelations(text, entities));

        // 提取相似关系
        relations.addAll(extractSimilarRelations(text, entities));

        // 基于实体共现提取关系
        relations.addAll(extractCoOccurrenceRelations(text, entities));

        return relations;
    }

    /**
     * 提取引用关系
     */
    private List<LegalRelation> extractCiteRelations(String text, List<LegalEntity> entities) {
        List<LegalRelation> relations = new ArrayList<>();
        Matcher matcher = CITE_PATTERN.matcher(text);

        while (matcher.find()) {
            String citeText = matcher.group();
            // 提取被引用的法律法规和法条
            LegalEntity sourceEntity = findEntityByType(entities, LegalEntity.EntityType.ARTICLE);
            LegalEntity targetEntity = findEntityByType(entities, LegalEntity.EntityType.LAW);

            if (sourceEntity != null && targetEntity != null) {
                LegalRelation relation = new LegalRelation(targetEntity, LegalRelation.RelationType.CITES);
                relation.setDescription(citeText);
                relation.setSource("text_extraction");
                relations.add(relation);
            }
        }

        return relations;
    }

    /**
     * 提取适用关系
     */
    private List<LegalRelation> extractApplyRelations(String text, List<LegalEntity> entities) {
        List<LegalRelation> relations = new ArrayList<>();
        Matcher matcher = APPLY_PATTERN.matcher(text);

        while (matcher.find()) {
            String applyText = matcher.group();
            LegalEntity sourceEntity = findEntityByType(entities, LegalEntity.EntityType.LAW);
            LegalEntity targetEntity = findEntityByType(entities, LegalEntity.EntityType.ARTICLE);

            if (sourceEntity != null && targetEntity != null) {
                LegalRelation relation = new LegalRelation(targetEntity, LegalRelation.RelationType.APPLIES_TO);
                relation.setDescription(applyText);
                relation.setSource("text_extraction");
                relations.add(relation);
            }
        }

        return relations;
    }

    /**
     * 提取相似关系
     */
    private List<LegalRelation> extractSimilarRelations(String text, List<LegalEntity> entities) {
        List<LegalRelation> relations = new ArrayList<>();
        Matcher matcher = SIMILAR_PATTERN.matcher(text);

        while (matcher.find()) {
            // 找到相似关系，但需要更多上下文来确定具体实体
            // 这里简化处理，实际需要更复杂的逻辑
        }

        return relations;
    }

    /**
     * 基于实体共现提取关系
     */
    private List<LegalRelation> extractCoOccurrenceRelations(String text, List<LegalEntity> entities) {
        List<LegalRelation> relations = new ArrayList<>();

        // 如果两个实体在同一句话中出现，认为它们有关系
        String[] sentences = text.split("[。；！？]");
        for (String sentence : sentences) {
            List<LegalEntity> sentenceEntities = new ArrayList<>();
            for (LegalEntity entity : entities) {
                if (sentence.contains(entity.getName())) {
                    sentenceEntities.add(entity);
                }
            }

            // 为同一句话中的实体创建关系
            for (int i = 0; i < sentenceEntities.size(); i++) {
                for (int j = i + 1; j < sentenceEntities.size(); j++) {
                    LegalEntity entity1 = sentenceEntities.get(i);
                    LegalEntity entity2 = sentenceEntities.get(j);

                    if (!entity1.getId().equals(entity2.getId())) {
                        LegalRelation relation = new LegalRelation(entity2, LegalRelation.RelationType.RELATED_TO);
                        relation.setDescription("共现关系");
                        relation.setWeight(0.5);
                        relation.setSource("co_occurrence");
                        relations.add(relation);
                    }
                }
            }
        }

        return relations;
    }

    /**
     * 根据类型查找实体
     */
    private LegalEntity findEntityByType(List<LegalEntity> entities, LegalEntity.EntityType type) {
        for (LegalEntity entity : entities) {
            if (entity.getType() == type) {
                return entity;
            }
        }
        return null;
    }

    /**
     * 关系去重
     */
    public List<LegalRelation> deduplicateRelations(List<LegalRelation> relations) {
        List<LegalRelation> uniqueRelations = new ArrayList<>();
        for (LegalRelation relation : relations) {
            boolean isDuplicate = false;
            for (LegalRelation unique : uniqueRelations) {
                if (unique.getTarget().getId().equals(relation.getTarget().getId()) &&
                    unique.getType() == relation.getType()) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                uniqueRelations.add(relation);
            }
        }
        return uniqueRelations;
    }
}