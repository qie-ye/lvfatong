package com.lvatong.lft.agent;

import com.lvatong.lft.ai.ChatService;
import com.lvatong.lft.ai.ToolExecutor;
import com.lvatong.lft.ai.ToolRegistry;
import com.lvatong.lft.ai.ZhipuApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ReAct (Reasoning + Acting) Agent 推理框架
 *
 * 推理链：Thought → Action → Observation → Thought → ... → Final Answer
 *
 * 优势：
 * 1. 显式推理过程，可解释性强
 * 2. 自主决定何时调用工具
 * 3. 根据观察结果动态调整策略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReActAgent {

    private final ChatService chatService;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final ZhipuApiClient zhipuApiClient;

    private static final int MAX_ITERATIONS = 8;
    private static final String REACT_SYSTEM_PROMPT = """
            你是一个专业的法律AI助手，采用ReAct（Reasoning + Acting）推理模式。

            ## 推理模式
            对于每个问题，请按以下步骤思考和行动：

            1. **Thought**：分析问题，思考需要什么信息
            2. **Action**：如果需要外部信息，选择一个工具调用
            3. **Observation**：观察工具返回的结果
            4. **Thought**：基于观察结果，继续思考
            5. ... 重复直到得出结论
            6. **Final Answer**：给出最终答案

            ## 可用工具
            %s

            ## 输出格式
            严格按以下格式输出：

            Thought: <你的思考过程>
            Action: <工具名称>
            Action Input: <工具参数，JSON格式>

            或者当得出结论时：

            Thought: <最终思考>
            Final Answer: <最终答案>

            ## 注意事项
            1. 每次只能调用一个工具
            2. 必须等待Observation结果后再继续思考
            3. 如果不需要工具，可以直接给出Final Answer
            4. 法律回答必须引用具体法条
            5. 最终答案末尾添加免责声明
            """;

    /**
     * ReAct 推理执行
     */
    public ReActResult execute(String question, String context, List<Map<String, String>> history) {
        log.info("ReAct Agent started for question: {}", truncate(question, 50));

        List<ReActStep> steps = new ArrayList<>();
        String toolsDescription = buildToolsDescription();

        // 构建初始消息
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                String.format(REACT_SYSTEM_PROMPT, toolsDescription)));

        if (context != null && !context.isBlank()) {
            messages.add(Map.of("role", "system", "content",
                    "【检索到的法律知识】\n" + context));
        }

        if (history != null && !history.isEmpty()) {
            messages.addAll(history);
        }

        messages.add(Map.of("role", "user", "content", question));

        String finalAnswer = null;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            log.debug("ReAct iteration {}/{}", i + 1, MAX_ITERATIONS);

            try {
                // 调用 LLM 进行推理
                String response = callLLM(messages);
                log.debug("ReAct response: {}", truncate(response, 200));

                // 解析响应
                ReActStep step = parseResponse(response);
                steps.add(step);

                if (step.isFinal()) {
                    finalAnswer = step.getFinalAnswer();
                    log.info("ReAct completed in {} iterations", i + 1);
                    break;
                }

                // 执行工具调用
                if (step.getAction() != null && !step.getAction().isBlank()) {
                    String toolResult = executeTool(step.getAction(), step.getActionInput());
                    step.setObservation(toolResult);

                    // 将结果添加到消息历史
                    messages.add(Map.of("role", "assistant", "content", response));
                    messages.add(Map.of("role", "user", "content",
                            "Observation: " + toolResult));

                    log.debug("Tool {} executed, result length: {}",
                            step.getAction(), toolResult.length());
                }

            } catch (Exception e) {
                log.error("ReAct iteration {} failed: {}", i + 1, e.getMessage());
                steps.add(ReActStep.builder()
                        .thought("推理过程出错: " + e.getMessage())
                        .isFinal(true)
                        .finalAnswer("抱歉，处理您的问题时出现了错误，请稍后重试。")
                        .build());
                break;
            }
        }

        if (finalAnswer == null) {
            finalAnswer = "抱歉，经过多次尝试仍未能得出结论，请尝试简化您的问题。";
        }

        return ReActResult.builder()
                .question(question)
                .steps(steps)
                .finalAnswer(finalAnswer)
                .iterations(steps.size())
                .build();
    }

    /**
     * 调用 LLM
     */
    private String callLLM(List<Map<String, String>> messages) {
        return zhipuApiClient.chat("glm-4-plus", messages, 0.3, 2048);
    }

    /**
     * 解析 LLM 响应
     */
    private ReActStep parseResponse(String response) {
        ReActStep.ReActStepBuilder builder = ReActStep.builder();
        builder.rawResponse(response);

        // 提取 Thought
        if (response.contains("Thought:")) {
            String thought = extractAfter(response, "Thought:");
            builder.thought(thought.split("Action:|Final Answer:")[0].trim());
        }

        // 检查是否有 Final Answer
        if (response.contains("Final Answer:")) {
            String finalAnswer = extractAfter(response, "Final Answer:");
            builder.isFinal(true);
            builder.finalAnswer(finalAnswer.trim());
            return builder.build();
        }

        // 提取 Action
        if (response.contains("Action:")) {
            String action = extractAfter(response, "Action:");
            builder.action(action.split("\n")[0].trim());
        }

        // 提取 Action Input
        if (response.contains("Action Input:")) {
            String actionInput = extractAfter(response, "Action Input:");
            builder.actionInput(actionInput.split("\n")[0].trim());
        }

        return builder.build();
    }

    /**
     * 执行工具调用
     */
    private String executeTool(String toolName, String toolInput) {
        try {
            return toolExecutor.execute(toolName, toolInput);
        } catch (Exception e) {
            log.warn("Tool {} execution failed: {}", toolName, e.getMessage());
            return "工具执行失败: " + e.getMessage();
        }
    }

    /**
     * 构建工具描述
     */
    private String buildToolsDescription() {
        StringBuilder sb = new StringBuilder();
        List<Map<String, Object>> tools = toolRegistry.getAllTools();

        for (Map<String, Object> tool : tools) {
            Map<String, Object> function = (Map<String, Object>) tool.get("function");
            sb.append("- ").append(function.get("name")).append(": ")
                    .append(function.get("description")).append("\n");
        }

        return sb.toString();
    }

    /**
     * 提取指定标记后的内容
     */
    private String extractAfter(String text, String marker) {
        int idx = text.indexOf(marker);
        if (idx < 0) return "";
        return text.substring(idx + marker.length());
    }

    private String truncate(String text, int maxLen) {
        return text != null && text.length() > maxLen
                ? text.substring(0, maxLen) + "..."
                : text;
    }

    // ==================== 数据模型 ====================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReActStep {
        private String thought;
        private String action;
        private String actionInput;
        private String observation;
        private String rawResponse;
        private boolean isFinal;
        private String finalAnswer;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReActResult {
        private String question;
        private List<ReActStep> steps;
        private String finalAnswer;
        private int iterations;
    }
}
