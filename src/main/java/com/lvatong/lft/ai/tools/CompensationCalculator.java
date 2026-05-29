package com.lvatong.lft.ai.tools;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 赔偿计算工具
 *
 * 支持计算各类法律赔偿金额：
 * - 劳动纠纷：经济补偿金、赔偿金、加班费
 * - 工伤赔偿：一次性伤残补助金、医疗补助金
 * - 交通事故：医疗费、误工费、残疾赔偿金
 */
@Slf4j
@Component
public class CompensationCalculator {

    /**
     * 计算赔偿金额
     *
     * @param params JSON格式参数
     * @return 计算结果
     */
    public String calculate(String params) {
        try {
            JSONObject json = JSON.parseObject(params);
            String type = json.getString("type");

            return switch (type) {
                case "labor_compensation" -> calculateLaborCompensation(json);
                case "labor_penalty" -> calculateLaborPenalty(json);
                case "work_injury" -> calculateWorkInjury(json);
                case "traffic_accident" -> calculateTrafficAccident(json);
                case "overtime" -> calculateOvertime(json);
                default -> "不支持的赔偿类型: " + type;
            };
        } catch (Exception e) {
            log.error("Compensation calculation failed: {}", e.getMessage());
            return "计算失败: " + e.getMessage();
        }
    }

    /**
     * 计算劳动经济补偿金
     * 《劳动合同法》第47条：每满一年支付一个月工资
     */
    private String calculateLaborCompensation(JSONObject params) {
        double monthlySalary = params.getDoubleValue("monthlySalary");
        int workYears = params.getIntValue("workYears");
        int extraMonths = params.getIntValue("extraMonths");

        // 经济补偿 = 月工资 × 工作年限（6个月以上不满1年按1年算，不满6个月支付半个月）
        double compensation;
        if (extraMonths >= 6) {
            compensation = monthlySalary * (workYears + 1);
        } else {
            compensation = monthlySalary * workYears + monthlySalary * 0.5;
        }

        // 高收入限制：月工资高于当地职工平均工资3倍的，按3倍支付，最高不超过12年
        // 此处简化处理

        Map<String, Object> result = new HashMap<>();
        result.put("type", "劳动经济补偿金");
        result.put("monthlySalary", monthlySalary);
        result.put("workYears", workYears);
        result.put("extraMonths", extraMonths);
        result.put("compensation", compensation);
        result.put("formula", "月工资 × 工作年限");
        result.put("legalBasis", "《劳动合同法》第47条");
        result.put("explanation", String.format(
                "工作%d年%d个月，月工资%.0f元，经济补偿金为%.0f元",
                workYears, extraMonths, monthlySalary, compensation));

        return JSON.toJSONString(result);
    }

    /**
     * 计算违法解除赔偿金
     * 《劳动合同法》第87条：经济补偿标准的二倍
     */
    private String calculateLaborPenalty(JSONObject params) {
        double monthlySalary = params.getDoubleValue("monthlySalary");
        int workYears = params.getIntValue("workYears");

        // 赔偿金 = 经济补偿 × 2
        double compensation = monthlySalary * workYears;
        double penalty = compensation * 2;

        Map<String, Object> result = new HashMap<>();
        result.put("type", "违法解除赔偿金");
        result.put("monthlySalary", monthlySalary);
        result.put("workYears", workYears);
        result.put("compensation", compensation);
        result.put("penalty", penalty);
        result.put("formula", "经济补偿金 × 2");
        result.put("legalBasis", "《劳动合同法》第87条");
        result.put("explanation", String.format(
                "工作%d年，月工资%.0f元，违法解除赔偿金为%.0f元（经济补偿%.0f元的2倍）",
                workYears, monthlySalary, penalty, compensation));

        return JSON.toJSONString(result);
    }

    /**
     * 计算工伤赔偿
     * 《工伤保险条例》相关条款
     */
    private String calculateWorkInjury(JSONObject params) {
        double monthlySalary = params.getDoubleValue("monthlySalary");
        int disabilityLevel = params.getIntValue("disabilityLevel"); // 1-10级
        double medicalExpenses = params.getDoubleValue("medicalExpenses");

        // 一次性伤残补助金（月工资 × 月数）
        int disabilityMonths = getDisabilityMonths(disabilityLevel);
        double disabilityAllowance = monthlySalary * disabilityMonths;

        // 一次性工伤医疗补助金和就业补助金（各地标准不同，此处估算）
        double medicalAllowance = monthlySalary * getMedicalAllowanceMonths(disabilityLevel);
        double employmentAllowance = monthlySalary * getEmploymentAllowanceMonths(disabilityLevel);

        double total = medicalExpenses + disabilityAllowance + medicalAllowance + employmentAllowance;

        Map<String, Object> result = new HashMap<>();
        result.put("type", "工伤赔偿");
        result.put("disabilityLevel", disabilityLevel);
        result.put("monthlySalary", monthlySalary);
        result.put("medicalExpenses", medicalExpenses);
        result.put("disabilityAllowance", disabilityAllowance);
        result.put("disabilityAllowanceMonths", disabilityMonths);
        result.put("medicalAllowance", medicalAllowance);
        result.put("employmentAllowance", employmentAllowance);
        result.put("total", total);
        result.put("legalBasis", "《工伤保险条例》第35-37条");
        result.put("explanation", String.format(
                "%d级伤残，月工资%.0f元，工伤赔偿总计%.0f元",
                disabilityLevel, monthlySalary, total));

        return JSON.toJSONString(result);
    }

    /**
     * 计算交通事故赔偿
     */
    private String calculateTrafficAccident(JSONObject params) {
        double medicalExpenses = params.getDoubleValue("medicalExpenses");
        double lostIncome = params.getDoubleValue("lostIncome"); // 误工费
        double nursingFee = params.getDoubleValue("nursingFee"); // 护理费
        double transportFee = params.getDoubleValue("transportFee");
        double hospitalFoodAllowance = params.getDoubleValue("hospitalFoodAllowance");
        int disabilityLevel = params.getIntValue("disabilityLevel");
        double annualIncome = params.getDoubleValue("annualIncome");
        int compensationYears = params.getIntValue("compensationYears");

        // 残疾赔偿金 = 年收入 × 赔偿年限 × 伤残系数
        double disabilityCoefficient = getDisabilityCoefficient(disabilityLevel);
        double disabilityCompensation = annualIncome * compensationYears * disabilityCoefficient;

        double total = medicalExpenses + lostIncome + nursingFee + transportFee
                + hospitalFoodAllowance + disabilityCompensation;

        Map<String, Object> result = new HashMap<>();
        result.put("type", "交通事故赔偿");
        result.put("medicalExpenses", medicalExpenses);
        result.put("lostIncome", lostIncome);
        result.put("nursingFee", nursingFee);
        result.put("disabilityCompensation", disabilityCompensation);
        result.put("total", total);
        result.put("legalBasis", "《民法典》第1179条、《最高人民法院关于审理人身损害赔偿案件适用法律若干问题的解释》");
        result.put("explanation", String.format(
                "交通事故赔偿总计%.0f元，其中残疾赔偿金%.0f元",
                total, disabilityCompensation));

        return JSON.toJSONString(result);
    }

    /**
     * 计算加班费
     * 《劳动法》第44条
     */
    private String calculateOvertime(JSONObject params) {
        double hourlyWage = params.getDoubleValue("hourlyWage");
        int weekdayHours = params.getIntValue("weekdayHours"); // 工作日加班
        int weekendHours = params.getIntValue("weekendHours"); // 周末加班
        int holidayHours = params.getIntValue("holidayHours"); // 法定节假日加班

        // 工作日加班：1.5倍
        double weekdayOvertime = hourlyWage * 1.5 * weekdayHours;
        // 周末加班：2倍
        double weekendOvertime = hourlyWage * 2 * weekendHours;
        // 法定节假日：3倍
        double holidayOvertime = hourlyWage * 3 * holidayHours;

        double total = weekdayOvertime + weekendOvertime + holidayOvertime;

        Map<String, Object> result = new HashMap<>();
        result.put("type", "加班费");
        result.put("hourlyWage", hourlyWage);
        result.put("weekdayOvertime", weekdayOvertime);
        result.put("weekendOvertime", weekendOvertime);
        result.put("holidayOvertime", holidayOvertime);
        result.put("total", total);
        result.put("legalBasis", "《劳动法》第44条");
        result.put("explanation", String.format(
                "工作日加班%d小时(%.0f元)、周末加班%d小时(%.0f元)、节假日加班%d小时(%.0f元)，加班费合计%.0f元",
                weekdayHours, weekdayOvertime, weekendHours, weekendOvertime,
                holidayHours, holidayOvertime, total));

        return JSON.toJSONString(result);
    }

    // 辅助方法

    private int getDisabilityMonths(int level) {
        return switch (level) {
            case 1 -> 27;
            case 2 -> 25;
            case 3 -> 23;
            case 4 -> 21;
            case 5 -> 18;
            case 6 -> 16;
            case 7 -> 13;
            case 8 -> 11;
            case 9 -> 9;
            case 10 -> 7;
            default -> 7;
        };
    }

    private int getMedicalAllowanceMonths(int level) {
        return switch (level) {
            case 5 -> 18;
            case 6 -> 15;
            case 7 -> 12;
            case 8 -> 9;
            case 9 -> 6;
            case 10 -> 3;
            default -> 0;
        };
    }

    private int getEmploymentAllowanceMonths(int level) {
        return switch (level) {
            case 5 -> 36;
            case 6 -> 30;
            case 7 -> 20;
            case 8 -> 15;
            case 9 -> 10;
            case 10 -> 5;
            default -> 0;
        };
    }

    private double getDisabilityCoefficient(int level) {
        return switch (level) {
            case 1 -> 1.0;
            case 2 -> 0.9;
            case 3 -> 0.8;
            case 4 -> 0.7;
            case 5 -> 0.6;
            case 6 -> 0.5;
            case 7 -> 0.4;
            case 8 -> 0.3;
            case 9 -> 0.2;
            case 10 -> 0.1;
            default -> 0.0;
        };
    }
}
