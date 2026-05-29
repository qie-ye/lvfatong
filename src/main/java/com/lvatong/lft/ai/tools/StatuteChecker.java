package com.lvatong.lft.ai.tools;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 诉讼时效检查工具
 *
 * 检查各类法律纠纷的诉讼时效：
 * - 普通诉讼时效：3年（《民法典》第188条）
 * - 特殊诉讼时效：1年（身体伤害赔偿等）
 * - 最长诉讼时效：20年
 * - 劳动仲裁时效：1年（《劳动争议调解仲裁法》第27条）
 */
@Slf4j
@Component
public class StatuteChecker {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 检查诉讼时效
     *
     * @param params JSON格式参数
     * @return 时效检查结果
     */
    public String check(String params) {
        try {
            JSONObject json = JSON.parseObject(params);
            String caseType = json.getString("caseType");
            String incidentDate = json.getString("incidentDate");
            String discoveryDate = json.getString("discoveryDate"); // 知道或应当知道权利被侵害之日

            return switch (caseType) {
                case "civil" -> checkCivilStatute(json);
                case "labor" -> checkLaborStatute(json);
                case "contract" -> checkContractStatute(json);
                case "tort" -> checkTortStatute(json);
                case "product_liability" -> checkProductLiabilityStatute(json);
                case "environmental" -> checkEnvironmentalStatute(json);
                default -> "不支持的案件类型: " + caseType;
            };
        } catch (Exception e) {
            log.error("Statute check failed: {}", e.getMessage());
            return "时效检查失败: " + e.getMessage();
        }
    }

    /**
     * 检查普通民事诉讼时效
     * 《民法典》第188条：3年
     */
    private String checkCivilStatute(JSONObject params) {
        String incidentDate = params.getString("incidentDate");
        String currentDate = params.getString("currentDate");

        LocalDate incident = LocalDate.parse(incidentDate, DATE_FORMAT);
        LocalDate current = currentDate != null ? LocalDate.parse(currentDate, DATE_FORMAT) : LocalDate.now();

        // 普通诉讼时效3年
        LocalDate deadline = incident.plusYears(3);
        long daysRemaining = ChronoUnit.DAYS.between(current, deadline);
        boolean expired = daysRemaining < 0;

        Map<String, Object> result = new HashMap<>();
        result.put("caseType", "普通民事诉讼");
        result.put("statutePeriod", "3年");
        result.put("incidentDate", incidentDate);
        result.put("deadline", deadline.format(DATE_FORMAT));
        result.put("daysRemaining", daysRemaining);
        result.put("expired", expired);
        result.put("legalBasis", "《民法典》第188条");
        result.put("explanation", expired
                ? String.format("诉讼时效已于%s届满，已过期%d天", deadline.format(DATE_FORMAT), Math.abs(daysRemaining))
                : String.format("诉讼时效至%s届满，还剩%d天", deadline.format(DATE_FORMAT), daysRemaining));

        // 特殊情况：时效中止、中断
        if (params.containsKey("interruptionReason")) {
            result.put("interruptionNote", "存在时效中断事由，时效重新计算");
        }

        return JSON.toJSONString(result);
    }

    /**
     * 检查劳动仲裁时效
     * 《劳动争议调解仲裁法》第27条：1年
     */
    private String checkLaborStatute(JSONObject params) {
        String incidentDate = params.getString("incidentDate");
        String currentDate = params.getString("currentDate");

        LocalDate incident = LocalDate.parse(incidentDate, DATE_FORMAT);
        LocalDate current = currentDate != null ? LocalDate.parse(currentDate, DATE_FORMAT) : LocalDate.now();

        // 劳动仲裁时效1年
        LocalDate deadline = incident.plusYears(1);
        long daysRemaining = ChronoUnit.DAYS.between(current, deadline);
        boolean expired = daysRemaining < 0;

        Map<String, Object> result = new HashMap<>();
        result.put("caseType", "劳动争议仲裁");
        result.put("statutePeriod", "1年");
        result.put("incidentDate", incidentDate);
        result.put("deadline", deadline.format(DATE_FORMAT));
        result.put("daysRemaining", daysRemaining);
        result.put("expired", expired);
        result.put("legalBasis", "《劳动争议调解仲裁法》第27条");
        result.put("explanation", expired
                ? String.format("劳动仲裁时效已于%s届满，已过期%d天", deadline.format(DATE_FORMAT), Math.abs(daysRemaining))
                : String.format("劳动仲裁时效至%s届满，还剩%d天", deadline.format(DATE_FORMAT), daysRemaining));
        result.put("note", "劳动关系存续期间因拖欠劳动报酬发生争议的，不受1年仲裁时效限制");

        return JSON.toJSONString(result);
    }

    /**
     * 检查合同纠纷诉讼时效
     * 《民法典》第188条：3年
     */
    private String checkContractStatute(JSONObject params) {
        String breachDate = params.getString("breachDate"); // 违约之日
        String currentDate = params.getString("currentDate");

        LocalDate breach = LocalDate.parse(breachDate, DATE_FORMAT);
        LocalDate current = currentDate != null ? LocalDate.parse(currentDate, DATE_FORMAT) : LocalDate.now();

        LocalDate deadline = breach.plusYears(3);
        long daysRemaining = ChronoUnit.DAYS.between(current, deadline);
        boolean expired = daysRemaining < 0;

        Map<String, Object> result = new HashMap<>();
        result.put("caseType", "合同纠纷");
        result.put("statutePeriod", "3年");
        result.put("breachDate", breachDate);
        result.put("deadline", deadline.format(DATE_FORMAT));
        result.put("daysRemaining", daysRemaining);
        result.put("expired", expired);
        result.put("legalBasis", "《民法典》第188条、第577条");
        result.put("explanation", expired
                ? String.format("合同违约诉讼时效已于%s届满", deadline.format(DATE_FORMAT))
                : String.format("合同违约诉讼时效至%s届满，还剩%d天", deadline.format(DATE_FORMAT), daysRemaining));

        return JSON.toJSONString(result);
    }

    /**
     * 检查侵权诉讼时效
     * 《民法典》第188条：3年
     */
    private String checkTortStatute(JSONObject params) {
        String injuryDate = params.getString("injuryDate");
        String discoveryDate = params.getString("discoveryDate"); // 发现损害之日
        String currentDate = params.getString("currentDate");

        LocalDate injury = LocalDate.parse(injuryDate, DATE_FORMAT);
        LocalDate discovery = discoveryDate != null ? LocalDate.parse(discoveryDate, DATE_FORMAT) : injury;
        LocalDate current = currentDate != null ? LocalDate.parse(currentDate, DATE_FORMAT) : LocalDate.now();

        // 从知道或应当知道权利被侵害之日起计算
        LocalDate deadline = discovery.plusYears(3);
        // 最长不超过损害发生之日起20年
        LocalDate maxDeadline = injury.plusYears(20);

        if (deadline.isAfter(maxDeadline)) {
            deadline = maxDeadline;
        }

        long daysRemaining = ChronoUnit.DAYS.between(current, deadline);
        boolean expired = daysRemaining < 0;

        Map<String, Object> result = new HashMap<>();
        result.put("caseType", "侵权纠纷");
        result.put("statutePeriod", "3年（最长20年）");
        result.put("injuryDate", injuryDate);
        result.put("discoveryDate", discoveryDate);
        result.put("deadline", deadline.format(DATE_FORMAT));
        result.put("daysRemaining", daysRemaining);
        result.put("expired", expired);
        result.put("legalBasis", "《民法典》第188条、第191条");
        result.put("explanation", expired
                ? String.format("侵权诉讼时效已于%s届满", deadline.format(DATE_FORMAT))
                : String.format("侵权诉讼时效至%s届满，还剩%d天", deadline.format(DATE_FORMAT), daysRemaining));

        return JSON.toJSONString(result);
    }

    /**
     * 检查产品责任诉讼时效
     * 《产品质量法》第45条：3年，最长10年
     */
    private String checkProductLiabilityStatute(JSONObject params) {
        String injuryDate = params.getString("injuryDate");
        String currentDate = params.getString("currentDate");

        LocalDate injury = LocalDate.parse(injuryDate, DATE_FORMAT);
        LocalDate current = currentDate != null ? LocalDate.parse(currentDate, DATE_FORMAT) : LocalDate.now();

        LocalDate deadline = injury.plusYears(3);
        LocalDate maxDeadline = injury.plusYears(10);

        if (deadline.isAfter(maxDeadline)) {
            deadline = maxDeadline;
        }

        long daysRemaining = ChronoUnit.DAYS.between(current, deadline);
        boolean expired = daysRemaining < 0;

        Map<String, Object> result = new HashMap<>();
        result.put("caseType", "产品责任");
        result.put("statutePeriod", "3年（最长10年）");
        result.put("injuryDate", injuryDate);
        result.put("deadline", deadline.format(DATE_FORMAT));
        result.put("daysRemaining", daysRemaining);
        result.put("expired", expired);
        result.put("legalBasis", "《产品质量法》第45条");
        result.put("explanation", expired
                ? String.format("产品责任诉讼时效已于%s届满", deadline.format(DATE_FORMAT))
                : String.format("产品责任诉讼时效至%s届满，还剩%d天", deadline.format(DATE_FORMAT), daysRemaining));

        return JSON.toJSONString(result);
    }

    /**
     * 检查环境侵权诉讼时效
     * 《环境保护法》第66条：3年
     */
    private String checkEnvironmentalStatute(JSONObject params) {
        String pollutionDate = params.getString("pollutionDate");
        String discoveryDate = params.getString("discoveryDate");
        String currentDate = params.getString("currentDate");

        LocalDate pollution = LocalDate.parse(pollutionDate, DATE_FORMAT);
        LocalDate discovery = discoveryDate != null ? LocalDate.parse(discoveryDate, DATE_FORMAT) : pollution;
        LocalDate current = currentDate != null ? LocalDate.parse(currentDate, DATE_FORMAT) : LocalDate.now();

        LocalDate deadline = discovery.plusYears(3);
        long daysRemaining = ChronoUnit.DAYS.between(current, deadline);
        boolean expired = daysRemaining < 0;

        Map<String, Object> result = new HashMap<>();
        result.put("caseType", "环境侵权");
        result.put("statutePeriod", "3年");
        result.put("pollutionDate", pollutionDate);
        result.put("deadline", deadline.format(DATE_FORMAT));
        result.put("daysRemaining", daysRemaining);
        result.put("expired", expired);
        result.put("legalBasis", "《环境保护法》第66条");
        result.put("explanation", expired
                ? String.format("环境侵权诉讼时效已于%s届满", deadline.format(DATE_FORMAT))
                : String.format("环境侵权诉讼时效至%s届满，还剩%d天", deadline.format(DATE_FORMAT), daysRemaining));

        return JSON.toJSONString(result);
    }
}
