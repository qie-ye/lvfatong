package com.lvatong.lft.ai;

import org.springframework.stereotype.Component;

/**
 * 各 Agent 提示词定义（Supervisor 模式）
 * 法律意见生成：Researcher → Analyst → Critic → Writer
 * 复杂合同分析：ContractResearcher → ContractAnalyst → ContractCritic → ContractWriter
 */
@Component
public class AgentPrompt {

    /* ============================================================
     * 法律意见生成 Agents
     * ============================================================ */

    /**
     * Researcher Agent：从 RAG 上下文提取结构化法律知识
     */
    public String buildResearcherPrompt(String question, String facts, String domain, String ragContext) {
        String domainStr = domain != null && !domain.isBlank() ? "【法律领域】" + domain + "\n" : "";
        String factsStr = facts != null && !facts.isBlank() ? "【案件事实摘要】\n" + facts + "\n\n" : "";
        String ctx = ragContext != null && !ragContext.isBlank()
                ? ragContext.length() > 3000 ? ragContext.substring(0, 3000) + "...(已截断)" : ragContext
                : "（无检索结果）";
        return """
                你是法律研究助手，负责从已检索的法律知识中提炼对本案有用的信息。

                %s%s【用户问题】
                %s

                【已检索法律知识】
                %s

                请输出结构化研究摘要，包含：
                1. **适用法律**：本案可能涉及的法律法规名称及条款
                2. **关键法律概念**：涉及的核心法律术语和定义
                3. **重要法律规定**：与本案直接相关的具体条文内容
                4. **类案参考**：类似情形的法律处理原则（如有）

                只输出研究摘要，简洁精准，不要给出最终结论。
                """.formatted(domainStr, factsStr, question, ctx);
    }

    /**
     * Analyst Agent：基于研究成果做深度法律分析
     */
    public String buildAnalystPrompt(String question, String facts, String domain, String research) {
        String domainStr = domain != null && !domain.isBlank() ? "法律领域：" + domain + "\n" : "";
        String factsStr = facts != null && !facts.isBlank() ? "案件事实：\n" + facts + "\n\n" : "";
        String researchStr = research != null && !research.isBlank() ? research : "（研究阶段未产出结果，请基于通用法律知识分析）";
        return """
                你是资深法律分析师，请基于研究摘要对以下法律问题进行深度分析。

                %s%s【用户问题】
                %s

                【研究摘要】
                %s

                请输出详细法律分析，必须覆盖：
                1. **法律关系梳理**：涉及哪些法律主体及其权利义务关系
                2. **法律适用论证**：说明适用哪些法律条文及理由
                3. **争议焦点**：列举可能存在的争议点及各方主张
                4. **法律风险**：识别对用户不利的法律风险

                分析要严谨，每项结论需有法律依据支撑。
                """.formatted(domainStr, factsStr, question, researchStr);
    }

    /**
     * Analyst Revise Agent：根据 Critic 审查意见修正原始分析
     */
    public String buildAnalystRevisePrompt(String question, String originalAnalysis, String critique) {
        String analysisStr = originalAnalysis != null && !originalAnalysis.isBlank()
                ? originalAnalysis : "（原始分析为空）";
        String critiqueStr = critique != null && !critique.isBlank()
                ? critique : "（审查意见为空）";
        return """
                你是资深法律分析师，请根据审查专家的意见，对原始法律分析进行修正和完善。

                【原始问题】
                %s

                【原始分析】
                %s

                【审查专家意见】
                %s

                请输出修正后的法律分析，要求：
                1. 针对审查意见中指出的法条引用错误进行更正
                2. 补充被遗漏的重要法律视角
                3. 修正逻辑不一致之处
                4. 保留原分析中正确的部分，只修改有问题的内容

                直接输出修正后的完整分析，不要说明修改了哪些内容。
                """.formatted(question,
                analysisStr.length() > 2500 ? analysisStr.substring(0, 2500) + "..." : analysisStr,
                critiqueStr.length() > 800 ? critiqueStr.substring(0, 800) + "..." : critiqueStr);
    }

    /**
     * Critic Agent：审查分析的准确性与完整性
     */
    public String buildCriticPrompt(String question, String analysis) {
        String analysisStr = analysis != null && !analysis.isBlank() ? analysis : "（分析阶段未产出结果）";
        return """
                你是法律审查专家，请对以下法律分析进行批判性审查。

                【原始问题】
                %s

                【待审查分析】
                %s

                请简洁指出（不超过300字）：
                1. **法条引用错误**：有无引用不存在或适用不当的法条
                2. **逻辑缺陷**：有无推理跳跃或自相矛盾之处
                3. **遗漏要点**：有无重要法律视角被忽略
                4. **改进建议**：针对上述问题的具体修正方向

                如果分析准确完整，直接输出：「分析准确，无需修正」
                """.formatted(question, analysisStr.length() > 3000 ? analysisStr.substring(0, 3000) + "..." : analysisStr);
    }

    /**
     * Writer Agent：综合所有输出，生成正式法律意见书
     */
    public String buildWriterPrompt(String question, String facts, String domain,
                                    String analysis, String critique) {
        String domainStr = domain != null && !domain.isBlank() ? "法律领域：" + domain + "\n" : "";
        String factsStr = facts != null && !facts.isBlank() ? "案件事实：\n" + facts + "\n\n" : "";
        String critiqueNote = (critique != null && !critique.isBlank()
                && !critique.contains("无需修正"))
                ? "\n\n【审查意见（需纳入）】\n" + critique : "";
        return """
                你是资深法律顾问，请综合分析报告撰写正式法律意见书。

                %s%s【用户问题】
                %s

                【综合分析】
                %s%s

                请严格按以下格式输出法律意见书：

                【法律分析】
                对案件事实进行详细的法律分析，包括法律关系梳理、权利义务分析、法律适用论证、争议焦点归纳。

                【结论】
                基于上述分析，给出明确的法律结论。

                【法律依据】
                列出适用的法律法规、司法解释，格式：《法律名称》第X条。

                【建议】
                1. 维权路径
                2. 证据收集建议
                3. 时效提醒
                4. 风险提示

                注意：引用具体法条需标注名称和条款号，不得给出确定性胜诉承诺。
                """.formatted(domainStr, factsStr, question,
                analysis != null ? analysis : "（待补充）", critiqueNote);
    }

    /* ============================================================
     * 复杂合同分析 Agents
     * ============================================================ */

    /**
     * Contract Researcher Agent：提炼合同相关法律知识
     */
    public String buildContractResearcherPrompt(String contractText, String ragContext) {
        String truncated = contractText.length() > 2000
                ? contractText.substring(0, 2000) + "...(已截断)" : contractText;
        String ctx = ragContext != null && !ragContext.isBlank()
                ? ragContext.length() > 2000 ? ragContext.substring(0, 2000) + "..." : ragContext
                : "（无检索结果）";
        return """
                你是合同法律研究助手，请从检索知识中提炼与以下合同分析相关的法律依据。

                【合同内容摘要】
                %s

                【检索到的法律知识】
                %s

                请输出简洁的研究摘要（不超过500字），包含：
                1. 适用的合同法律条文（合同法/民法典相关条款）
                2. 格式条款和霸王条款的认定标准
                3. 违约责任相关规定
                4. 本合同类型的常见法律风险点

                只输出摘要，不做合同分析。
                """.formatted(truncated, ctx);
    }

    /**
     * Contract Analyst Agent：深度合同风险分析
     */
    public String buildContractAnalystPrompt(String contractText, String research) {
        String truncated = contractText.length() > 5000
                ? contractText.substring(0, 5000) + "...(已截断)" : contractText;
        String researchStr = research != null && !research.isBlank()
                ? research : "（研究阶段未产出，请基于通用合同法知识分析）";
        return """
                你是合同风险分析师，请对以下合同进行深度风险分析。

                【法律研究参考】
                %s

                【合同全文】
                %s

                请从以下维度进行分析：
                1. **高风险条款**：违约金过高/无限责任/单方解除权等不平等条款
                2. **缺失条款**：重要条款（争议解决/不可抗力/保密期限）是否缺失
                3. **模糊表述**：可能引发争议的模糊或歧义表达
                4. **法律适用问题**：与现行法律不符的条款
                5. **综合风险等级**：低/中/高，并说明理由

                输出详细分析，每个风险点需说明条款位置和具体影响。
                """.formatted(researchStr, truncated);
    }

    /**
     * Contract Critic Agent：审查合同分析的完整性
     */
    public String buildContractCriticPrompt(String contractText, String analysis) {
        String truncated = contractText.length() > 1500
                ? contractText.substring(0, 1500) + "..." : contractText;
        String analysisStr = analysis != null && !analysis.isBlank()
                ? (analysis.length() > 2000 ? analysis.substring(0, 2000) + "..." : analysis)
                : "（分析阶段未产出）";
        return """
                你是合同审查专家，请检查以下合同风险分析是否有遗漏或错误。

                【合同摘要】
                %s

                【已有分析】
                %s

                请简洁指出（不超过300字）：
                1. 有无被遗漏的重要风险条款
                2. 风险等级判断是否准确
                3. 是否有法律适用错误

                如分析完整准确，输出：「分析完整，无需补充」
                """.formatted(truncated, analysisStr);
    }

    /**
     * Contract Writer Agent：综合输出最终分析报告（JSON友好格式）
     */
    public String buildContractWriterPrompt(String contractText, String analysis, String critique) {
        String critiqueNote = (critique != null && !critique.isBlank()
                && !critique.contains("无需补充"))
                ? "\n\n【审查补充意见】\n" + critique : "";
        String analysisStr = analysis != null ? analysis : "（待补充）";
        return """
                你是合同法律顾问，请综合以下分析和审查意见，输出最终合同分析摘要。

                【综合分析】
                %s%s

                请输出一段综合摘要（300-600字），内容包含：
                1. 合同整体风险评估（低/中/高）及主要依据
                2. 最需关注的3-5个风险点
                3. 优先建议的修改方向
                4. 签约前的注意事项

                输出纯文本摘要，不要JSON格式。
                """.formatted(analysisStr.length() > 4000 ? analysisStr.substring(0, 4000) + "..." : analysisStr,
                critiqueNote);
    }
}
