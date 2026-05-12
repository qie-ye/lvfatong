package com.lvatong.lft.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ZhipuApiClient zhipuApiClient;
    private final ModelRouterService modelRouterService;
    private final PromptTemplateService promptTemplateService;
    private final IntentClassifier intentClassifier;
    private final VerificationService verificationService;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;

    /**
     * 非流式法律问答
     */
    public String legalQa(String question, String context) {
        String model = modelRouterService.getModelForTask(ModelRouterService.TaskType.LEGAL_QA);
        ModelRouterService.ModelParams params = modelRouterService.getParamsForTask(ModelRouterService.TaskType.LEGAL_QA);
        String systemPrompt = promptTemplateService.buildLegalQaSystemPrompt(context);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", question)
        );
        try {
            String answer = zhipuApiClient.chat(model, messages, params.temperature(), params.maxTokens());
            return promptTemplateService.appendDisclaimer(answer);
        } catch (Exception e) {
            log.warn("Primary model {} failed, falling back: {}", model, e.getMessage());
            modelRouterService.markModelFailure(model);
            String fallback = modelRouterService.getFallbackModel(model);
            String answer = zhipuApiClient.chat(fallback, messages, params.temperature(), params.maxTokens());
            return promptTemplateService.appendDisclaimer(answer + "\n\n[注：当前使用降级模型，回答质量可能降低]");
        }
    }

    /**
     * 非流式法律问答（带对话历史 + 意图识别）
     */
    public String legalQa(String question, String context, List<Map<String, String>> history) {
        return legalQa(question, context, history, (String) null);
    }

    /**
     * 非流式法律问答（带对话历史 + 意图识别 + 记忆上下文）
     */
    public String legalQa(String question, String context, List<Map<String, String>> history, String memoryContext) {
        PromptTemplateService.IntentType intent = intentClassifier.classify(question);
        String model = modelRouterService.getModelForTask(ModelRouterService.TaskType.LEGAL_QA);
        ModelRouterService.ModelParams params = modelRouterService.getParamsForTask(ModelRouterService.TaskType.LEGAL_QA);
        String systemPrompt = promptTemplateService.buildSystemPromptByIntent(intent, context, memoryContext);
        java.util.ArrayList<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(history);
        messages.add(Map.of("role", "user", "content", question));
        try {
            String answer = zhipuApiClient.chat(model, messages, params.temperature(), params.maxTokens());
            return promptTemplateService.appendDisclaimer(answer);
        } catch (Exception e) {
            log.warn("Primary model {} failed, falling back: {}", model, e.getMessage());
            modelRouterService.markModelFailure(model);
            String fallback = modelRouterService.getFallbackModel(model);
            String answer = zhipuApiClient.chat(fallback, messages, params.temperature(), params.maxTokens());
            return promptTemplateService.appendDisclaimer(answer + "\n\n[注：当前使用降级模型，回答质量可能降低]");
        }
    }

    /**
     * SSE流式法律问答（带意图识别）
     */
    public void legalQaStream(String question, String context, List<Map<String, String>> history, ZhipuApiClient.SseEventHandler handler) {
        legalQaStream(question, context, history, (String) null, handler);
    }

    /**
     * SSE流式法律问答（带意图识别 + 记忆上下文）
     */
    public void legalQaStream(String question, String context, List<Map<String, String>> history, String memoryContext, ZhipuApiClient.SseEventHandler handler) {
        PromptTemplateService.IntentType intent = intentClassifier.classify(question);
        String model = modelRouterService.getModelForTask(ModelRouterService.TaskType.LEGAL_QA);
        ModelRouterService.ModelParams params = modelRouterService.getParamsForTask(ModelRouterService.TaskType.LEGAL_QA);
        String systemPrompt = promptTemplateService.buildSystemPromptByIntent(intent, context, memoryContext);
        java.util.ArrayList<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (history != null) messages.addAll(history);
        messages.add(Map.of("role", "user", "content", question));
        try {
            zhipuApiClient.chatStream(model, messages, params.temperature(), params.maxTokens(), new ZhipuApiClient.SseEventHandler() {
                @Override
                public void onContent(String content) {
                    handler.onContent(content);
                }
                @Override
                public void onComplete() {
                    handler.onContent(promptTemplateService.appendDisclaimer(""));
                    handler.onComplete();
                }
            });
        } catch (Exception e) {
            log.warn("Stream model {} failed, falling back: {}", model, e.getMessage());
            modelRouterService.markModelFailure(model);
            String fallback = modelRouterService.getFallbackModel(model);
            zhipuApiClient.chatStream(fallback, messages, params.temperature(), params.maxTokens(), new ZhipuApiClient.SseEventHandler() {
                @Override
                public void onContent(String content) {
                    handler.onContent(content);
                }
                @Override
                public void onComplete() {
                    handler.onContent("\n\n[注：当前使用降级模型，回答质量可能降低]");
                    handler.onContent(promptTemplateService.appendDisclaimer(""));
                    handler.onComplete();
                }
            });
        }
    }

    /**
     * 非流式法律问答（带显式意图，含验证+重试，跳过二次分类）
     */
    public String legalQa(String question, String context, List<Map<String, String>> history,
                          PromptTemplateService.IntentType intent) {
        return legalQa(question, context, history, intent, null);
    }

    public String legalQa(String question, String context, List<Map<String, String>> history,
                          PromptTemplateService.IntentType intent, String memoryContext) {
        String answer = legalQaRaw(question, context, history, intent, memoryContext);

        if (intent != PromptTemplateService.IntentType.CHAT) {
            VerificationService.VerificationResult vr =
                    verificationService.verifyLegalAnswer(question, answer, context);
            if (!vr.passed() && !vr.issues().isEmpty()) {
                log.info("Verification failed (score={}), retrying with issue hints", String.format("%.2f", vr.score()));
                String fixContext = context + buildIssueHint(vr);
                answer = legalQaRaw(question, fixContext, history, intent, memoryContext);
            }
        }

        return answer;
    }

    private String legalQaRaw(String question, String context, List<Map<String, String>> history,
                               PromptTemplateService.IntentType intent, String memoryContext) {
        String model = modelRouterService.getModelForTask(ModelRouterService.TaskType.LEGAL_QA);
        ModelRouterService.ModelParams params = modelRouterService.getParamsForTask(ModelRouterService.TaskType.LEGAL_QA);
        String systemPrompt = promptTemplateService.buildSystemPromptByIntent(intent, context, memoryContext);
        java.util.ArrayList<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (history != null) messages.addAll(history);
        messages.add(Map.of("role", "user", "content", question));
        try {
            String answer = zhipuApiClient.chat(model, messages, params.temperature(), params.maxTokens());
            return promptTemplateService.appendDisclaimer(answer);
        } catch (Exception e) {
            log.warn("Primary model {} failed, falling back: {}", model, e.getMessage());
            modelRouterService.markModelFailure(model);
            String fallback = modelRouterService.getFallbackModel(model);
            String answer = zhipuApiClient.chat(fallback, messages, params.temperature(), params.maxTokens());
            return promptTemplateService.appendDisclaimer(answer + "\n\n[注：当前使用降级模型，回答质量可能降低]");
        }
    }

    /**
     * 复杂法律问答（GLM-4-Plus 深度推理，含验证+重试）
     */
    public String complexLegalQa(String question, String context, List<Map<String, String>> history) {
        return complexLegalQa(question, context, history, null);
    }

    public String complexLegalQa(String question, String context, List<Map<String, String>> history, String memoryContext) {
        String answer = complexLegalQaRaw(question, context, history, memoryContext);

        VerificationService.VerificationResult vr =
                verificationService.verifyLegalAnswer(question, answer, context);
        if (!vr.passed() && !vr.issues().isEmpty()) {
            log.info("Complex answer verification failed (score={}), retrying", String.format("%.2f", vr.score()));
            answer = complexLegalQaRaw(question, context + buildIssueHint(vr), history, memoryContext);
        }

        return answer;
    }

    private String complexLegalQaRaw(String question, String context, List<Map<String, String>> history, String memoryContext) {
        String model = modelRouterService.getModelForTask(ModelRouterService.TaskType.DEEP_REASONING);
        ModelRouterService.ModelParams params = modelRouterService.getParamsForTask(ModelRouterService.TaskType.DEEP_REASONING);
        String systemPrompt = promptTemplateService.buildSystemPromptByIntent(
                PromptTemplateService.IntentType.COMPLEX_LEGAL, context, memoryContext);
        java.util.ArrayList<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (history != null) messages.addAll(history);
        messages.add(Map.of("role", "user", "content", question));
        try {
            String answer = zhipuApiClient.chat(model, messages, params.temperature(), params.maxTokens());
            return promptTemplateService.appendDisclaimer(answer);
        } catch (Exception e) {
            log.warn("Complex model {} failed, falling back to legalQa: {}", model, e.getMessage());
            return legalQaRaw(question, context, history, PromptTemplateService.IntentType.LEGAL_QA, memoryContext);
        }
    }

    private String buildIssueHint(VerificationService.VerificationResult vr) {
        StringBuilder sb = new StringBuilder("\n\n[验证发现以下问题需修正]\n");
        vr.issues().forEach(issue -> sb.append("- ").append(issue).append("\n"));
        if (!vr.suggestedFix().isBlank()) {
            sb.append("修正方向：").append(vr.suggestedFix());
        }
        return sb.toString();
    }

    /**
     * 支持 Function Calling 的法律问答（LLM 自主决定调用哪些工具）
     * 多轮循环：LLM → tool_calls → 执行工具 → 结果回传 → LLM → ... → 最终回答
     * 最大迭代 5 轮；失败时降级为普通 legalQaRaw
     */
    public String legalQaWithTools(String question, String context,
                                   List<Map<String, String>> history) {
        return legalQaWithTools(question, context, history, null);
    }

    public String legalQaWithTools(String question, String context,
                                   List<Map<String, String>> history, String memoryContext) {
        try {
            String model = modelRouterService.getModelForTask(ModelRouterService.TaskType.LEGAL_QA);
            ModelRouterService.ModelParams params = modelRouterService.getParamsForTask(ModelRouterService.TaskType.LEGAL_QA);
            String systemPrompt = promptTemplateService.buildSystemPromptByIntent(
                    PromptTemplateService.IntentType.COMPLEX_LEGAL, context, memoryContext);

            java.util.ArrayList<Map<String, Object>> messages = new java.util.ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            if (history != null) {
                history.forEach(h -> messages.add(Map.of("role", h.get("role"), "content", h.get("content"))));
            }
            messages.add(Map.of("role", "user", "content", question));

            java.util.List<Map<String, Object>> tools = toolRegistry.getAllTools();

            int maxIterations = 5;
            for (int i = 0; i < maxIterations; i++) {
                ZhipuApiClient.ChatWithToolsResult result =
                        zhipuApiClient.chatWithTools(model, messages, tools, params.temperature(), params.maxTokens());

                if (!result.hasToolCalls()) {
                    log.info("Function calling completed in {} round(s)", i + 1);
                    String content = result.content() != null ? result.content() : "";
                    return promptTemplateService.appendDisclaimer(content);
                }

                // Add assistant message containing tool_calls
                messages.add(Map.of("role", "assistant", "tool_calls", result.toolCalls()));

                // Execute each tool and append result
                for (ZhipuApiClient.ToolCall tc : result.toolCalls()) {
                    String toolResult = toolExecutor.execute(tc.name(), tc.arguments());
                    log.debug("Tool {} result length: {}", tc.name(), toolResult.length());
                    messages.add(Map.of(
                            "role", "tool",
                            "tool_call_id", tc.id(),
                            "content", toolResult
                    ));
                }
            }

            log.warn("Function calling max iterations ({}) reached, falling back", maxIterations);
        } catch (Exception e) {
            log.warn("legalQaWithTools failed, falling back to legalQaRaw: {}", e.getMessage());
        }

        return legalQaRaw(question, context, history, PromptTemplateService.IntentType.COMPLEX_LEGAL, memoryContext);
    }

    /**
     * 复杂法律问答 SSE 流式（使用 GLM-4-Plus）
     */
    public void complexLegalQaStream(String question, String context, List<Map<String, String>> history,
                                     ZhipuApiClient.SseEventHandler handler) {
        complexLegalQaStream(question, context, history, null, handler);
    }

    public void complexLegalQaStream(String question, String context, List<Map<String, String>> history,
                                     String memoryContext, ZhipuApiClient.SseEventHandler handler) {
        String model = modelRouterService.getModelForTask(ModelRouterService.TaskType.DEEP_REASONING);
        ModelRouterService.ModelParams params = modelRouterService.getParamsForTask(ModelRouterService.TaskType.DEEP_REASONING);
        String systemPrompt = promptTemplateService.buildSystemPromptByIntent(
                PromptTemplateService.IntentType.COMPLEX_LEGAL, context, memoryContext);
        java.util.ArrayList<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (history != null) messages.addAll(history);
        messages.add(Map.of("role", "user", "content", question));
        try {
            zhipuApiClient.chatStream(model, messages, params.temperature(), params.maxTokens(), new ZhipuApiClient.SseEventHandler() {
                @Override
                public void onContent(String content) {
                    handler.onContent(content);
                }
                @Override
                public void onComplete() {
                    handler.onContent(promptTemplateService.appendDisclaimer(""));
                    handler.onComplete();
                }
            });
        } catch (Exception e) {
            log.warn("Complex stream {} failed, falling back: {}", model, e.getMessage());
            legalQaStream(question, context, history, handler);
        }
    }

    /**
     * SSE 流式法律问答（带显式意图，跳过二次分类）
     */
    public void legalQaStream(String question, String context, List<Map<String, String>> history,
                               PromptTemplateService.IntentType intent, ZhipuApiClient.SseEventHandler handler) {
        legalQaStream(question, context, history, intent, null, handler);
    }

    public void legalQaStream(String question, String context, List<Map<String, String>> history,
                               PromptTemplateService.IntentType intent, String memoryContext, ZhipuApiClient.SseEventHandler handler) {
        String model = modelRouterService.getModelForTask(ModelRouterService.TaskType.LEGAL_QA);
        ModelRouterService.ModelParams params = modelRouterService.getParamsForTask(ModelRouterService.TaskType.LEGAL_QA);
        String systemPrompt = promptTemplateService.buildSystemPromptByIntent(intent, context, memoryContext);
        java.util.ArrayList<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (history != null) messages.addAll(history);
        messages.add(Map.of("role", "user", "content", question));
        try {
            zhipuApiClient.chatStream(model, messages, params.temperature(), params.maxTokens(), new ZhipuApiClient.SseEventHandler() {
                @Override
                public void onContent(String content) {
                    handler.onContent(content);
                }
                @Override
                public void onComplete() {
                    handler.onContent(promptTemplateService.appendDisclaimer(""));
                    handler.onComplete();
                }
            });
        } catch (Exception e) {
            log.warn("Stream model {} failed, falling back: {}", model, e.getMessage());
            modelRouterService.markModelFailure(model);
            String fallback = modelRouterService.getFallbackModel(model);
            zhipuApiClient.chatStream(fallback, messages, params.temperature(), params.maxTokens(), new ZhipuApiClient.SseEventHandler() {
                @Override
                public void onContent(String content) {
                    handler.onContent(content);
                }
                @Override
                public void onComplete() {
                    handler.onContent("\n\n[注：当前使用降级模型，回答质量可能降低]");
                    handler.onContent(promptTemplateService.appendDisclaimer(""));
                    handler.onComplete();
                }
            });
        }
    }

    /**
     * 轻量级单轮对话，用于内部辅助调用（查询改写、补充查询等）
     */
    public String simpleChat(String prompt, String model, double temperature, int maxTokens) {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", prompt)
        );
        return zhipuApiClient.chat(model, messages, temperature, maxTokens);
    }

    /**
     * 合同分析（非流式，使用GLM-4-Long）
     */
    public String contractAnalysis(String contractText, String context) {
        String model = modelRouterService.getModelForTask(ModelRouterService.TaskType.CONTRACT_ANALYSIS);
        ModelRouterService.ModelParams params = modelRouterService.getParamsForTask(ModelRouterService.TaskType.CONTRACT_ANALYSIS);
        String systemPrompt = promptTemplateService.buildContractAnalysisSystemPrompt(context);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", contractText)
        );
        try {
            String answer = zhipuApiClient.chat(model, messages, params.temperature(), params.maxTokens());
            return promptTemplateService.appendDisclaimer(answer);
        } catch (Exception e) {
            log.warn("Contract model {} failed, falling back: {}", model, e.getMessage());
            modelRouterService.markModelFailure(model);
            String fallback = modelRouterService.getFallbackModel(model);
            String answer = zhipuApiClient.chat(fallback, messages, params.temperature(), params.maxTokens());
            return promptTemplateService.appendDisclaimer(answer + "\n\n[注：当前使用降级模型，长文本分析能力受限]");
        }
    }
}
