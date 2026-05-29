package com.lvatong.lft.ai;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Function Calling 工具注册中心
 * 定义所有可供 LLM 自主调用的工具（ZhiPu function calling 格式）
 */
@Component
public class ToolRegistry {

    public List<Map<String, Object>> getAllTools() {
        return List.of(
                buildTool("search_law",
                        "搜索法律条文。当用户问题涉及具体法律规定、法条内容或法律依据时使用。",
                        buildParams(
                                Map.of(
                                        "query", buildProp("string", "检索关键词，如'合同违约金上限'"),
                                        "docType", buildProp("string", "文档类型，可选：LAW/REGULATION/CASE/INTERPRETATION"),
                                        "lawDomain", buildProp("string", "法律领域，可选：民法/刑法/劳动法/合同法/侵权法等")
                                ),
                                List.of("query")
                        )),

                buildTool("search_case",
                        "检索相似法律案例。当用户描述具体纠纷或需要参考判例时使用。",
                        buildParams(
                                Map.of(
                                        "query", buildProp("string", "案例检索关键词，如'劳动合同解除赔偿'"),
                                        "domain", buildProp("string", "法律领域，可选"),
                                        "topK", buildProp("integer", "返回数量，默认3，最多10")
                                ),
                                List.of("query")
                        )),

                buildTool("faq_lookup",
                        "查询常见法律问题解答库。当问题属于高频法律咨询时优先使用，可快速获取标准答案。",
                        buildParams(
                                Map.of("query", buildProp("string", "查询关键词")),
                                List.of("query")
                        )),

                buildTool("search_lawyer",
                        "推荐匹配律师。当用户需要专业法律服务或律师咨询时使用。",
                        buildParams(
                                Map.of(
                                        "specialty", buildProp("string", "专业领域，如'婚姻家庭'/'知识产权'/'劳动纠纷'"),
                                        "question", buildProp("string", "用户问题，用于智能匹配")
                                ),
                                List.of("specialty")
                        )),

                buildTool("analyze_contract",
                        "获取已上传合同的分析结果。当用户询问已上传合同的具体风险或条款内容时使用。",
                        buildParams(
                                Map.of(
                                        "contractId", buildProp("integer", "合同文档ID"),
                                        "question", buildProp("string", "关于合同的具体问题")
                                ),
                                List.of("contractId")
                        )),

                buildTool("calculate_compensation",
                        "计算各类法律赔偿金额。包括劳动经济补偿、违法解除赔偿、工伤赔偿、交通事故赔偿、加班费等。当用户询问赔偿金额计算时使用。",
                        buildCompensationParams()
                ),

                buildTool("check_statute_of_limitations",
                        "检查各类法律纠纷的诉讼时效。包括普通民事诉讼(3年)、劳动仲裁(1年)、合同纠纷、侵权纠纷等。当用户询问诉讼时效或是否过期时使用。",
                        buildStatuteParams()
                )
        );
    }

    private Map<String, Object> buildTool(String name, String description, Map<String, Object> parameters) {
        Map<String, Object> function = new HashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);

        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        tool.put("function", function);
        return tool;
    }

    private Map<String, Object> buildParams(Map<String, Object> properties, List<String> required) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", "object");
        params.put("properties", properties);
        params.put("required", required);
        return params;
    }

    private Map<String, Object> buildProp(String type, String description) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", type);
        prop.put("description", description);
        return prop;
    }

    private Map<String, Object> buildCompensationParams() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("type", buildProp("string", "赔偿类型：labor_compensation(劳动补偿)、labor_penalty(违法解除赔偿)、work_injury(工伤)、traffic_accident(交通事故)、overtime(加班费)"));
        properties.put("monthlySalary", buildProp("number", "月工资（元）"));
        properties.put("workYears", buildProp("integer", "工作年限"));
        properties.put("extraMonths", buildProp("integer", "额外月数（6个月以上按1年算）"));
        properties.put("disabilityLevel", buildProp("integer", "伤残等级（1-10级）"));
        properties.put("medicalExpenses", buildProp("number", "医疗费（元）"));
        properties.put("annualIncome", buildProp("number", "年收入（元）"));
        properties.put("compensationYears", buildProp("integer", "赔偿年限"));
        properties.put("hourlyWage", buildProp("number", "时薪（元）"));
        properties.put("weekdayHours", buildProp("integer", "工作日加班小时"));
        properties.put("weekendHours", buildProp("integer", "周末加班小时"));
        properties.put("holidayHours", buildProp("integer", "节假日加班小时"));

        return buildParams(properties, List.of("type"));
    }

    private Map<String, Object> buildStatuteParams() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("caseType", buildProp("string", "案件类型：civil(民事)、labor(劳动)、contract(合同)、tort(侵权)、product_liability(产品责任)、environmental(环境侵权)"));
        properties.put("incidentDate", buildProp("string", "事发日期（yyyy-MM-dd）"));
        properties.put("discoveryDate", buildProp("string", "发现权利被侵害日期（yyyy-MM-dd）"));
        properties.put("currentDate", buildProp("string", "当前日期（yyyy-MM-dd，默认今天）"));
        properties.put("breachDate", buildProp("string", "违约日期（合同纠纷用）"));
        properties.put("injuryDate", buildProp("string", "损害发生日期（侵权纠纷用）"));

        return buildParams(properties, List.of("caseType", "incidentDate"));
    }
}
