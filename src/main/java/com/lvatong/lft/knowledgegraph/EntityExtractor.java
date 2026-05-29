package com.lvatong.lft.knowledgegraph;

import com.lvatong.lft.model.graph.LegalEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class EntityExtractor {

    // 法律法规名称模式
    private static final Pattern LAW_PATTERN = Pattern.compile(
            "《[^》]+》|[^，。；！？\\s]{2,}(?:法|条例|规定|办法|细则|准则|标准)");

    // 法条引用模式
    private static final Pattern ARTICLE_PATTERN = Pattern.compile(
            "第[一二三四五六七八九十百千零\\d]+条|第[一二三四五六七八九十百千零\\d]+款");

    // 案例编号模式
    private static final Pattern CASE_PATTERN = Pattern.compile(
            "\\(\\d{4}\\)[^\\d]+\\d+号|\\[\\d{4}\\][^\\d]+\\d+号");

    // 法律概念模式
    private static final Pattern CONCEPT_PATTERN = Pattern.compile(
            "(?:是指|包括|分为|属于|构成|成立|生效|无效|撤销|解除|终止|变更|转让|继承|侵权|违约|赔偿|处罚|拘留|逮捕|起诉|审判|执行|调解|仲裁|诉讼|复议|申诉|上诉|抗诉)");

    /**
     * 从文本中提取法律实体
     */
    public List<LegalEntity> extractEntities(String text) {
        List<LegalEntity> entities = new ArrayList<>();

        // 提取法律法规
        entities.addAll(extractLaws(text));

        // 提取法条
        entities.addAll(extractArticles(text));

        // 提取案例
        entities.addAll(extractCases(text));

        // 提取法律概念
        entities.addAll(extractConcepts(text));

        return entities;
    }

    /**
     * 提取法律法规
     */
    private List<LegalEntity> extractLaws(String text) {
        List<LegalEntity> entities = new ArrayList<>();
        Matcher matcher = LAW_PATTERN.matcher(text);

        while (matcher.find()) {
            String name = matcher.group();
            LegalEntity entity = new LegalEntity();
            entity.setName(name);
            entity.setType(LegalEntity.EntityType.LAW);
            entity.setSource("text_extraction");
            entities.add(entity);
        }

        return entities;
    }

    /**
     * 提取法条
     */
    private List<LegalEntity> extractArticles(String text) {
        List<LegalEntity> entities = new ArrayList<>();
        Matcher matcher = ARTICLE_PATTERN.matcher(text);

        while (matcher.find()) {
            String name = matcher.group();
            LegalEntity entity = new LegalEntity();
            entity.setName(name);
            entity.setType(LegalEntity.EntityType.ARTICLE);
            entity.setSource("text_extraction");
            entities.add(entity);
        }

        return entities;
    }

    /**
     * 提取案例
     */
    private List<LegalEntity> extractCases(String text) {
        List<LegalEntity> entities = new ArrayList<>();
        Matcher matcher = CASE_PATTERN.matcher(text);

        while (matcher.find()) {
            String name = matcher.group();
            LegalEntity entity = new LegalEntity();
            entity.setName(name);
            entity.setType(LegalEntity.EntityType.CASE);
            entity.setSource("text_extraction");
            entities.add(entity);
        }

        return entities;
    }

    /**
     * 提取法律概念
     */
    private List<LegalEntity> extractConcepts(String text) {
        List<LegalEntity> entities = new ArrayList<>();
        Matcher matcher = CONCEPT_PATTERN.matcher(text);

        while (matcher.find()) {
            String name = matcher.group();
            LegalEntity entity = new LegalEntity();
            entity.setName(name);
            entity.setType(LegalEntity.EntityType.CONCEPT);
            entity.setSource("text_extraction");
            entities.add(entity);
        }

        return entities;
    }

    /**
     * 实体去重
     */
    public List<LegalEntity> deduplicateEntities(List<LegalEntity> entities) {
        List<LegalEntity> uniqueEntities = new ArrayList<>();
        for (LegalEntity entity : entities) {
            boolean isDuplicate = false;
            for (LegalEntity unique : uniqueEntities) {
                if (unique.getName().equals(entity.getName()) && 
                    unique.getType() == entity.getType()) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                uniqueEntities.add(entity);
            }
        }
        return uniqueEntities;
    }
}