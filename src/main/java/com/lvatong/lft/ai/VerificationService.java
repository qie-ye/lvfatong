package com.lvatong.lft.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lvatong.lft.model.dto.ContractAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final ZhipuApiClient zhipuApiClient;
    private final PromptTemplateService promptTemplateService;
    private final ModelRouterService modelRouterService;

    public record VerificationResult(boolean passed, double score, List<String> issues, String suggestedFix) {}

    public record ContractConflictResult(boolean consistent, double score, List<String> conflicts) {}

    /**
     * 验证法律回答的准确性（法条引用 / 法律适用 / 逻辑自洽），失败时 fail-open
     */
    public VerificationResult verifyLegalAnswer(String question, String answer, String context) {
        try {
            String prompt = promptTemplateService.buildVerificationPrompt(question, answer, context);
            String model = modelRouterService.getModelForTask(ModelRouterService.TaskType.LEGAL_QA);
            List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", prompt));
            String response = zhipuApiClient.chat(model, messages, 0.1, 512);
            VerificationResult result = parseVerificationResult(response);
            log.info("Verification: passed={}, score={}", result.passed(), String.format("%.2f", result.score()));
            return result;
        } catch (Exception e) {
            log.warn("Legal answer verification failed (fail-open): {}", e.getMessage());
            return new VerificationResult(true, 0.75, List.of(), "");
        }
    }

    /**
     * 合同分析规则-AI交叉验证：检测规则判高风险但AI判低风险的条款
     */
    public ContractConflictResult detectContractConflicts(
            List<ContractAnalysisResult.ClauseAnalysis> ruleResults,
            ContractAnalysisResult aiResult) {
        List<String> conflicts = new ArrayList<>();

        if (aiResult.getClauses() == null || aiResult.getClauses().isEmpty()) {
            return new ContractConflictResult(true, 0.85, conflicts);
        }

        for (ContractAnalysisResult.ClauseAnalysis ruleClause : ruleResults) {
            if (!"高".equals(ruleClause.getRiskLevel())) continue;
            aiResult.getClauses().stream()
                    .filter(ai -> ai.getIndex() == ruleClause.getIndex()
                            && ai.getRiskLevel() != null
                            && !"高".equals(ai.getRiskLevel()))
                    .findFirst()
                    .ifPresent(ai -> conflicts.add(
                            "条款" + ruleClause.getIndex() + "（" + ruleClause.getTitle()
                                    + "）：规则引擎判高风险，AI判" + ai.getRiskLevel() + "风险"));
        }

        double score = Math.max(0.5, 0.95 - conflicts.size() * 0.15);
        if (!conflicts.isEmpty()) {
            log.info("Contract conflict detected: {} issues, score={}", conflicts.size(),
                    String.format("%.2f", score));
        }
        return new ContractConflictResult(conflicts.isEmpty(), score, conflicts);
    }

    private VerificationResult parseVerificationResult(String response) {
        try {
            String jsonStr = extractJson(response);
            JSONObject json = JSON.parseObject(jsonStr);
            boolean passed = json.getBooleanValue("passed");
            double score = json.getDoubleValue("score");
            if (score == 0.0) score = passed ? 0.8 : 0.4;

            List<String> issues = new ArrayList<>();
            JSONArray issuesArray = json.getJSONArray("issues");
            if (issuesArray != null) {
                issuesArray.forEach(item -> issues.add(item.toString()));
            }

            String suggestedFix = json.getString("suggestedFix");
            return new VerificationResult(passed, score, issues, suggestedFix != null ? suggestedFix : "");
        } catch (Exception e) {
            log.debug("Verification JSON parse failed, fallback heuristic: {}", e.getMessage());
            boolean passed = !response.contains("不通过")
                    && !response.contains("\"passed\":false")
                    && !response.contains("\"passed\": false");
            return new VerificationResult(passed, passed ? 0.7 : 0.4, List.of(), "");
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        return (start >= 0 && end > start) ? response.substring(start, end + 1) : response;
    }
}
