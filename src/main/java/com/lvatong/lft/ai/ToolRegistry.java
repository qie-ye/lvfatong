package com.lvatong.lft.ai;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "query", Map.of("type", "string", "description", "检索关键词，如'合同违约金上限'"),
                                        "docType", Map.of("type", "string", "description", "文档类型，可选：LAW/REGULATION/CASE/INTERPRETATION"),
                                        "lawDomain", Map.of("type", "string", "description", "法律领域，可选：民法/刑法/劳动法/合同法/侵权法等")
                                ),
                                "required", List.of("query")
                        )),

                buildTool("search_case",
                        "检索相似法律案例。当用户描述具体纠纷或需要参考判例时使用。",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "query", Map.of("type", "string", "description", "案例检索关键词，如'劳动合同解除赔偿'"),
                                        "domain", Map.of("type", "string", "description", "法律领域，可选"),
                                        "topK", Map.of("type", "integer", "description", "返回数量，默认3，最多10")
                                ),
                                "required", List.of("query")
                        )),

                buildTool("faq_lookup",
                        "查询常见法律问题解答库。当问题属于高频法律咨询时优先使用，可快速获取标准答案。",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "query", Map.of("type", "string", "description", "查询关键词")
                                ),
                                "required", List.of("query")
                        )),

                buildTool("search_lawyer",
                        "推荐匹配律师。当用户需要专业法律服务或律师咨询时使用。",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "specialty", Map.of("type", "string", "description", "专业领域，如'婚姻家庭'/'知识产权'/'劳动纠纷'"),
                                        "question", Map.of("type", "string", "description", "用户问题，用于智能匹配")
                                ),
                                "required", List.of("specialty")
                        )),

                buildTool("analyze_contract",
                        "获取已上传合同的分析结果。当用户询问已上传合同的具体风险或条款内容时使用。",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "contractId", Map.of("type", "integer", "description", "合同文档ID"),
                                        "question", Map.of("type", "string", "description", "关于合同的具体问题")
                                ),
                                "required", List.of("contractId")
                        ))
        );
    }

    private Map<String, Object> buildTool(String name, String description, Map<String, Object> parameters) {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", parameters
                )
        );
    }
}
