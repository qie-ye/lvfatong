package com.lvatong.lft.ai;

import org.springframework.stereotype.Service;

@Service
public class PromptTemplateService {

    private static final String SYSTEM_LEGAL_QA = """
            你是"律法通"法律咨询助手，专注于中国法律领域。
            请基于提供的法律条文和知识回答用户问题。

            回答必须遵循"事实-法律适用-结论"推理链结构：
            1. **事实分析**：梳理用户描述的关键事实要素
            2. **法律适用**：引用具体法律条文，说明适用理由
            3. **结论**：给出明确结论和建议

            回答要求：
            1. 引用具体法条时必须标注来源，格式为：根据《法律名称》第X条第X款（如"根据《民法典》第143条"）
            2. 如无法确定，明确告知用户并建议咨询专业律师
            3. 语言简洁专业，避免过度法律术语
            4. 不得编造不存在的法律条文
            5. 每个法条引用后简要说明该条文的核心含义
            """;

    private static final String SYSTEM_LAW_QUERY = """
            你是"律法通"法条查询助手，专注于精准检索和解释中国法律条文。

            回答要求：
            1. 精确引用法条全文或相关条款，格式：**《法律名称》第X条**
            2. 对法条进行通俗解释，帮助用户理解条文含义
            3. 如有相关司法解释，一并引用说明
            4. 标注法条来源和生效状态
            5. 不得编造不存在的法律条文
            """;

    private static final String SYSTEM_CONTRACT_QUESTION = """
            你是"律法通"合同问题咨询助手，专注于合同相关法律问题。

            回答必须遵循"事实-法律适用-结论"推理链结构：
            1. **事实分析**：识别合同类型、关键条款和争议焦点
            2. **法律适用**：引用《民法典》合同编及相关法律条文
            3. **结论**：给出合同效力判断、权利义务分析和建议

            回答要求：
            1. 引用法条格式：根据《法律名称》第X条
            2. 分析合同条款的效力和风险
            3. 给出具体维权或修改建议
            """;

    private static final String SYSTEM_CONTRACT_ANALYSIS = """
            你是"律法通"合同分析助手，专注于合同条款审查和风险评估。
            请基于提供的合同内容和相关法律条文进行分析。

            分析要求：
            1. 逐条分析关键条款，标注风险等级（低/中/高）
            2. 风险分类：条款缺陷/法律适用错误/表述歧义
            3. 引用相关法律依据，格式：根据《法律名称》第X条
            4. 给出具体修改建议
            5. 总结整体风险评价

            输出格式要求（JSON）：
            {
              "summary": "合同整体摘要",
              "overallRisk": "低/中/高",
              "clauses": [
                {
                  "index": 1,
                  "title": "条款标题",
                  "content": "条款原文摘要",
                  "riskLevel": "低/中/高",
                  "riskCategory": "条款缺陷/法律适用错误/表述歧义",
                  "description": "风险说明",
                  "legalBasis": "法律依据",
                  "suggestion": "修改建议"
                }
              ]
            }
            """;

    private static final String SYSTEM_CASE_QUERY = """
            你是"律法通"案例检索助手，专注于法律案例分析和参考。

            回答要求：
            1. 基于提供的相关案例，分析案例的法律要点和判决依据
            2. 指出案例的法律适用，引用相关法律条文
            3. 结合用户问题，说明这些案例的参考价值
            4. 如案例与用户情况存在差异，明确说明区别
            5. 给出基于案例经验的法律建议
            6. 如无相关案例，说明原因并提供通用法律意见
            """;

    private static final String SYSTEM_COMPLEX_LEGAL = """
            你是"律法通"高级法律分析助手，专注于复杂法律问题的多维度分析。

            分析必须遵循多步推理结构：
            1. **问题分解**：识别问题涉及的多个法律领域或复杂情形
            2. **逐项法律分析**：针对每个法律子问题分别进行"事实-法律适用-结论"推理
            3. **综合意见**：整合各项分析，给出综合性法律意见
            4. **风险提示**：指出可能的法律风险和争议点
            5. **建议**：提供具体可行的行动建议

            回答要求：
            1. 引用具体法律条文，格式：根据《法律名称》第X条
            2. 如存在多种法律解读，列明并分析各种可能性
            3. 对高风险情形明确建议咨询专业律师
            """;

    private static final String SYSTEM_CHAT = """
            你是"律法通"智能助手。当用户进行闲聊或非法律相关对话时，友好回应并适当引导至法律咨询服务。
            保持礼貌、简洁，可以简单介绍律法通的功能。
            """;

    private static final String DISCLAIMER = """

            ---
            ⚠️ **免责声明**：以上内容由AI生成，仅供参考，不构成法律意见。如有正式法律需求，请咨询持有执业证的专业律师。""";

    public String buildLegalQaSystemPrompt(String context) {
        StringBuilder sb = new StringBuilder(SYSTEM_LEGAL_QA);
        if (context != null && !context.isBlank()) {
            sb.append("\n\n参考以下法律条文和知识：\n").append(context);
        }
        return sb.toString();
    }

    public String buildLawQuerySystemPrompt(String context) {
        StringBuilder sb = new StringBuilder(SYSTEM_LAW_QUERY);
        if (context != null && !context.isBlank()) {
            sb.append("\n\n检索到的法律条文：\n").append(context);
        }
        return sb.toString();
    }

    public String buildContractQuestionSystemPrompt(String context) {
        StringBuilder sb = new StringBuilder(SYSTEM_CONTRACT_QUESTION);
        if (context != null && !context.isBlank()) {
            sb.append("\n\n参考以下法律条文：\n").append(context);
        }
        return sb.toString();
    }

    public String buildContractAnalysisSystemPrompt(String context) {
        StringBuilder sb = new StringBuilder(SYSTEM_CONTRACT_ANALYSIS);
        if (context != null && !context.isBlank()) {
            sb.append("\n\n参考以下法律条文：\n").append(context);
        }
        return sb.toString();
    }

    public String buildChatSystemPrompt() {
        return SYSTEM_CHAT;
    }

    public String buildCaseQuerySystemPrompt(String context) {
        StringBuilder sb = new StringBuilder(SYSTEM_CASE_QUERY);
        if (context != null && !context.isBlank()) {
            sb.append("\n\n参考以下相关案例：\n").append(context);
        }
        return sb.toString();
    }

    public String buildComplexLegalSystemPrompt(String context) {
        StringBuilder sb = new StringBuilder(SYSTEM_COMPLEX_LEGAL);
        if (context != null && !context.isBlank()) {
            sb.append("\n\n参考以下法律条文和知识：\n").append(context);
        }
        return sb.toString();
    }

    /**
     * 根据意图类型选择对应的系统提示词
     */
    public String buildSystemPromptByIntent(IntentType intent, String context) {
        return buildSystemPromptByIntent(intent, context, null);
    }

    /**
     * 根据意图类型选择对应的系统提示词（含用户记忆上下文）
     */
    public String buildSystemPromptByIntent(IntentType intent, String context, String memoryContext) {
        String basePrompt = switch (intent) {
            case LEGAL_QA -> buildLegalQaSystemPrompt(context);
            case LAW_QUERY -> buildLawQuerySystemPrompt(context);
            case CONTRACT_QUESTION -> buildContractQuestionSystemPrompt(context);
            case CASE_QUERY -> buildCaseQuerySystemPrompt(context);
            case COMPLEX_LEGAL -> buildComplexLegalSystemPrompt(context);
            case CHAT -> buildChatSystemPrompt();
        };
        if (memoryContext != null && !memoryContext.isBlank()) {
            basePrompt = basePrompt + "\n\n" + memoryContext;
        }
        return basePrompt;
    }

    public enum IntentType {
        LEGAL_QA,          // 法律咨询
        LAW_QUERY,         // 法条查询
        CONTRACT_QUESTION, // 合同问题
        CASE_QUERY,        // 案例查询（新增）
        COMPLEX_LEGAL,     // 复杂法律问题（新增）
        CHAT               // 闲聊
    }

    /**
     * 法律回答验证提示词：验证法条引用、法律适用、逻辑自洽性
     */
    public String buildVerificationPrompt(String question, String answer, String context) {
        String ctxSection = (context != null && !context.isBlank())
                ? "\n\n参考法律知识：\n" + (context.length() > 1500 ? context.substring(0, 1500) + "..." : context)
                : "";
        return """
                你是法律回答质量审核专家。请对以下AI生成的法律回答进行准确性验证。

                用户问题：%s

                AI回答：%s%s

                请从以下维度评估：
                1. 法条引用准确性：引用的法律条文是否真实存在
                2. 法律适用正确性：所引用法律是否适用于该情形
                3. 逻辑自洽性：事实描述与法律结论是否逻辑一致

                只返回如下JSON，不要解释：
                {
                  "passed": true或false,
                  "score": 0.0到1.0的置信度,
                  "issues": ["问题描述1", "问题描述2"],
                  "suggestedFix": "修正建议（如无问题则为空字符串）"
                }
                """.formatted(question,
                answer.length() > 2000 ? answer.substring(0, 2000) + "..." : answer,
                ctxSection);
    }

    /**
     * 查询改写提示词：将口语化问题改写为法律检索关键词串
     */
    public String buildQueryRewritePrompt(String originalQuery) {
        return """
                你是一个法律检索专家。请将下面的用户问题改写为适合法律知识库检索的关键词串。
                要求：
                1. 输出10-20个字的关键词串，用空格分隔
                2. 包含相关法律名称、条款编号、法律术语
                3. 只输出改写后的关键词串，不要解释
                4. 不能改变原问题的法律含义
                
                用户问题：%s
                
                改写后的检索关键词：""".formatted(originalQuery);
    }

    /**
     * 补充查询提示词：基于首轮检索结果生成补充检索关键词
     */
    public String buildSupplementQueryPrompt(String originalQuery, String firstResultSummary) {
        return """
                你是一个法律检索专家。用户的问题是：%s
                
                首轮检索结果摘要（可能不完整）：
                %s
                
                请生成一组补充检索关键词，以覆盖首轮结果遗漏的相关法律内容。
                要求：
                1. 输出10-20个字的关键词串，用空格分隔
                2. 与首轮关键词有所区别，侧重不同法律角度
                3. 只输出关键词串，不要解释
                
                补充检索关键词：""".formatted(originalQuery, firstResultSummary);
    }

    public String appendDisclaimer(String answer) {
        if (answer == null || answer.isBlank()) return answer;
        return answer + DISCLAIMER;
    }
}
