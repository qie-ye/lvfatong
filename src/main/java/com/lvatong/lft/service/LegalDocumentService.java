package com.lvatong.lft.service;

import com.lvatong.lft.ai.ModelRouterService;
import com.lvatong.lft.ai.ZhipuApiClient;
import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.dto.GenerateDocumentRequest;
import com.lvatong.lft.model.dto.LegalDocumentResponse;
import com.lvatong.lft.model.entity.LegalDocument;
import com.lvatong.lft.rag.RAGService;
import com.lvatong.lft.repository.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegalDocumentService {

    private final LegalDocumentRepository legalDocumentRepository;
    private final ZhipuApiClient zhipuApiClient;
    private final RAGService ragService;
    private final ModelRouterService modelRouterService;

    private static final Map<String, String> DOC_TYPE_PROMPTS = Map.of(
            "COMPLAINT", """
                    请根据以下信息生成一份民事起诉状，格式要求：
                    1. 标题：民事起诉状
                    2. 原告信息（姓名、性别、民族、出生日期、住址、联系方式）
                    3. 被告信息（姓名/名称、住址、联系方式）
                    4. 诉讼请求（分条列明）
                    5. 事实与理由（详细陈述案件事实，引用相关法律条文）
                    6. 证据清单
                    7. 此致 XXX人民法院
                    8. 附：本诉状副本X份
                    注意：使用正式法律文书用语，法条引用准确。""",

            "DEFENSE", """
                    请根据以下信息生成一份民事答辩状，格式要求：
                    1. 标题：民事答辩状
                    2. 答辩人信息
                    3. 被答辩人信息
                    4. 答辩请求
                    5. 答辩理由（针对原告诉求逐条答辩，引用法律依据）
                    6. 证据清单
                    7. 此致 XXX人民法院
                    注意：针对性强，逻辑严密，法条引用准确。""",

            "ARBITRATION", """
                    请根据以下信息生成一份劳动仲裁申请书，格式要求：
                    1. 标题：劳动争议仲裁申请书
                    2. 申请人信息（姓名、性别、身份证号、住址、联系方式）
                    3. 被申请人信息（单位名称、住所地、法定代表人、联系方式）
                    4. 仲裁请求（分条列明）
                    5. 事实与理由（详细陈述，引用劳动法相关条文）
                    6. 证据清单
                    7. 此致 XXX劳动争议仲裁委员会
                    注意：劳动争议需先仲裁后诉讼，引用劳动法/劳动合同法条文。""",

            "PETITION", """
                    请根据以下信息生成一份申请书（如财产保全申请书/先予执行申请书等），格式要求：
                    1. 标题：申请书
                    2. 申请人信息
                    3. 请求事项
                    4. 事实与理由
                    5. 担保情况（如适用）
                    6. 此致 XXX人民法院
                    注意：理由充分，法律依据明确。""",

            "INDICTMENT", """
                    请根据以下信息生成一份刑事自诉状/报案材料，格式要求：
                    1. 标题：刑事自诉状/报案材料
                    2. 自诉人/报案人信息
                    3. 被告/嫌疑人信息
                    4. 案由
                    5. 诉讼请求/报案请求
                    6. 事实与理由（详细陈述犯罪事实，引用刑法条文）
                    7. 证据清单
                    8. 此致 XXX人民法院/公安机关
                    注意：犯罪构成要件分析清晰，法条引用准确。""",

            "OTHER", """
                    请根据以下信息生成一份法律文书，使用正式法律文书格式，包含：
                    1. 标题
                    2. 当事人信息
                    3. 请求事项
                    4. 事实与理由
                    5. 法律依据
                    6. 证据清单
                    注意：使用正式法律文书用语，法条引用准确。"""
    );

    /**
     * 生成法律文书（异步）
     */
    @Transactional
    public LegalDocumentResponse generateDocument(Long userId, GenerateDocumentRequest request) {
        LegalDocument doc = new LegalDocument();
        doc.setUserId(userId);
        doc.setTitle(request.getTitle());
        doc.setDocType(request.getDocType());
        doc.setDomain(request.getDomain());
        doc.setFacts(request.getFacts());
        doc.setClaims(request.getClaims());
        doc.setStatus("GENERATING");
        doc = legalDocumentRepository.save(doc);

        doGenerateAsync(doc.getId());

        return LegalDocumentResponse.summaryFrom(doc);
    }

    @Async("documentExecutor")
    public void doGenerateAsync(Long docId) {
        doGenerate(docId);
    }

    private void doGenerate(Long docId) {
        LegalDocument doc = legalDocumentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException("文书不存在"));

        try {
            String typePrompt = DOC_TYPE_PROMPTS.getOrDefault(doc.getDocType(), DOC_TYPE_PROMPTS.get("OTHER"));

            // RAG检索相关法律知识
            String ragContext = ragService.retrieveAndBuildContextEnhanced(
                    doc.getFacts() + " " + (doc.getDomain() != null ? doc.getDomain() : ""),
                    null, doc.getDomain(), 5);

            String userMessage = String.format(
                    "%s\n\n案件事实：\n%s\n\n%s",
                    typePrompt,
                    doc.getFacts(),
                    doc.getClaims() != null && !doc.getClaims().isBlank()
                            ? "请求/主张：\n" + doc.getClaims() : "");

            String model = modelRouterService.getModelForTask(ModelRouterService.TaskType.DEEP_REASONING);
            doc.setModel(model);

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", "你是一位资深法律文书撰写专家。请严格按照指定格式生成法律文书，确保法条引用准确、逻辑严密、用语规范。\n\n相关法律知识：\n" + ragContext),
                    Map.of("role", "user", "content", userMessage)
            );

            String response = zhipuApiClient.chat(model, messages, 0.6, 8192);

            doc.setContent(response);
            doc.setStatus("COMPLETED");
            legalDocumentRepository.save(doc);
            log.info("Legal document generated: id={} type={} model={}", docId, doc.getDocType(), model);

        } catch (Exception e) {
            log.error("Legal document generation failed for {}: {}", docId, e.getMessage());
            doc.setStatus("FAILED");
            legalDocumentRepository.save(doc);
        }
    }

    /**
     * 获取文书详情
     */
    public LegalDocumentResponse getDocument(Long userId, Long docId) {
        LegalDocument doc = legalDocumentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException("文书不存在"));
        if (!doc.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该文书");
        }
        return LegalDocumentResponse.from(doc);
    }

    /**
     * 获取用户文书列表
     */
    public List<LegalDocumentResponse> listByUser(Long userId) {
        return legalDocumentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(LegalDocumentResponse::summaryFrom)
                .toList();
    }

    /**
     * 按类型查询用户文书
     */
    public List<LegalDocumentResponse> listByUserAndType(Long userId, String docType) {
        return legalDocumentRepository.findByUserIdAndDocType(userId, docType)
                .stream()
                .map(LegalDocumentResponse::summaryFrom)
                .toList();
    }
}
