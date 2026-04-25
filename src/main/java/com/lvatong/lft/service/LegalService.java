package com.lvatong.lft.service;

import com.lvatong.lft.ai.ChatService;
import com.lvatong.lft.ai.IntentClassifier;
import com.lvatong.lft.ai.PromptTemplateService;
import com.lvatong.lft.ai.ZhipuApiClient;
import com.lvatong.lft.knowledge.FaqService;
import com.lvatong.lft.model.entity.ChatMessage;
import com.lvatong.lft.model.entity.ChatSession;
import com.lvatong.lft.model.entity.FaqEntry;
import com.lvatong.lft.rag.HybridSearchService;
import com.lvatong.lft.rag.RAGService;
import com.lvatong.lft.repository.ChatMessageRepository;
import com.lvatong.lft.repository.ChatSessionRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegalService {

    private final ChatService chatService;
    private final RAGService ragService;
    private final HybridSearchService hybridSearchService;
    private final ChatMemoryService chatMemoryService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TransactionTemplate transactionTemplate;
    private final FaqService faqService;
    private final IntentClassifier intentClassifier;
    private final CaseService caseService;
    private final SessionSummaryService sessionSummaryService;
    private final UserMemoryService userMemoryService;

    private static final String FAQ_SUFFIX = "\n\n---\n*此回答来源于律法通常见问题库，仅供参考。如有复杂情况，建议咨询专业律师。*";
    private static final double CONFIDENCE_LOW_THRESHOLD = 0.6;
    private static final String LOW_CONFIDENCE_SUFFIX = "\n\n⚠️ **提示**：您的问题意图较为模糊，以上回答基于通用法律知识，建议进一步咨询专业律师确认。";

    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    @PreDestroy
    public void shutdown() {
        sseExecutor.shutdown();
        try {
            if (!sseExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                sseExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            sseExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 创建新对话会话
     */
    @Transactional
    public ChatSession createSession(Long userId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title != null ? title : "新对话");
        session.setType(ChatSession.SessionType.LEGAL_QA);
        return chatSessionRepository.save(session);
    }

    /**
     * 获取用户对话列表
     */
    public List<ChatSession> getSessions(Long userId) {
        return chatSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取对话消息历史
     */
    public List<ChatMessage> getMessages(Long sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /**
     * 非流式法律问答（FAQ优先匹配）
     */
    @Transactional
    public ChatMessage chat(Long userId, Long sessionId, String question, String docType, String lawDomain) {
        ChatSession session = ensureSession(userId, sessionId, question);

        // FAQ优先匹配：命中预置高频问题直接返回标准答案
        FaqEntry faq = faqService.matchFaq(question);
        String answer;
        if (faq != null) {
            log.info("FAQ matched for question: {}", question);
            answer = faq.getAnswer() + FAQ_SUFFIX;
        } else {
            IntentClassifier.IntentResult intentResult = intentClassifier.classifyWithConfidence(question);
            PromptTemplateService.IntentType intent = intentResult.type();
            double confidence = intentResult.confidence();
            log.info("Intent: {} (confidence={})", intent, String.format("%.2f", confidence));

            if (confidence < CONFIDENCE_LOW_THRESHOLD) {
                intent = PromptTemplateService.IntentType.LEGAL_QA;
            }

            String context = buildContextByIntent(intent, question, docType, lawDomain);
            List<Map<String, String>> history = chatMemoryService.getSessionHistory(session.getId());
            String memoryContext = userMemoryService.buildMemoryContext(userId, question);
            answer = routeAnswer(question, context, history, intent, confidence, memoryContext);
        }

        chatMemoryService.addMessage(session.getId(), "user", question);
        chatMemoryService.addMessage(session.getId(), "assistant", answer);

        saveMessage(session.getId(), userId, ChatMessage.MessageRole.USER, question);
        return saveMessage(session.getId(), userId, ChatMessage.MessageRole.ASSISTANT, answer);
    }

    /**
     * SSE流式法律问答（FAQ优先匹配）
     * 注意：不在主线程加 @Transactional，异步线程中的 DB 操作使用 TransactionTemplate 手动事务
     */
    public SseEmitter chatStream(Long userId, Long sessionId, String question, String docType, String lawDomain) {
        // 在主线程事务中完成 session 确认和用户消息保存
        ChatSession session = transactionTemplate.execute(status -> {
            ChatSession s = ensureSession(userId, sessionId, question);
            chatMemoryService.addMessage(s.getId(), "user", question);
            chatMemoryService.refreshSession(s.getId());
            saveMessage(s.getId(), userId, ChatMessage.MessageRole.USER, question);
            return s;
        });

        SseEmitter emitter = new SseEmitter(120_000L);

        // Build memory context for this user
        final String memoryContext = userMemoryService.buildMemoryContext(userId, question);

        // FAQ优先匹配：命中则直接推送FAQ答案
        FaqEntry faq = faqService.matchFaq(question);
        if (faq != null) {
            log.info("FAQ matched (stream) for question: {}", question);
            String faqAnswer = faq.getAnswer() + FAQ_SUFFIX;
            sseExecutor.execute(() -> {
                try {
                    emitter.send(SseEmitter.event().data(faqAnswer));
                    chatMemoryService.addMessage(session.getId(), "assistant", faqAnswer);
                    transactionTemplate.executeWithoutResult(status -> {
                        saveMessage(session.getId(), userId, ChatMessage.MessageRole.ASSISTANT, faqAnswer);
                    });
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    emitter.complete();
                } catch (Exception e) {
                    log.warn("FAQ SSE send failed: {}", e.getMessage());
                    emitter.completeWithError(e);
                }
            });
            return emitter;
        }

        IntentClassifier.IntentResult intentResult = intentClassifier.classifyWithConfidence(question);
        PromptTemplateService.IntentType resolvedIntent = intentResult.confidence() < CONFIDENCE_LOW_THRESHOLD
                ? PromptTemplateService.IntentType.LEGAL_QA : intentResult.type();
        final double finalConfidence = intentResult.confidence();
        log.info("Stream intent: {} (confidence={})", resolvedIntent, String.format("%.2f", finalConfidence));

        final String context = buildContextByIntent(resolvedIntent, question, docType, lawDomain);

        // Auto-compress context if usage exceeds threshold
        ChatMemoryService.ContextUsage usage = chatMemoryService.estimateContextUsage(session.getId());
        if (usage.needCompress()) {
            log.info("Auto-compressing context for session {} (usage={}%)", session.getId(), String.format("%.0f", usage.usageRatio() * 100));
            chatMemoryService.compressContext(session.getId());
        }

        final List<Map<String, String>> history = chatMemoryService.getSessionHistory(session.getId());
        final PromptTemplateService.IntentType finalIntent = resolvedIntent;

        StringBuilder fullAnswer = new StringBuilder();

        sseExecutor.execute(() -> {
            try {
                ZhipuApiClient.SseEventHandler baseHandler = new ZhipuApiClient.SseEventHandler() {
                    @Override
                    public void onContent(String content) {
                        try {
                            emitter.send(SseEmitter.event().data(content));
                            fullAnswer.append(content);
                        } catch (Exception e) {
                            log.warn("SSE send failed: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void onComplete() {
                        try {
                            if (finalConfidence < CONFIDENCE_LOW_THRESHOLD) {
                                emitter.send(SseEmitter.event().data(LOW_CONFIDENCE_SUFFIX));
                                fullAnswer.append(LOW_CONFIDENCE_SUFFIX);
                            }
                            String answer = fullAnswer.toString();
                            chatMemoryService.addMessage(session.getId(), "assistant", answer);
                            transactionTemplate.executeWithoutResult(status -> {
                                saveMessage(session.getId(), userId, ChatMessage.MessageRole.ASSISTANT, answer);
                            });
                            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                            emitter.complete();
                        } catch (Exception e) {
                            log.warn("SSE complete failed: {}", e.getMessage());
                        }
                    }
                };

                if (finalIntent == PromptTemplateService.IntentType.COMPLEX_LEGAL) {
                    chatService.complexLegalQaStream(question, context, history, memoryContext, baseHandler);
                } else {
                    chatService.legalQaStream(question, context, history, finalIntent, memoryContext, baseHandler);
                }
            } catch (Exception e) {
                log.error("SSE stream error: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> {
            log.warn("SSE emitter timeout for session {}", session.getId());
            chatMemoryService.addMessage(session.getId(), "assistant", fullAnswer.toString());
        });

        return emitter;
    }

    /**
     * 删除对话会话及其所有消息（删除前同步生成摘要）
     */
    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        ChatSession session = chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在或无权限"));
        // Generate summary before deleting
        sessionSummaryService.generateSummary(sessionId, userId);
        chatMemoryService.clearSession(sessionId);
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.delete(session);
        log.info("Session {} deleted by user {}", sessionId, userId);
    }

    /**
     * 重命名对话会话
     */
    @Transactional
    public ChatSession renameSession(Long userId, Long sessionId, String title) {
        ChatSession session = chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在或无权限"));
        session.setTitle(title);
        return chatSessionRepository.save(session);
    }

    /**
     * 法条检索
     */
    public List<HybridSearchService.SearchResult> searchLaw(String query, String docType, String lawDomain, int topK) {
        return hybridSearchService.search(query, docType, lawDomain, topK);
    }

    private String buildContextByIntent(PromptTemplateService.IntentType intent, String question,
                                        String docType, String lawDomain) {
        if (intent == PromptTemplateService.IntentType.CASE_QUERY) {
            return buildCaseContext(question);
        }
        return ragService.retrieveAndBuildContextEnhanced(question, docType, lawDomain, 5);
    }

    private String buildCaseContext(String question) {
        try {
            List<HybridSearchService.SearchResult> results = caseService.semanticSearch(question, null, 5);
            if (results.isEmpty()) {
                return ragService.retrieveAndBuildContextEnhanced(question, null, null, 5);
            }
            StringBuilder sb = new StringBuilder("相关法律案例：\n\n");
            for (int i = 0; i < results.size(); i++) {
                sb.append("【案例").append(i + 1).append("】\n");
                sb.append(results.get(i).content()).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Case context build failed, falling back to RAG: {}", e.getMessage());
            return ragService.retrieveAndBuildContextEnhanced(question, null, null, 5);
        }
    }

    private String routeAnswer(String question, String context, List<Map<String, String>> history,
                                PromptTemplateService.IntentType intent, double confidence, String memoryContext) {
        String answer = switch (intent) {
            case COMPLEX_LEGAL -> chatService.legalQaWithTools(question, context, history, memoryContext);
            default -> chatService.legalQa(question, context, history, intent, memoryContext);
        };
        if (confidence < CONFIDENCE_LOW_THRESHOLD) {
            answer = answer + LOW_CONFIDENCE_SUFFIX;
        }
        return answer;
    }

    private ChatSession ensureSession(Long userId, Long sessionId, String question) {
        if (sessionId != null) {
            chatMemoryService.refreshSession(sessionId);
            return chatSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        }
        String title = question.length() > 20 ? question.substring(0, 20) + "..." : question;
        return createSession(userId, title);
    }

    /**
     * 显式结束会话：设status=ENDED，触发异步摘要生成
     */
    @Transactional
    public void endSession(Long userId, Long sessionId) {
        ChatSession session = chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在或无权限"));
        session.setStatus(ChatSession.SessionStatus.ENDED.name());
        chatSessionRepository.save(session);
        chatMemoryService.endSession(sessionId);
        // 异步生成摘要
        sessionSummaryService.generateSummaryAsync(sessionId, userId);
        log.info("Session {} ended by user {}", sessionId, userId);
    }

    /**
     * 结束当前会话 + 创建新会话 + 预热记忆
     */
    @Transactional
    public ChatSession startNewSession(Long userId, Long currentSessionId, String question) {
        // End current session if exists
        if (currentSessionId != null) {
            try {
                endSession(userId, currentSessionId);
            } catch (Exception e) {
                log.warn("Failed to end session {}: {}", currentSessionId, e.getMessage());
            }
        }

        // Create new session
        String title = question != null && question.length() > 20 ? question.substring(0, 20) + "..." : "新对话";
        ChatSession newSession = createSession(userId, title);

        // Warmup with L2/L3 memories
        String memoryContext = userMemoryService.buildMemoryContext(userId, question);
        if (memoryContext != null && !memoryContext.isBlank()) {
            List<Map<String, String>> warmupMessages = List.of(
                    Map.of("role", "system", "content", memoryContext)
            );
            chatMemoryService.warmupSession(newSession.getId(), warmupMessages);
        }

        log.info("New session {} created for user {} (warmed up: {})", newSession.getId(), userId, memoryContext != null);
        return newSession;
    }

    private ChatMessage saveMessage(Long sessionId, Long userId, ChatMessage.MessageRole role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        return chatMessageRepository.save(message);
    }
}
