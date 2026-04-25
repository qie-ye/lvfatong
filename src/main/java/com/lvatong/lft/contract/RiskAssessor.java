package com.lvatong.lft.contract;

import com.lvatong.lft.model.dto.ContractAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RiskAssessor {

    // Pre-compiled risk detection patterns
    private static final Pattern P_BREACH_PENALTY_HIGH = Pattern.compile("违约金.*[2-9]\\d%|违约金.*百分之[二三四五六七八九]十");
    private static final Pattern P_UNLIMITED_LIABILITY = Pattern.compile("承担一切.*(?:损失|责任)|无限(?:连带)?责任|全部赔偿");
    private static final Pattern P_UNILATERAL_TERMINATION = Pattern.compile("(?:甲方|一方).*(?:有权|可以).*(?:单方|随时).*(?:变更|解除|终止)");
    private static final Pattern P_COUNTERPART_TERMINATION = Pattern.compile("(?:乙方|另一方|对方).*(?:有权|可以).*(?:变更|解除|终止)");
    private static final Pattern P_DISPUTE_RESOLUTION = Pattern.compile("仲裁|诉讼|争议解决|管辖");
    private static final Pattern P_FORCE_MAJEURE = Pattern.compile("不可抗力");
    private static final Pattern P_CONFIDENTIALITY = Pattern.compile("保密");
    private static final Pattern P_CONFIDENTIALITY_PERIOD = Pattern.compile("保密期限|保密.*(?:\\d+年|期限)");
    private static final Pattern P_STANDARD_CLAUSE = Pattern.compile("(?:免除|排除).*(?:己方|自身|本方).*(?:责任|义务)|最终解释权");
    private static final Pattern P_VAGUE_EXPRESSION = Pattern.compile("(?:适当|合理|视情况|另行协商|酌情).*(?:赔偿|补偿|处理)");

    /**
     * 规则引擎：基于法律专家知识规则的风险评估
     */
    public List<ContractAnalysisResult.ClauseAnalysis> assess(List<ClauseExtractor.Clause> clauses) {
        List<ContractAnalysisResult.ClauseAnalysis> results = new ArrayList<>();
        for (ClauseExtractor.Clause clause : clauses) {
            ContractAnalysisResult.ClauseAnalysis analysis = new ContractAnalysisResult.ClauseAnalysis();
            analysis.setIndex(clause.getIndex());
            analysis.setTitle(clause.getTitle());
            analysis.setContent(truncate(clause.getContent(), 500));

            List<RiskItem> risks = detectRisks(clause.getContent());
            if (risks.isEmpty()) {
                analysis.setRiskLevel("低");
                analysis.setRiskCategory("");
                analysis.setDescription("未发现明显风险");
                analysis.setLegalBasis("");
                analysis.setSuggestion("");
            } else {
                RiskItem highest = risks.stream()
                        .reduce((a, b) -> riskWeight(a.level) >= riskWeight(b.level) ? a : b)
                        .orElse(risks.get(0));
                analysis.setRiskLevel(highest.level);
                analysis.setRiskCategory(highest.category);
                analysis.setDescription(highest.description);
                analysis.setLegalBasis(highest.legalBasis);
                analysis.setSuggestion(highest.suggestion);
            }

            results.add(analysis);
        }
        return results;
    }

    /**
     * 计算整体风险等级
     */
    public String calculateOverallRisk(List<ContractAnalysisResult.ClauseAnalysis> clauses) {
        long highCount = clauses.stream().filter(c -> "高".equals(c.getRiskLevel())).count();
        long mediumCount = clauses.stream().filter(c -> "中".equals(c.getRiskLevel())).count();

        if (highCount >= 2) return "高";
        if (highCount >= 1 || mediumCount >= 3) return "中";
        return "低";
    }

    private List<RiskItem> detectRisks(String content) {
        List<RiskItem> risks = new ArrayList<>();
        String lower = content.toLowerCase();

        // 违约金过高风险
        if (P_BREACH_PENALTY_HIGH.matcher(lower).find()) {
            risks.add(new RiskItem("高", "条款缺陷",
                    "违约金比例可能过高，超过实际损失的合理范围",
                    "根据《民法典》第585条，约定的违约金过分高于造成的损失的，法院可以适当减少",
                    "建议将违约金比例调整至合理范围（一般不超过合同标的30%）"));
        }

        // 无限责任条款
        if (P_UNLIMITED_LIABILITY.matcher(lower).find()) {
            risks.add(new RiskItem("高", "条款缺陷",
                    "存在无限责任条款，可能导致一方承担过重的赔偿责任",
                    "根据《民法典》第584条，赔偿损失不得超过违约方订立合同时可预见的损失",
                    "建议明确赔偿范围和上限，增加责任限制条款"));
        }

        // 单方变更/解除权
        if (P_UNILATERAL_TERMINATION.matcher(lower).find() &&
                !P_COUNTERPART_TERMINATION.matcher(lower).find()) {
            risks.add(new RiskItem("高", "条款缺陷",
                    "单方面赋予一方变更或解除合同的权利，权利义务不对等",
                    "根据《民法典》第543条，当事人协商一致可以变更合同",
                    "建议设定对等的解除条件，或明确单方解除的具体条件和补偿机制"));
        }

        // 争议解决条款缺失
        if (!P_DISPUTE_RESOLUTION.matcher(lower).find()) {
            risks.add(new RiskItem("中", "条款缺陷",
                    "缺少争议解决条款，发生纠纷时可能导致管辖不明",
                    "建议约定明确的争议解决方式（仲裁或诉讼）",
                    "建议增加争议解决条款，明确仲裁机构或管辖法院"));
        }

        // 不可抗力条款缺失
        if (!P_FORCE_MAJEURE.matcher(lower).find()) {
            risks.add(new RiskItem("中", "条款缺陷",
                    "缺少不可抗力条款",
                    "根据《民法典》第180条，不可抗力是不能预见、不能避免且不能克服的客观情况",
                    "建议增加不可抗力条款，明确不可抗力的定义、通知义务和法律后果"));
        }

        // 保密条款审查
        if (P_CONFIDENTIALITY.matcher(lower).find() && !P_CONFIDENTIALITY_PERIOD.matcher(lower).find()) {
            risks.add(new RiskItem("中", "表述歧义",
                    "保密条款未约定保密期限，可能导致无限期保密义务",
                    "",
                    "建议明确保密期限和保密信息的范围"));
        }

        // 格式条款风险
        if (P_STANDARD_CLAUSE.matcher(lower).find()) {
            risks.add(new RiskItem("高", "法律适用错误",
                    "包含免除己方责任或'最终解释权'等格式条款，可能无效",
                    "根据《民法典》第497条，不合理地免除或减轻其责任的格式条款无效",
                    "建议删除此类条款，确保双方权利义务对等"));
        }

        // 模糊表述
        if (P_VAGUE_EXPRESSION.matcher(lower).find()) {
            risks.add(new RiskItem("中", "表述歧义",
                    "关键条款使用模糊表述（如'适当''合理'），缺乏明确标准",
                    "",
                    "建议将模糊表述替换为具体数额、比例或计算方式"));
        }

        return risks;
    }

    private int riskWeight(String level) {
        return switch (level) {
            case "高" -> 3;
            case "中" -> 2;
            case "低" -> 1;
            default -> 0;
        };
    }

    private String truncate(String text, int maxLen) {
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class RiskItem {
        private final String level;
        private final String category;
        private final String description;
        private final String legalBasis;
        private final String suggestion;
    }
}
