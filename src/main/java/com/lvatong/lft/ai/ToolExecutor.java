package com.lvatong.lft.ai;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lvatong.lft.ai.tools.CompensationCalculator;
import com.lvatong.lft.ai.tools.StatuteChecker;
import com.lvatong.lft.knowledge.FaqService;
import com.lvatong.lft.model.entity.FaqEntry;
import com.lvatong.lft.rag.HybridSearchService;
import com.lvatong.lft.repository.ContractDocumentRepository;
import com.lvatong.lft.service.CaseService;
import com.lvatong.lft.model.dto.LawyerProfileResponse;
import com.lvatong.lft.service.LawyerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Function Calling 工具执行器
 * 将 LLM 的 tool_call 分发到对应的 Service 方法，并格式化结果为文本
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolExecutor {

    private final HybridSearchService hybridSearchService;
    private final CaseService caseService;
    private final FaqService faqService;
    private final LawyerService lawyerService;
    @Lazy
    private final ContractDocumentRepository contractDocumentRepository;
    private final CompensationCalculator compensationCalculator;
    private final StatuteChecker statuteChecker;

    /**
     * 执行工具调用，返回格式化的文本结果
     *
     * @param toolName  工具名称
     * @param arguments JSON 格式的参数字符串
     * @return 工具执行结果（文本）
     */
    public String execute(String toolName, String arguments) {
        log.info("Executing tool: {} with args: {}", toolName, arguments);
        try {
            JSONObject args = JSON.parseObject(arguments);
            return switch (toolName) {
                case "search_law"       -> searchLaw(args);
                case "search_case"      -> searchCase(args);
                case "faq_lookup"       -> faqLookup(args);
                case "search_lawyer"    -> searchLawyer(args);
                case "analyze_contract" -> analyzeContract(args);
                case "calculate_compensation" -> calculateCompensation(args);
                case "check_statute_of_limitations" -> checkStatute(args);
                default -> "【工具 " + toolName + " 未实现】";
            };
        } catch (Exception e) {
            log.warn("Tool {} execution failed: {}", toolName, e.getMessage());
            return "【工具执行失败：" + e.getMessage() + "】";
        }
    }

    private String searchLaw(JSONObject args) {
        String query = args.getString("query");
        String docType = args.getString("docType");
        String lawDomain = args.getString("lawDomain");
        List<HybridSearchService.SearchResult> results = hybridSearchService.search(query, docType, lawDomain, 5);
        if (results.isEmpty()) {
            return "未找到相关法律条文。";
        }
        StringBuilder sb = new StringBuilder("【法律条文检索结果】\n");
        for (int i = 0; i < results.size(); i++) {
            HybridSearchService.SearchResult r = results.get(i);
            sb.append(i + 1).append(". ").append(r.docType()).append("\n")
              .append(r.content()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private String searchCase(JSONObject args) {
        String query = args.getString("query");
        String domain = args.getString("domain");
        int topK = args.containsKey("topK") ? Math.min(args.getIntValue("topK"), 5) : 3;
        List<HybridSearchService.SearchResult> results = caseService.semanticSearch(query, domain, topK);
        if (results.isEmpty()) {
            return "未找到相似案例。";
        }
        StringBuilder sb = new StringBuilder("【相似案例检索结果】\n");
        for (int i = 0; i < results.size(); i++) {
            HybridSearchService.SearchResult r = results.get(i);
            sb.append(i + 1).append(". 案例（").append(r.docType()).append("）\n")
              .append(r.content()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private String faqLookup(JSONObject args) {
        String query = args.getString("query");
        FaqEntry matched = faqService.matchFaq(query);
        if (matched != null) {
            return "【FAQ匹配结果】\n问：" + matched.getQuestion() + "\n答：" + matched.getAnswer();
        }
        List<FaqEntry> list = faqService.search(query, 3);
        if (list.isEmpty()) {
            return "未找到相关FAQ。";
        }
        StringBuilder sb = new StringBuilder("【FAQ搜索结果】\n");
        for (int i = 0; i < list.size(); i++) {
            FaqEntry e = list.get(i);
            sb.append(i + 1).append(". 问：").append(e.getQuestion())
              .append("\n   答：").append(e.getAnswer()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private String searchLawyer(JSONObject args) {
        String specialty = args.getString("specialty");
        String question = args.getString("question");
        try {
            List<LawyerProfileResponse> lawyers = question != null && !question.isBlank()
                    ? lawyerService.recommendLawyers(question, 3)
                    : lawyerService.searchBySpecialty(specialty, 0, 3).getContent();
            if (lawyers.isEmpty()) {
                return "暂未找到匹配的律师，建议直接联系平台客服。";
            }
            StringBuilder sb = new StringBuilder("【推荐律师列表】\n");
            for (int i = 0; i < lawyers.size(); i++) {
                LawyerProfileResponse lp = lawyers.get(i);
                sb.append(i + 1).append(". ")
                  .append(lp.getRealName() != null ? lp.getRealName() : "(未填写姓名)")
                  .append(" | ").append(lp.getLawFirm() != null ? lp.getLawFirm() : "")
                  .append(" | 专长：").append(lp.getSpecialties() != null ? String.join("/", lp.getSpecialties()) : specialty)
                  .append(" | 评分：").append(lp.getRating() != null ? lp.getRating() : "--")
                  .append(lp.getAvailable() != null && !lp.getAvailable() ? " [暂不可预约]" : "")
                  .append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("search_lawyer failed: {}", e.getMessage());
            return "律师搜索暂时不可用，请稍后再试。";
        }
    }

    private String analyzeContract(JSONObject args) {
        Long contractId = args.getLong("contractId");
        String question = args.getString("question");
        try {
            var doc = contractDocumentRepository.findById(contractId);
            if (doc.isEmpty()) {
                return "未找到ID为" + contractId + "的合同，请确认合同已上传。";
            }
            String analysisJson = doc.get().getAnalysisResult();
            if (analysisJson == null || analysisJson.isBlank()) {
                return "合同" + contractId + "尚未完成分析，请稍后查询。";
            }
            String summary = analysisJson.length() > 800 ? analysisJson.substring(0, 800) + "..." : analysisJson;
            return "【合同分析结果】（ID=" + contractId + "）\n" + summary
                    + (question != null ? "\n\n（关于\"" + question + "\"的具体内容请参考上述分析）" : "");
        } catch (Exception e) {
            log.warn("analyze_contract failed: {}", e.getMessage());
            return "合同分析查询失败：" + e.getMessage();
        }
    }

    private String calculateCompensation(JSONObject args) {
        try {
            return compensationCalculator.calculate(args.toJSONString());
        } catch (Exception e) {
            log.warn("calculate_compensation failed: {}", e.getMessage());
            return "赔偿计算失败：" + e.getMessage();
        }
    }

    private String checkStatute(JSONObject args) {
        try {
            return statuteChecker.check(args.toJSONString());
        } catch (Exception e) {
            log.warn("check_statute_of_limitations failed: {}", e.getMessage());
            return "时效检查失败：" + e.getMessage();
        }
    }
}
