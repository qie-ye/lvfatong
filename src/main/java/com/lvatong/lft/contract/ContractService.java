package com.lvatong.lft.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lvatong.lft.ai.AgentOrchestrator;
import com.lvatong.lft.ai.ChatService;
import com.lvatong.lft.ai.VerificationService;
import com.lvatong.lft.async.AsyncTaskMessage;
import com.lvatong.lft.async.AsyncTaskProducer;
import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.service.NotificationService;
import com.lvatong.lft.model.dto.ContractAnalysisResult;
import com.lvatong.lft.model.dto.ContractModificationSuggestion;
import com.lvatong.lft.model.dto.ContractUploadResponse;
import com.lvatong.lft.model.entity.ContractDocument;
import com.lvatong.lft.model.entity.ContractTemplate;
import com.lvatong.lft.rag.RAGService;
import com.lvatong.lft.repository.ContractDocumentRepository;
import com.lvatong.lft.repository.ContractTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final DocumentParser documentParser;
    private final ClauseExtractor clauseExtractor;
    private final RiskAssessor riskAssessor;
    private final ChatService chatService;
    private final RAGService ragService;
    private final VerificationService verificationService;
    private final AgentOrchestrator agentOrchestrator;
    private final ContractDocumentRepository contractDocumentRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ObjectMapper objectMapper;
    private final AsyncTaskProducer asyncTaskProducer;
    private final NotificationService notificationService;

    @Value("${contract.upload-dir:./uploads/contracts}")
    private String uploadDir;

    /**
     * 上传并解析合同文件
     */
    @Transactional
    public ContractUploadResponse upload(Long userId, MultipartFile file) {
        documentParser.validate(file);

        String originalFilename = file.getOriginalFilename();
        String extension = documentParser.getExtension(originalFilename != null ? originalFilename : "unknown");
        String storedFilename = UUID.randomUUID() + "." + extension;

        ContractDocument doc = new ContractDocument();
        doc.setUserId(userId);
        doc.setFilename(originalFilename);
        doc.setFileType(documentParser.getFileType(file));
        doc.setFileSize(file.getSize());
        doc.setFilePath(Paths.get(uploadDir, storedFilename).toString());
        doc.setStatus(ContractDocument.AnalysisStatus.UPLOADED);

        String parsedText;
        try {
            doc.setStatus(ContractDocument.AnalysisStatus.PARSING);
            parsedText = documentParser.parse(file);
            doc.setParsedText(parsedText);
            doc.setStatus(ContractDocument.AnalysisStatus.PARSED);
        } catch (Exception e) {
            doc.setStatus(ContractDocument.AnalysisStatus.FAILED);
            contractDocumentRepository.save(doc);
            throw new BusinessException("文档解析失败: " + e.getMessage());
        }

        Path uploadPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(storedFilename);
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }

        doc = contractDocumentRepository.save(doc);

        return ContractUploadResponse.from(doc.getId(), originalFilename,
                doc.getFileType(), doc.getFileSize(), doc.getStatus().name());
    }

    /**
     * 触发异步合同分析
     */
    public void analyzeAsync(Long userId, Long contractId) {
        ContractDocument doc = contractDocumentRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException("合同文档不存在"));
        verifyOwnership(doc, userId);

        if (doc.getStatus() == ContractDocument.AnalysisStatus.ANALYZING) {
            throw new BusinessException("合同正在分析中，请稍后查询结果");
        }
        if (doc.getParsedText() == null || doc.getParsedText().isBlank()) {
            throw new BusinessException("合同文档尚未解析完成");
        }

        doc.setStatus(ContractDocument.AnalysisStatus.ANALYZING);
        contractDocumentRepository.save(doc);

        asyncTaskProducer.publish(new AsyncTaskMessage(
                AsyncTaskMessage.TaskType.CONTRACT, contractId, userId));
    }

    /**
     * 执行合同分析（由 AsyncTaskRouter 通过 Redis Stream 调用）
     */
    public void executeAnalysis(Long contractId) {
        ContractDocument doc = contractDocumentRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException("合同文档不存在"));

        try {
            List<ClauseExtractor.Clause> clauses = clauseExtractor.extract(doc.getParsedText());
            log.info("Contract {} extracted {} clauses", contractId, clauses.size());

            List<ContractAnalysisResult.ClauseAnalysis> ruleResults = riskAssessor.assess(clauses);
            ContractAnalysisResult result = enhanceWithAI(doc.getParsedText(), ruleResults);

            String jsonResult = objectMapper.writeValueAsString(result);
            doc.setAnalysisResult(jsonResult);
            doc.setStatus(ContractDocument.AnalysisStatus.COMPLETED);
            contractDocumentRepository.save(doc);
            notificationService.send(doc.getUserId(), "CONTRACT_ANALYSIS",
                    "合同分析完成",
                    "您的合同《" + doc.getFilename() + "》已分析完成，请查看分析报告。");
        } catch (Exception e) {
            log.error("Contract analysis failed for {}: {}", contractId, e.getMessage());
            doc.setStatus(ContractDocument.AnalysisStatus.FAILED);
            contractDocumentRepository.save(doc);
            notificationService.send(doc.getUserId(), "CONTRACT_ANALYSIS",
                    "合同分析失败",
                    "您的合同《" + doc.getFilename() + "》分析失败，请重新提交。");
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取合同分析结果
     */
    public ContractAnalysisResult getAnalysisResult(Long userId, Long contractId) {
        ContractDocument doc = contractDocumentRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException("合同文档不存在"));
        verifyOwnership(doc, userId);

        if (doc.getAnalysisResult() == null) {
            throw new BusinessException("合同尚未分析完成");
        }

        try {
            return objectMapper.readValue(doc.getAnalysisResult(), ContractAnalysisResult.class);
        } catch (Exception e) {
            throw new BusinessException("分析结果解析失败");
        }
    }

    /**
     * 获取用户的合同列表
     */
    public List<ContractDocument> listByUser(Long userId) {
        return contractDocumentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取合同文档（含权限校验）
     */
    public ContractDocument getContractForUser(Long userId, Long contractId) {
        ContractDocument doc = contractDocumentRepository.findById(contractId)
                .orElseThrow(() -> new BusinessException("合同文档不存在"));
        verifyOwnership(doc, userId);
        return doc;
    }

    private static final int COMPLEX_HIGH_RISK_THRESHOLD = 2;
    private static final int COMPLEX_TEXT_LENGTH_THRESHOLD = 6000;

    /**
     * AI增强分析：复杂合同（高风险条款≥2 或 文本>6000字）走 Multi-Agent 流程，否则走单次调用
     */
    private ContractAnalysisResult enhanceWithAI(String contractText,
                                                  List<ContractAnalysisResult.ClauseAnalysis> ruleResults) {
        ContractAnalysisResult result = new ContractAnalysisResult();
        result.setClauses(ruleResults);

        long highRiskCount = ruleResults.stream()
                .filter(c -> "高".equals(c.getRiskLevel())).count();
        boolean isComplex = highRiskCount >= COMPLEX_HIGH_RISK_THRESHOLD
                || contractText.length() > COMPLEX_TEXT_LENGTH_THRESHOLD;

        if (isComplex) {
            log.info("Complex contract detected (highRisk={}, len={}), using agent orchestration",
                    highRiskCount, contractText.length());
            enhanceWithAgents(contractText, result);
        } else {
            enhanceWithSingleCall(contractText, result);
        }

        if (result.getSummary() == null || result.getSummary().isBlank()) {
            result.setSummary(generateRuleSummary(ruleResults));
        }
        if (result.getOverallRisk() == null || result.getOverallRisk().isBlank()) {
            result.setOverallRisk(riskAssessor.calculateOverallRisk(ruleResults));
        }

        // 规则-AI 交叉验证
        try {
            VerificationService.ContractConflictResult conflict =
                    verificationService.detectContractConflicts(ruleResults, result);
            result.setVerificationScore(conflict.score());
            result.setConflictNotes(conflict.conflicts().isEmpty() ? null : conflict.conflicts());
            if (!conflict.consistent()) {
                log.info("Contract conflict detected: {}", conflict.conflicts());
                result.setOverallRisk(riskAssessor.calculateOverallRisk(ruleResults));
            }
        } catch (Exception e) {
            log.warn("Contract cross-validation failed: {}", e.getMessage());
        }

        return result;
    }

    private void enhanceWithAgents(String contractText, ContractAnalysisResult result) {
        try {
            String ragContext = ragService.retrieveAndBuildContext("合同条款分析 违约责任 格式条款", 3);
            String agentSummary = agentOrchestrator.orchestrateComplexContract(contractText, ragContext);
            result.setSummary(agentSummary);
            result.setOverallRisk(extractOverallRiskFromSummary(agentSummary));
        } catch (Exception e) {
            log.warn("Agent contract analysis failed, falling back to single call: {}", e.getMessage());
            enhanceWithSingleCall(contractText, result);
        }
    }

    private void enhanceWithSingleCall(String contractText, ContractAnalysisResult result) {
        try {
            String context = ragService.retrieveAndBuildContext("合同条款分析 违约责任 格式条款", 3);
            String truncatedText = contractText.length() > 10000
                    ? contractText.substring(0, 10000) + "\n...(文档过长已截断)"
                    : contractText;
            String aiResponse = chatService.contractAnalysis(truncatedText, context);

            try {
                String jsonStr = extractJsonFromResponse(aiResponse);
                ContractAnalysisResult aiResult = objectMapper.readValue(jsonStr, ContractAnalysisResult.class);
                if (aiResult.getSummary() != null) {
                    result.setSummary(aiResult.getSummary());
                }
                if (aiResult.getOverallRisk() != null) {
                    result.setOverallRisk(aiResult.getOverallRisk());
                }
                if (aiResult.getClauses() != null && !aiResult.getClauses().isEmpty()) {
                    mergeAiClauses(result, aiResult.getClauses());
                }
            } catch (Exception e) {
                log.warn("AI response JSON parse failed, using AI response as summary: {}", e.getMessage());
                result.setSummary(aiResponse.length() > 2000 ? aiResponse.substring(0, 2000) : aiResponse);
            }
        } catch (Exception e) {
            log.warn("AI single-call analysis failed: {}", e.getMessage());
        }
    }

    private void verifyOwnership(ContractDocument doc, Long userId) {
        if (!doc.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该合同文档");
        }
    }

    /**
     * 从 Agent 生成的摘要文本中启发式提取整体风险等级
     */
    private String extractOverallRiskFromSummary(String summary) {
        if (summary == null || summary.isBlank()) return null;
        if (summary.contains("高风险") || summary.contains("风险：高") || summary.contains("整体风险：高")) return "高";
        if (summary.contains("中风险") || summary.contains("风险：中") || summary.contains("整体风险：中")) return "中";
        if (summary.contains("低风险") || summary.contains("风险：低") || summary.contains("整体风险：低")) return "低";
        return null;
    }

    private String extractJsonFromResponse(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private void mergeAiClauses(ContractAnalysisResult ruleResult,
                                 List<ContractAnalysisResult.ClauseAnalysis> aiClauses) {
        for (ContractAnalysisResult.ClauseAnalysis aiClause : aiClauses) {
            boolean merged = false;
            for (ContractAnalysisResult.ClauseAnalysis ruleClause : ruleResult.getClauses()) {
                if (ruleClause.getIndex() == aiClause.getIndex()) {
                    if (aiClause.getLegalBasis() != null && !aiClause.getLegalBasis().isBlank()
                            && (ruleClause.getLegalBasis() == null || ruleClause.getLegalBasis().isBlank())) {
                        ruleClause.setLegalBasis(aiClause.getLegalBasis());
                    }
                    if (aiClause.getSuggestion() != null && !aiClause.getSuggestion().isBlank()
                            && (ruleClause.getSuggestion() == null || ruleClause.getSuggestion().isBlank())) {
                        ruleClause.setSuggestion(aiClause.getSuggestion());
                    }
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                ruleResult.getClauses().add(aiClause);
            }
        }
    }

    private String generateRuleSummary(List<ContractAnalysisResult.ClauseAnalysis> clauses) {
        long high = clauses.stream().filter(c -> "高".equals(c.getRiskLevel())).count();
        long medium = clauses.stream().filter(c -> "中".equals(c.getRiskLevel())).count();
        long low = clauses.stream().filter(c -> "低".equals(c.getRiskLevel())).count();
        return String.format("合同共分析%d个条款，其中高风险%d项、中风险%d项、低风险%d项。",
                clauses.size(), high, medium, low);
    }

    /**
     * 生成修改建议（基于分析结果 + AI深度建议）
     */
    public List<ContractModificationSuggestion> generateModificationSuggestions(Long userId, Long contractId) {
        ContractAnalysisResult analysis = getAnalysisResult(userId, contractId);
        ContractDocument doc = getContractForUser(userId, contractId);

        List<ContractModificationSuggestion> suggestions = new java.util.ArrayList<>();

        // 从分析结果中提取修改建议
        for (ContractAnalysisResult.ClauseAnalysis clause : analysis.getClauses()) {
            if (clause.getSuggestion() != null && !clause.getSuggestion().isBlank()
                    && !"低".equals(clause.getRiskLevel())) {
                suggestions.add(ContractModificationSuggestion.builder()
                        .clauseIndex(clause.getIndex())
                        .clauseTitle(clause.getTitle())
                        .riskLevel(clause.getRiskLevel())
                        .originalContent(clause.getContent())
                        .suggestion(clause.getSuggestion())
                        .legalBasis(clause.getLegalBasis())
                        .build());
            }
        }

        // AI增强：对高风险条款生成具体修改文本
        try {
            String highRiskClauses = suggestions.stream()
                    .filter(s -> "高".equals(s.getRiskLevel()))
                    .map(s -> "条款" + s.getClauseIndex() + "(" + s.getClauseTitle() + "): " + s.getOriginalContent())
                    .reduce((a, b) -> a + "\n\n" + b)
                    .orElse("");

            if (!highRiskClauses.isBlank()) {
                String context = ragService.retrieveAndBuildContext("合同条款修改建议 违约责任 格式条款", 3);
                String prompt = "请针对以下高风险合同条款，给出具体的修改建议文本（包含修改前和修改后的对比）：\n\n" + highRiskClauses;
                String aiResponse = chatService.legalQa(prompt, context);

                // 将AI建议附加到第一个高风险建议上
                if (!suggestions.isEmpty()) {
                    suggestions.get(0).setAiModificationDetail(aiResponse.length() > 3000
                            ? aiResponse.substring(0, 3000) : aiResponse);
                }
            }
        } catch (Exception e) {
            log.warn("AI modification suggestion failed: {}", e.getMessage());
        }

        return suggestions;
    }

    /**
     * 合同对比：比较两份合同的差异
     */
    public String compareContracts(Long userId, Long contractId1, Long contractId2) {
        ContractDocument doc1 = getContractForUser(userId, contractId1);
        ContractDocument doc2 = getContractForUser(userId, contractId2);

        String text1 = doc1.getParsedText();
        String text2 = doc2.getParsedText();

        if (text1 == null || text2 == null) {
            throw new BusinessException("合同文本未解析，无法对比");
        }

        // 简单文本差异对比
        String truncated1 = text1.length() > 8000 ? text1.substring(0, 8000) : text1;
        String truncated2 = text2.length() > 8000 ? text2.substring(0, 8000) : text2;

        try {
            String prompt = String.format(
                    "请对比以下两份合同文本，列出关键差异点，包括：条款增减、权利义务变化、风险差异等。\n\n" +
                    "【合同一：%s】\n%s\n\n【合同二：%s】\n%s",
                    doc1.getFilename(), truncated1, doc2.getFilename(), truncated2);

            return chatService.legalQa(prompt, "");
        } catch (Exception e) {
            throw new BusinessException("合同对比分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取合同模板列表
     */
    @Cacheable(value = "contractTemplatesCache", key = "#category ?: 'all'")
    public List<ContractTemplate> listTemplates(String category) {
        if (category != null && !category.isBlank()) {
            return contractTemplateRepository.findByCategory(category);
        }
        return contractTemplateRepository.findByEnabledTrueOrderByCategory();
    }

    /**
     * 获取模板详情
     */
    @Cacheable(value = "contractTemplatesCache", key = "'tmpl:' + #templateId")
    public ContractTemplate getTemplate(Long templateId) {
        return contractTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("模板不存在"));
    }
}
