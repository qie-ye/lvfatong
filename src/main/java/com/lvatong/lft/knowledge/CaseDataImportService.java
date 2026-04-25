package com.lvatong.lft.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lvatong.lft.model.entity.LegalCase;
import com.lvatong.lft.repository.LegalCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * 案例数据批量导入服务
 *
 * 支持两种格式：
 * 1. CAIL 格式（中国法研杯竞赛数据集，每行一个 JSON）
 *    格式参考：https://github.com/china-ai-law-challenge/CAIL2019
 *    字段：fact / meta.accusation / meta.relevant_articles / meta.crime_province / meta.criminals
 *
 * 2. 律法通标准格式（JSON Array）
 *    字段：title / caseNo / caseType / court / year / domain / keywords / province
 *          summary / facts / ruling / analysis
 *
 * 数据来源建议：
 * - CAIL2019 数据集（刑事案件，183,000+条）：https://github.com/china-ai-law-challenge/CAIL2019
 * - 裁判文书网公开数据包（需在官网申请下载）：https://wenshu.court.gov.cn/
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseDataImportService {

    private final LegalCaseRepository legalCaseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 自动识别格式并导入案例文件
     *
     * @param file      JSON 文件（CAIL 每行JSON 或 标准数组JSON）
     * @param caseType  案件类型（刑事/民事/行政），CAIL格式固定为"刑事"
     * @param maxCount  最多导入条数
     * @return 实际导入数量
     */
    @Transactional
    public int importFromFile(MultipartFile file, String caseType, int maxCount) throws Exception {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        log.info("开始导入案例文件：{} caseType={} maxCount={}", filename, caseType, maxCount);

        String content = new String(file.getBytes(), StandardCharsets.UTF_8).trim();

        if (content.startsWith("[")) {
            return importStandardFormat(content, maxCount);
        } else {
            return importCailFormat(file, caseType, maxCount);
        }
    }

    // ──────────────────────────────────────────────────────────
    // CAIL 格式（每行一个JSON，刑事案件）
    // ──────────────────────────────────────────────────────────

    private int importCailFormat(MultipartFile file, String caseType, int maxCount) throws Exception {
        int imported = 0;
        List<LegalCase> batch = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && imported < maxCount) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    LegalCase lc = parseCailLine(line, caseType);
                    if (lc != null) {
                        batch.add(lc);
                        imported++;
                        if (batch.size() >= 100) {
                            legalCaseRepository.saveAll(batch);
                            batch.clear();
                            log.debug("已批量保存，累计：{}", imported);
                        }
                    }
                } catch (Exception e) {
                    log.warn("跳过无效行：{}", e.getMessage());
                }
            }
        }

        if (!batch.isEmpty()) legalCaseRepository.saveAll(batch);
        log.info("CAIL格式导入完成，共 {} 条", imported);
        return imported;
    }

    private LegalCase parseCailLine(String line, String caseType) throws Exception {
        JsonNode node = objectMapper.readTree(line);
        String fact = node.path("fact").asText("").trim();
        if (fact.isBlank()) return null;

        JsonNode meta = node.path("meta");

        StringJoiner accusation = new StringJoiner("、");
        meta.path("accusation").forEach(a -> accusation.add(a.asText()));

        StringJoiner articles = new StringJoiner("，");
        meta.path("relevant_articles").forEach(a -> {
            String law = a.path("law").asText("刑法");
            int art = a.path("article").asInt(0);
            if (art > 0) articles.add(law + "第" + art + "条");
        });

        String province = meta.path("crime_province").asText("未知");
        String city     = meta.path("crime_city").asText("");
        String term     = extractTerm(meta.path("term_of_imprisonment"));

        String title = "【" + accusation + "】" + fact.substring(0, Math.min(50, fact.length())) + "...";
        String keywords = accusation + "," + articles;

        LegalCase lc = new LegalCase();
        lc.setTitle(title);
        lc.setCaseType(caseType != null ? caseType : "刑事");
        lc.setCourt("人民法院");
        lc.setYear(String.valueOf(java.time.Year.now().getValue()));
        lc.setDomain("刑法");
        lc.setKeywords(keywords.length() > 500 ? keywords.substring(0, 500) : keywords);
        lc.setProvince(province + city);
        lc.setSummary(fact.length() > 200 ? fact.substring(0, 200) + "…" : fact);
        lc.setFacts(fact);
        lc.setRuling(term);
        lc.setAnalysis("适用法条：" + articles);
        lc.setVectorIndexed(false);
        return lc;
    }

    private String extractTerm(JsonNode term) {
        if (term == null || term.isMissingNode()) return "";
        if (term.path("death_penalty").asBoolean(false))   return "判处死刑";
        if (term.path("life_imprisonment").asBoolean(false)) return "判处无期徒刑";
        int months = term.path("imprisonment").asInt(0);
        if (months > 0) {
            int years = months / 12;
            int rem   = months % 12;
            return years > 0 ? "有期徒刑" + years + "年" + (rem > 0 ? rem + "个月" : "")
                             : "有期徒刑" + rem + "个月";
        }
        return "免于刑事处罚";
    }

    // ──────────────────────────────────────────────────────────
    // 律法通标准 JSON 数组格式
    // ──────────────────────────────────────────────────────────

    private int importStandardFormat(String content, int maxCount) throws Exception {
        JsonNode array = objectMapper.readTree(content);
        if (!array.isArray()) throw new IllegalArgumentException("标准格式应为 JSON 数组");

        int imported = 0;
        List<LegalCase> batch = new ArrayList<>();

        for (JsonNode node : array) {
            if (imported >= maxCount) break;
            try {
                LegalCase lc = parseStandardNode(node);
                batch.add(lc);
                imported++;
                if (batch.size() >= 100) {
                    legalCaseRepository.saveAll(batch);
                    batch.clear();
                }
            } catch (Exception e) {
                log.warn("跳过无效记录：{}", e.getMessage());
            }
        }

        if (!batch.isEmpty()) legalCaseRepository.saveAll(batch);
        log.info("标准格式导入完成，共 {} 条", imported);
        return imported;
    }

    private LegalCase parseStandardNode(JsonNode node) {
        LegalCase lc = new LegalCase();
        lc.setTitle(   node.path("title").asText("案例"));
        lc.setCaseNo(  node.path("caseNo").asText(null));
        lc.setCaseType(node.path("caseType").asText("民事"));
        lc.setCourt(   node.path("court").asText(null));
        lc.setYear(    node.path("year").asText(null));
        lc.setDomain(  node.path("domain").asText(null));
        lc.setKeywords(node.path("keywords").asText(null));
        lc.setProvince(node.path("province").asText(null));
        lc.setSummary( node.path("summary").asText(null));
        lc.setFacts(   node.path("facts").asText(null));
        lc.setRuling(  node.path("ruling").asText(null));
        lc.setAnalysis(node.path("analysis").asText(null));
        lc.setVectorIndexed(false);
        return lc;
    }
}
