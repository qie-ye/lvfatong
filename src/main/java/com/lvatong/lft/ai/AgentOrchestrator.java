package com.lvatong.lft.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 轻量级 Multi-Agent 编排器（Supervisor 模式）
 *
 * 法律意见生成流程：Researcher → Analyst → Critic → Writer
 * 复杂合同分析流程：ContractResearcher → ContractAnalyst → ContractCritic → ContractWriter
 *
 * 每个 Agent 是一次带专属 prompt 的 simpleChat 调用；
 * 任意 Agent 失败时以空字符串降级继续，最终由 Writer 兜底。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final ChatService chatService;
    private final ModelRouterService modelRouterService;
    private final AgentPrompt agentPrompt;

    public record AgentResult(
            String researchSummary,
            String analysis,
            String critique,
            String finalAnswer
    ) {}

    /**
     * 法律意见 Agent 编排（法律意见生成专用）
     */
    public AgentResult orchestrateLegalOpinion(String question, String facts, String domain, String ragContext) {
        String fastModel = modelRouterService.getModelForTask(ModelRouterService.TaskType.LEGAL_QA);
        String deepModel = modelRouterService.getModelForTask(ModelRouterService.TaskType.DEEP_REASONING);

        // Step 1: Researcher — 提炼法律知识
        String researchSummary = runAgent("Researcher",
                agentPrompt.buildResearcherPrompt(question, facts, domain, ragContext),
                fastModel, 0.3, 2048);

        // Step 2: Analyst — 深度法律分析
        String analysis = runAgent("Analyst",
                agentPrompt.buildAnalystPrompt(question, facts, domain, researchSummary),
                deepModel, 0.6, 4096);

        // Step 3: Critic — 批判性审查
        String critique = runAgent("Critic",
                agentPrompt.buildCriticPrompt(question, analysis),
                fastModel, 0.3, 1024);

        // Step 3b: Analyst Revise — Critic 发现问题时修正分析
        boolean criticPassed = critique.isBlank() || critique.contains("无需修正");
        if (!criticPassed) {
            log.info("Critic found issues, running Analyst revision step");
            String revised = runAgent("AnalystRevise",
                    agentPrompt.buildAnalystRevisePrompt(question, analysis, critique),
                    deepModel, 0.5, 4096);
            if (!revised.isBlank()) {
                analysis = revised;
            }
        }

        // Step 4: Writer — 整合输出正式意见书
        String finalAnswer = runAgent("Writer",
                agentPrompt.buildWriterPrompt(question, facts, domain, analysis, critique),
                deepModel, 0.5, 8192);

        if (finalAnswer.isBlank()) {
            log.warn("Writer agent produced empty output, falling back to analyst output");
            finalAnswer = analysis;
        }

        log.info("Legal opinion agent orchestration completed");
        return new AgentResult(researchSummary, analysis, critique, finalAnswer);
    }

    /**
     * 复杂合同 Agent 编排
     */
    public String orchestrateComplexContract(String contractText, String ragContext) {
        String fastModel = modelRouterService.getModelForTask(ModelRouterService.TaskType.LEGAL_QA);
        String deepModel = modelRouterService.getModelForTask(ModelRouterService.TaskType.DEEP_REASONING);

        // Step 1: ContractResearcher
        String research = runAgent("ContractResearcher",
                agentPrompt.buildContractResearcherPrompt(contractText, ragContext),
                fastModel, 0.3, 1024);

        // Step 2: ContractAnalyst
        String analysis = runAgent("ContractAnalyst",
                agentPrompt.buildContractAnalystPrompt(contractText, research),
                deepModel, 0.6, 4096);

        // Step 3: ContractCritic
        String critique = runAgent("ContractCritic",
                agentPrompt.buildContractCriticPrompt(contractText, analysis),
                fastModel, 0.3, 1024);

        // Step 4: ContractWriter
        String finalSummary = runAgent("ContractWriter",
                agentPrompt.buildContractWriterPrompt(contractText, analysis, critique),
                deepModel, 0.5, 4096);

        if (finalSummary.isBlank()) {
            log.warn("ContractWriter agent produced empty output, falling back to analyst output");
            return analysis;
        }

        log.info("Complex contract agent orchestration completed");
        return finalSummary;
    }

    private String runAgent(String agentName, String prompt, String model, double temperature, int maxTokens) {
        try {
            log.info("Running {} agent with model={}", agentName, model);
            String result = chatService.simpleChat(prompt, model, temperature, maxTokens);
            log.info("{} agent completed ({} chars)", agentName, result.length());
            return result;
        } catch (Exception e) {
            log.warn("{} agent failed: {}", agentName, e.getMessage());
            return "";
        }
    }
}
