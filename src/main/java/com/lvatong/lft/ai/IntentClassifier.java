package com.lvatong.lft.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentClassifier {

    private final ZhipuApiClient zhipuApiClient;
    private final ModelRouterService modelRouterService;

    private static final String INTENT_SYSTEM_PROMPT = """
            你是意图分类器，判断用户输入的意图类型。
            只返回以下六种类型之一，不要返回其他内容：
            - LEGAL_QA：法律咨询（如"我该怎么办""是否违法""有什么权利"）
            - LAW_QUERY：法条查询（如"民法典第143条""劳动法怎么规定""刑法第xxx条"）
            - CONTRACT_QUESTION：合同问题（如"合同""签约""违约""条款""定金""解约"）
            - CASE_QUERY：案例查询（如"有没有类似案例""法院怎么判""类似情况判决结果"）
            - COMPLEX_LEGAL：复杂法律问题（涉及多个法律领域、情况复杂、需要综合分析）
            - CHAT：闲聊（非法律相关的日常对话）
            
            只返回类型名称，不要解释。
            """;

    private static final double CONFIDENCE_LOW = 0.6;
    private static final double CONFIDENCE_MEDIUM = 0.8;

    private static final int MAX_CACHE_SIZE = 500;
    private final ConcurrentHashMap<String, IntentResult> intentCache = new ConcurrentHashMap<>();

    public record IntentResult(PromptTemplateService.IntentType type, double confidence) {}

    private static final List<Pattern> LAW_QUERY_PATTERNS = List.of(
            Pattern.compile("第\\d+条"),
            Pattern.compile("(?:民法典|刑法|劳动法|公司法|合同法|宪法|行政法|民诉法|刑诉法).*(?:第|规定|条文|条款)"),
            Pattern.compile("(?:什么|哪|怎么).*(?:规定|条文|法条|法律)"),
            Pattern.compile(".*(?:法条|条文|条款|规定).*(?:是什么|有哪些|怎么写)")
    );

    private static final List<Pattern> CONTRACT_PATTERNS = List.of(
            Pattern.compile("合同"),
            Pattern.compile("(?:签约|解约|违约|毁约)"),
            Pattern.compile("(?:定金|订金|押金|保证金)"),
            Pattern.compile("(?:条款|协议|租赁|买卖|劳务)"),
            Pattern.compile("(?:甲方|乙方|双方)")
    );

    private static final List<Pattern> LEGAL_QA_PATTERNS = List.of(
            Pattern.compile("(?:违法|犯罪|侵权|维权|起诉|上诉|申诉)"),
            Pattern.compile("(?:我该怎么|是否可以|有没有权利|能不能)"),
            Pattern.compile("(?:赔偿|补偿|损失|伤害|工伤)"),
            Pattern.compile("(?:离婚|抚养|继承|遗产|赡养)"),
            Pattern.compile("(?:劳动|工资|社保|加班|辞退|解雇)"),
            Pattern.compile("(?:房产|房屋|物业|拆迁|租赁)")
    );

    private static final List<Pattern> CASE_QUERY_PATTERNS = List.of(
            Pattern.compile("(?:案例|案件|判例|先例)"),
            Pattern.compile("(?:法院|法官|判决|裁定|裁判)"),
            Pattern.compile("(?:胜诉|败诉|判了多少|怎么判的)"),
            Pattern.compile("(?:类似情况|有没有.*例子|有无先例|参考案例)")
    );

    private static final List<Pattern> COMPLEX_LEGAL_PATTERNS = List.of(
            Pattern.compile("(?:同时|既.*又|一方面.*另一方面)"),
            Pattern.compile("(?:综合|多个.*问题|几种情况|涉及多)"),
            Pattern.compile("(?:复杂|多方面|多种法律|多重)")
    );

    /**
     * 分类用户意图并返回置信度（规则引擎优先，低置信度时调用AI）
     */
    public IntentResult classifyWithConfidence(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return new IntentResult(PromptTemplateService.IntentType.CHAT, 1.0);
        }

        String cacheKey = userInput.trim().toLowerCase();
        IntentResult cached = intentCache.get(cacheKey);
        if (cached != null) {
            log.debug("Intent cached: {} ({}) for: {}", cached.type(), cached.confidence(), truncate(userInput, 50));
            return cached;
        }

        IntentResult ruleResult = classifyByRules(userInput);
        if (ruleResult != null) {
            log.debug("Intent by rules: {} ({}) for: {}", ruleResult.type(), ruleResult.confidence(), truncate(userInput, 50));
            putCache(cacheKey, ruleResult);
            return ruleResult;
        }

        IntentResult aiResult = classifyByAI(userInput);
        log.debug("Intent by AI: {} ({}) for: {}", aiResult.type(), aiResult.confidence(), truncate(userInput, 50));
        putCache(cacheKey, aiResult);
        return aiResult;
    }

    /**
     * 向后兼容：仅返回意图类型
     */
    public PromptTemplateService.IntentType classify(String userInput) {
        return classifyWithConfidence(userInput).type();
    }

    private void putCache(String key, IntentResult value) {
        if (intentCache.size() < MAX_CACHE_SIZE) {
            intentCache.put(key, value);
        }
    }

    /**
     * 基于规则引擎的意图分类（带置信度）
     */
    private IntentResult classifyByRules(String input) {
        String trimmed = input.trim();

        int lawQueryScore = scorePatterns(trimmed, LAW_QUERY_PATTERNS);
        int contractScore = scorePatterns(trimmed, CONTRACT_PATTERNS);
        int legalQaScore = scorePatterns(trimmed, LEGAL_QA_PATTERNS);
        int caseQueryScore = scorePatterns(trimmed, CASE_QUERY_PATTERNS);
        int complexScore = scorePatterns(trimmed, COMPLEX_LEGAL_PATTERNS);

        // COMPLEX_LEGAL: explicit complexity markers OR long question hitting multiple categories
        int categoriesHit = (lawQueryScore > 0 ? 1 : 0) + (contractScore > 0 ? 1 : 0)
                + (legalQaScore > 0 ? 1 : 0) + (caseQueryScore > 0 ? 1 : 0);
        if (complexScore >= 1 || (categoriesHit >= 2 && trimmed.length() > 80)) {
            return new IntentResult(PromptTemplateService.IntentType.COMPLEX_LEGAL, 0.75);
        }

        // CASE_QUERY
        if (caseQueryScore >= 1 && caseQueryScore >= lawQueryScore && caseQueryScore >= contractScore) {
            return new IntentResult(PromptTemplateService.IntentType.CASE_QUERY,
                    caseQueryScore >= 2 ? CONFIDENCE_MEDIUM : CONFIDENCE_LOW);
        }

        // LAW_QUERY
        if (lawQueryScore >= 2 && lawQueryScore > contractScore && lawQueryScore > legalQaScore) {
            return new IntentResult(PromptTemplateService.IntentType.LAW_QUERY, CONFIDENCE_MEDIUM);
        }
        if (lawQueryScore >= 1 && lawQueryScore > contractScore && lawQueryScore > legalQaScore) {
            return new IntentResult(PromptTemplateService.IntentType.LAW_QUERY, CONFIDENCE_LOW);
        }

        // CONTRACT_QUESTION
        if (contractScore >= 2 && contractScore >= legalQaScore) {
            return new IntentResult(PromptTemplateService.IntentType.CONTRACT_QUESTION, CONFIDENCE_MEDIUM);
        }
        if (contractScore >= 1 && contractScore > legalQaScore) {
            return new IntentResult(PromptTemplateService.IntentType.CONTRACT_QUESTION, CONFIDENCE_LOW);
        }

        // LEGAL_QA
        if (legalQaScore >= 2) {
            return new IntentResult(PromptTemplateService.IntentType.LEGAL_QA, CONFIDENCE_MEDIUM);
        }
        if (legalQaScore >= 1) {
            return new IntentResult(PromptTemplateService.IntentType.LEGAL_QA, CONFIDENCE_LOW);
        }

        return null;
    }

    private int scorePatterns(String input, List<Pattern> patterns) {
        int score = 0;
        for (Pattern pattern : patterns) {
            if (pattern.matcher(input).find()) {
                score++;
            }
        }
        return score;
    }

    /**
     * 基于AI的意图分类（规则无法判断时使用，返回中等置信度）
     */
    private IntentResult classifyByAI(String input) {
        try {
            String model = modelRouterService.getModelForTask(ModelRouterService.TaskType.LEGAL_QA);
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", INTENT_SYSTEM_PROMPT),
                    Map.of("role", "user", "content", input)
            );
            String result = zhipuApiClient.chat(model, messages, 0.1, 32).trim();
            return new IntentResult(parseIntentType(result), 0.70);
        } catch (Exception e) {
            log.warn("AI intent classification failed, defaulting to LEGAL_QA: {}", e.getMessage());
            return new IntentResult(PromptTemplateService.IntentType.LEGAL_QA, 0.50);
        }
    }

    private PromptTemplateService.IntentType parseIntentType(String result) {
        try {
            return PromptTemplateService.IntentType.valueOf(result);
        } catch (IllegalArgumentException e) {
            if (result.contains("LAW_QUERY")) return PromptTemplateService.IntentType.LAW_QUERY;
            if (result.contains("CONTRACT")) return PromptTemplateService.IntentType.CONTRACT_QUESTION;
            if (result.contains("CASE")) return PromptTemplateService.IntentType.CASE_QUERY;
            if (result.contains("COMPLEX")) return PromptTemplateService.IntentType.COMPLEX_LEGAL;
            if (result.contains("CHAT")) return PromptTemplateService.IntentType.CHAT;
            return PromptTemplateService.IntentType.LEGAL_QA;
        }
    }

    private String truncate(String text, int maxLen) {
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
