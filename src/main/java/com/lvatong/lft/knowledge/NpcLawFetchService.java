package com.lvatong.lft.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lvatong.lft.model.entity.KnowledgeDocument;
import com.lvatong.lft.repository.KnowledgeDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 国家法律法规数据库（flk.npc.gov.cn）抓取服务
 * API 为公开接口，无需 Token，礼貌性限速 500ms/次
 */
@Slf4j
@Service
public class NpcLawFetchService {

    private static final String BASE_URL      = "https://flk.npc.gov.cn/api/";
    private static final String DETAIL_URL    = "https://flk.npc.gov.cn/api/detail";
    private static final int    BATCH_SIZE    = 10;
    private static final long   RATE_LIMIT_MS = 600;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper  = new ObjectMapper();

    private final KnowledgeService knowledgeService;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    private static final Map<String, String> DOMAIN_KEYWORDS = Map.ofEntries(
            Map.entry("刑法",   "刑法"),
            Map.entry("民法典", "民法"),
            Map.entry("合同",   "合同法"),
            Map.entry("婚姻",   "婚姻家庭"),
            Map.entry("劳动",   "劳动法"),
            Map.entry("行政",   "行政法"),
            Map.entry("知识产权", "知识产权"),
            Map.entry("专利",   "知识产权"),
            Map.entry("著作权", "知识产权"),
            Map.entry("商标",   "知识产权"),
            Map.entry("公司",   "公司法"),
            Map.entry("证券",   "证券法"),
            Map.entry("消费者", "消费者权益"),
            Map.entry("税",     "税法"),
            Map.entry("环境",   "环境法"),
            Map.entry("土地",   "房产法"),
            Map.entry("房",     "房产法"),
            Map.entry("交通",   "交通法"),
            Map.entry("医疗",   "医疗卫生法"),
            Map.entry("食品",   "食品安全法"),
            Map.entry("宪法",   "宪法")
    );

    public NpcLawFetchService(KnowledgeService knowledgeService,
                               KnowledgeDocumentRepository knowledgeDocumentRepository) {
        this.knowledgeService = knowledgeService;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
    }

    /**
     * 抓取并导入法律法规
     *
     * @param lawType  法规类型：flfg=法律法规 sfjs=司法解释 xzfg=行政法规
     * @param maxCount 最多导入条数（建议不超过100，避免频繁请求）
     * @return 实际导入数量
     */
    public int fetchAndImport(String lawType, int maxCount) {
        log.info("开始从国家法律法规数据库抓取，type={} maxCount={}", lawType, maxCount);
        int imported = 0;
        int page = 1;

        while (imported < maxCount) {
            int size = Math.min(BATCH_SIZE, maxCount - imported);
            List<JsonNode> items = fetchList(lawType, page, size);
            if (items.isEmpty()) break;

            for (JsonNode item : items) {
                if (imported >= maxCount) break;
                try {
                    String id    = item.path("id").asText();
                    String title = item.path("title").asText();

                    if (knowledgeDocumentRepository.existsByTitle(title)) {
                        log.debug("已存在，跳过：{}", title);
                        continue;
                    }

                    String fullText = fetchDetail(id, lawType);
                    if (fullText == null || fullText.isBlank()) continue;

                    String domain  = inferDomain(title);
                    String publish = item.path("publish").asText().substring(0, 10);
                    String source  = "国家法律法规数据库 " + publish;
                    String url     = buildLawUrl(id, title);

                    knowledgeService.importLaw(title, source, domain, fullText, url);
                    log.info("已导入：{}", title);
                    imported++;

                    Thread.sleep(RATE_LIMIT_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.warn("导入失败：{} - {}", item.path("title").asText(), e.getMessage());
                }
            }

            if (items.size() < size) break;
            page++;
        }

        log.info("抓取完成，共导入 {} 条法律文档", imported);
        return imported;
    }

    /**
     * 触发所有已导入但未向量化的文档进行分块+Embedding入库
     */
    public int ingestPending() {
        return knowledgeService.ingestPendingDocuments();
    }

    // ──────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────

    private List<JsonNode> fetchList(String type, int page, int size) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .queryParam("searchType", "title,content")
                    .queryParam("sortTr",     "f_bbrq_s desc")
                    .queryParam("sort",       "true")
                    .queryParam("page",       page)
                    .queryParam("size",       size)
                    .queryParam("type",       type)
                    .queryParam("_",          System.currentTimeMillis())
                    .build(false).toUriString();

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, buildBrowserHeaders(), String.class);

            JsonNode root = objectMapper.readTree(resp.getBody());
            if (!root.path("success").asBoolean(false)) return List.of();

            JsonNode data = root.path("result").path("data");
            List<JsonNode> items = new ArrayList<>();
            data.forEach(items::add);
            return items;
        } catch (Exception e) {
            log.error("获取法规列表失败 page={}: {}", page, e.getMessage());
            return List.of();
        }
    }

    private String fetchDetail(String id, String type) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(DETAIL_URL)
                    .queryParam("id",   id)
                    .queryParam("type", type)
                    .queryParam("_",    System.currentTimeMillis())
                    .build().toUriString();

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, buildBrowserHeaders(), String.class);

            JsonNode root = objectMapper.readTree(resp.getBody());
            String body   = root.path("result").path("body").asText("");
            return stripHtml(body);
        } catch (Exception e) {
            log.error("获取法规详情失败 id={}: {}", id, e.getMessage());
            return null;
        }
    }

    private HttpEntity<Void> buildBrowserHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent",      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        headers.set("Accept",          "application/json, text/plain, */*");
        headers.set("Accept-Language", "zh-CN,zh;q=0.9");
        headers.set("Referer",         "https://flk.npc.gov.cn/");
        return new HttpEntity<>(headers);
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html
                .replaceAll("<br\\s*/?>",              "\n")
                .replaceAll("<p[^>]*>",                "\n")
                .replaceAll("<div[^>]*>",              "\n")
                .replaceAll("<[^>]+>",                 "")
                .replaceAll("&nbsp;",                  " ")
                .replaceAll("&lt;",                    "<")
                .replaceAll("&gt;",                    ">")
                .replaceAll("&amp;",                   "&")
                .replaceAll("&quot;",                  "\"")
                .replaceAll("&#\\d+;",                 "")
                .replaceAll("[ \\t]+",                 " ")
                .replaceAll("\\n{3,}",                 "\n\n")
                .trim();
    }

    /**
     * 构造国家法律法规数据库的法条永久链接
     * 格式：detail2.html 使用 Base64 编码的 ID
     */
    private String buildLawUrl(String id, String title) {
        try {
            String encoded = java.util.Base64.getEncoder().encodeToString(id.getBytes());
            return "https://flk.npc.gov.cn/detail2.html?" + encoded;
        } catch (Exception e) {
            return "https://flk.npc.gov.cn/?searchType=title&keyWord="
                    + java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private String inferDomain(String title) {
        for (Map.Entry<String, String> e : DOMAIN_KEYWORDS.entrySet()) {
            if (title.contains(e.getKey())) return e.getValue();
        }
        return "综合法律";
    }
}
