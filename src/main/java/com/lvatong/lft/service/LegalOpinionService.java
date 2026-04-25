package com.lvatong.lft.service;

import com.lvatong.lft.ai.AgentOrchestrator;
import com.lvatong.lft.ai.ChatService;
import com.lvatong.lft.ai.ModelRouterService;
import com.lvatong.lft.ai.ZhipuApiClient;
import com.lvatong.lft.async.AsyncTaskMessage;
import com.lvatong.lft.async.AsyncTaskProducer;
import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.dto.GenerateOpinionRequest;
import com.lvatong.lft.model.dto.LegalOpinionResponse;
import com.lvatong.lft.model.entity.LegalOpinion;
import com.lvatong.lft.rag.RAGService;
import com.lvatong.lft.repository.LegalOpinionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegalOpinionService {

    private final LegalOpinionRepository legalOpinionRepository;
    private final ChatService chatService;
    private final RAGService ragService;
    private final ZhipuApiClient zhipuApiClient;
    private final ModelRouterService modelRouterService;
    private final AgentOrchestrator agentOrchestrator;
    private final AsyncTaskProducer asyncTaskProducer;

    private static final Pattern P_ANALYSIS = Pattern.compile("(?s)【法律分析】(.*?)(?=【|\\Z)");
    private static final Pattern P_CONCLUSION = Pattern.compile("(?s)【结论】(.*?)(?=【|\\Z)");
    private static final Pattern P_BASIS = Pattern.compile("(?s)【法律依据】(.*?)(?=【|\\Z)");
    private static final Pattern P_SUGGESTION = Pattern.compile("(?s)【建议】(.*?)(?=【|\\Z)");

    private static final String OPINION_SYSTEM_PROMPT = """
            你是一位资深法律顾问，正在为用户撰写正式的法律意见书。请严格按照以下格式输出：

            【法律分析】
            对案件事实进行详细的法律分析，包括：
            1. 法律关系梳理
            2. 权利义务分析
            3. 法律适用论证
            4. 争议焦点归纳

            【结论】
            基于上述分析，给出明确的法律结论。

            【法律依据】
            列出适用的法律法规、司法解释和指导性案例。

            【建议】
            给出具体的行动建议，包括：
            1. 维权路径
            2. 证据收集建议
            3. 时效提醒
            4. 风险提示

            注意：
            - 分析必须严谨、客观，有法律依据支撑
            - 引用具体法条时标注法律名称和条款号
            - 如存在多种法律观点，应分别阐述
            - 不得给出确定性胜诉承诺
            """;

    /**
     * 生成法律意见（异步）
     */
    @Transactional
    public LegalOpinionResponse generateOpinion(Long userId, GenerateOpinionRequest request) {
        LegalOpinion opinion = new LegalOpinion();
        opinion.setUserId(userId);
        opinion.setTitle(request.getTitle());
        opinion.setDomain(request.getDomain());
        opinion.setQuestion(request.getQuestion());
        opinion.setFacts(request.getFacts());
        opinion.setStatus("GENERATING");
        opinion = legalOpinionRepository.save(opinion);

        try {
            asyncTaskProducer.publish(new AsyncTaskMessage(
                    AsyncTaskMessage.TaskType.OPINION, opinion.getId(), userId));
        } catch (Exception e) {
            log.warn("Redis Stream unavailable, executing opinion generation synchronously: {}", e.getMessage());
            try {
                executeGeneration(opinion.getId());
            } catch (Exception ex) {
                log.error("Synchronous opinion generation also failed: {}", ex.getMessage());
            }
        }

        return LegalOpinionResponse.summaryFrom(opinion);
    }

    /**
     * 执行意见生成（由 AsyncTaskRouter 通过 Redis Stream 调用）
     */
    public void executeGeneration(Long opinionId) {
        LegalOpinion opinion = legalOpinionRepository.findById(opinionId)
                .orElseThrow(() -> new BusinessException("法律意见不存在"));

        try {
            String ragContext = ragService.retrieveAndBuildContextEnhanced(
                    opinion.getQuestion() + " " + (opinion.getDomain() != null ? opinion.getDomain() : ""),
                    null, opinion.getDomain(), 5);

            String model = modelRouterService.getModelForTask(ModelRouterService.TaskType.DEEP_REASONING);
            opinion.setModel(model);

            String response = generateWithAgents(opinion, ragContext);

            parseOpinionResponse(opinion, response);

            opinion.setStatus("COMPLETED");
            legalOpinionRepository.save(opinion);
            log.info("Legal opinion generated (agent): id={}", opinionId);

        } catch (Exception e) {
            log.error("Legal opinion generation failed for {}: {}", opinionId, e.getMessage());
            opinion.setStatus("FAILED");
            legalOpinionRepository.save(opinion);
        }
    }

    /**
     * 使用 Multi-Agent 编排生成法律意见；失败时降级为单次 LLM 调用
     */
    private String generateWithAgents(LegalOpinion opinion, String ragContext) {
        try {
            AgentOrchestrator.AgentResult result = agentOrchestrator.orchestrateLegalOpinion(
                    opinion.getQuestion(),
                    opinion.getFacts(),
                    opinion.getDomain(),
                    ragContext);
            if (!result.finalAnswer().isBlank()) {
                return result.finalAnswer();
            }
        } catch (Exception e) {
            log.warn("Agent orchestration failed, falling back to single LLM call: {}", e.getMessage());
        }

        // 降级：原单次调用路径
        String model = modelRouterService.getModelForTask(ModelRouterService.TaskType.DEEP_REASONING);
        List<java.util.Map<String, String>> messages = List.of(
                java.util.Map.of("role", "system", "content", OPINION_SYSTEM_PROMPT + "\n\n相关法律知识：\n" + ragContext),
                java.util.Map.of("role", "user", "content", buildOpinionPrompt(opinion))
        );
        return zhipuApiClient.chat(model, messages, 0.6, 8192);
    }

    private String buildOpinionPrompt(LegalOpinion opinion) {
        StringBuilder sb = new StringBuilder();
        sb.append("请就以下法律问题出具法律意见书：\n\n");
        sb.append("问题：").append(opinion.getQuestion()).append("\n\n");
        if (opinion.getFacts() != null && !opinion.getFacts().isBlank()) {
            sb.append("案件事实：\n").append(opinion.getFacts()).append("\n\n");
        }
        if (opinion.getDomain() != null && !opinion.getDomain().isBlank()) {
            sb.append("法律领域：").append(opinion.getDomain()).append("\n\n");
        }
        return sb.toString();
    }

    private void parseOpinionResponse(LegalOpinion opinion, String response) {
        opinion.setAnalysis(extractSection(response, P_ANALYSIS));
        opinion.setConclusion(extractSection(response, P_CONCLUSION));
        opinion.setLegalBasis(extractSection(response, P_BASIS));
        opinion.setSuggestions(extractSection(response, P_SUGGESTION));

        // Agent 可能不输出【】标记格式 — 按段落回退解析
        if (opinion.getAnalysis() == null || opinion.getAnalysis().isBlank()) {
            // 尝试取第一个较长段落作为分析
            String[] paragraphs = response.split("\n{2,}");
            for (String p : paragraphs) {
                if (p.length() > 100) {
                    opinion.setAnalysis(p.trim());
                    break;
                }
            }
        }
        // 如果仍为空，将完整响应存入 analysis
        if ((opinion.getAnalysis() == null || opinion.getAnalysis().isBlank())
                && (opinion.getConclusion() == null || opinion.getConclusion().isBlank())) {
            opinion.setAnalysis(response);
        }
    }

    private String extractSection(String text, Pattern pattern) {
        var matcher = pattern.matcher(text);
        if (matcher.find()) {
            String content = matcher.group(1).trim();
            return content.isBlank() ? null : content;
        }
        return null;
    }

    /**
     * 获取法律意见详情
     */
    public LegalOpinionResponse getOpinion(Long userId, Long opinionId) {
        LegalOpinion opinion = legalOpinionRepository.findById(opinionId)
                .orElseThrow(() -> new BusinessException("法律意见不存在"));
        if (!opinion.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该法律意见");
        }
        return LegalOpinionResponse.from(opinion);
    }

    /**
     * 获取用户的法律意见列表
     */
    public List<LegalOpinionResponse> listByUser(Long userId) {
        return legalOpinionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(LegalOpinionResponse::summaryFrom)
                .toList();
    }
}
