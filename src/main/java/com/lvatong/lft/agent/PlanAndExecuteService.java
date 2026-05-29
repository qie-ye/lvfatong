package com.lvatong.lft.agent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lvatong.lft.ai.ChatService;
import com.lvatong.lft.ai.ToolExecutor;
import com.lvatong.lft.ai.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Plan-and-Execute 模式
 *
 * 适用于复杂法律问题，将问题分解为多个子任务，按计划执行
 *
 * 流程：
 * 1. Planner：分析问题，制定执行计划
 * 2. Executor：逐个执行子任务
 * 3. Synthesizer：综合所有结果，生成最终答案
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanAndExecuteService {

    private final ChatService chatService;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final ReActAgent reActAgent;

    private static final String PLANNER_PROMPT = """
            你是一个法律问题规划专家。请将以下复杂法律问题分解为可执行的子任务。

            ## 用户问题
            %s

            ## 检索到的法律知识
            %s

            ## 可用工具
            %s

            ## 输出要求
            请以JSON数组格式输出子任务列表，每个子任务包含：
            - id: 唯一标识
            - description: 任务描述
            - type: 任务类型（必须是可用工具之一）
            - parameters: 任务参数（JSON对象）
            - dependencies: 依赖的任务id列表

            ## 示例
            ```json
            [
              {
                "id": "task1",
                "description": "查询劳动法关于经济补偿的规定",
                "type": "search_law",
                "parameters": {"query": "劳动法 经济补偿"},
                "dependencies": []
              },
              {
                "id": "task2",
                "description": "计算应得的经济补偿金额",
                "type": "calculate_compensation",
                "parameters": {"salary": "10000", "years": "5"},
                "dependencies": ["task1"]
              }
            ]
            ```

            请直接输出JSON数组，不要其他解释。
            """;

    private static final String SYNTHESIZER_PROMPT = """
            你是一个法律分析专家。请根据以下子任务的执行结果，综合分析并回答用户的问题。

            ## 用户问题
            %s

            ## 子任务执行结果
            %s

            ## 要求
            1. 综合所有子任务结果，给出完整的法律分析
            2. 引用具体的法条和案例
            3. 给出明确的结论和建议
            4. 末尾添加免责声明
            """;

    /**
     * 执行 Plan-and-Execute 流程
     */
    public PlanExecuteResult execute(String question, String context) {
        log.info("Plan-and-Execute started for question: {}", truncate(question, 50));

        // Step 1: 规划
        List<SubTask> plan = createPlan(question, context);
        log.info("Plan created with {} subtasks", plan.size());

        // Step 2: 执行
        List<SubTaskResult> results = executePlan(plan);
        log.info("Plan executed, {} results", results.size());

        // Step 3: 综合
        String finalAnswer = synthesize(question, results);
        log.info("Plan-and-Execute completed");

        return PlanExecuteResult.builder()
                .question(question)
                .plan(plan)
                .results(results)
                .finalAnswer(finalAnswer)
                .build();
    }

    /**
     * 创建执行计划
     */
    private List<SubTask> createPlan(String question, String context) {
        String toolsDescription = buildToolsDescription();
        String prompt = String.format(PLANNER_PROMPT, question,
                context != null ? context : "无", toolsDescription);

        try {
            String response = chatService.simpleChat(prompt, "glm-4-plus", 0.3, 2048);
            return parsePlan(response);
        } catch (Exception e) {
            log.warn("Plan creation failed, using simple plan: {}", e.getMessage());
            return createSimplePlan(question);
        }
    }

    /**
     * 解析计划
     */
    private List<SubTask> parsePlan(String response) {
        List<SubTask> tasks = new ArrayList<>();

        try {
            // 提取JSON数组
            String jsonStr = extractJsonArray(response);
            JSONArray array = JSON.parseArray(jsonStr);

            for (int i = 0; i < array.size(); i++) {
                JSONObject obj = array.getJSONObject(i);
                SubTask task = SubTask.builder()
                        .id(obj.getString("id"))
                        .description(obj.getString("description"))
                        .type(SubTask.TaskType.valueOf(obj.getString("type").toUpperCase()))
                        .parameters(obj.getJSONObject("parameters"))
                        .dependencies(parseStringList(obj.getJSONArray("dependencies")))
                        .status(SubTask.TaskStatus.PENDING)
                        .build();
                tasks.add(task);
            }
        } catch (Exception e) {
            log.warn("Failed to parse plan, using simple plan: {}", e.getMessage());
            return createSimplePlan(null);
        }

        return tasks;
    }

    /**
     * 创建简单计划（降级方案）
     */
    private List<SubTask> createSimplePlan(String question) {
        List<SubTask> tasks = new ArrayList<>();

        tasks.add(SubTask.builder()
                .id("task1")
                .description("搜索相关法律条文")
                .type(SubTask.TaskType.SEARCH_LAW)
                .parameters(Map.of("query", question != null ? question : "法律咨询"))
                .dependencies(List.of())
                .status(SubTask.TaskStatus.PENDING)
                .build());

        tasks.add(SubTask.builder()
                .id("task2")
                .description("搜索相关案例")
                .type(SubTask.TaskType.SEARCH_CASE)
                .parameters(Map.of("query", question != null ? question : "法律案例"))
                .dependencies(List.of())
                .status(SubTask.TaskStatus.PENDING)
                .build());

        return tasks;
    }

    /**
     * 执行计划
     */
    private List<SubTaskResult> executePlan(List<SubTask> plan) {
        List<SubTaskResult> results = new ArrayList<>();

        for (SubTask task : plan) {
            // 检查依赖是否完成
            if (!areDependenciesCompleted(task, results)) {
                log.warn("Skipping task {} due to unmet dependencies", task.getId());
                continue;
            }

            task.setStatus(SubTask.TaskStatus.IN_PROGRESS);

            try {
                String result = executeTask(task);
                task.setResult(result);
                task.setStatus(SubTask.TaskStatus.COMPLETED);

                results.add(SubTaskResult.builder()
                        .taskId(task.getId())
                        .description(task.getDescription())
                        .result(result)
                        .success(true)
                        .build());

                log.debug("Task {} completed: {}", task.getId(), truncate(result, 100));

            } catch (Exception e) {
                log.error("Task {} failed: {}", task.getId(), e.getMessage());
                task.setStatus(SubTask.TaskStatus.FAILED);

                results.add(SubTaskResult.builder()
                        .taskId(task.getId())
                        .description(task.getDescription())
                        .result("执行失败: " + e.getMessage())
                        .success(false)
                        .build());
            }
        }

        return results;
    }

    /**
     * 执行单个任务
     */
    private String executeTask(SubTask task) {
        // 如果是综合任务，使用ReAct Agent
        if (task.getType() == SubTask.TaskType.SYNTHESIZE) {
            return reActAgent.execute(
                    task.getDescription(),
                    null,
                    null
            ).getFinalAnswer();
        }

        // 其他任务使用工具执行
        String toolName = mapTaskTypeToTool(task.getType());
        String parameters = JSON.toJSONString(task.getParameters());

        return toolExecutor.execute(toolName, parameters);
    }

    /**
     * 映射任务类型到工具名称
     */
    private String mapTaskTypeToTool(SubTask.TaskType type) {
        return switch (type) {
            case SEARCH_LAW -> "search_law";
            case SEARCH_CASE -> "search_case";
            case FAQ_LOOKUP -> "faq_lookup";
            case SEARCH_LAWYER -> "search_lawyer";
            case ANALYZE_CONTRACT -> "analyze_contract";
            case CALCULATE_COMPENSATION -> "calculate_compensation";
            case CHECK_STATUTE -> "check_statute_of_limitations";
            case SYNTHESIZE -> "synthesize";
        };
    }

    /**
     * 检查依赖是否完成
     */
    private boolean areDependenciesCompleted(SubTask task, List<SubTaskResult> results) {
        if (task.getDependencies() == null || task.getDependencies().isEmpty()) {
            return true;
        }

        for (String depId : task.getDependencies()) {
            boolean found = results.stream()
                    .anyMatch(r -> r.getTaskId().equals(depId) && r.isSuccess());
            if (!found) {
                return false;
            }
        }

        return true;
    }

    /**
     * 综合结果
     */
    private String synthesize(String question, List<SubTaskResult> results) {
        StringBuilder resultsText = new StringBuilder();
        for (SubTaskResult result : results) {
            resultsText.append("### ").append(result.getDescription()).append("\n");
            resultsText.append(result.isSuccess() ? "✅ 成功" : "❌ 失败").append("\n");
            resultsText.append(result.getResult()).append("\n\n");
        }

        String prompt = String.format(SYNTHESIZER_PROMPT, question, resultsText.toString());

        try {
            return chatService.simpleChat(prompt, "glm-4-plus", 0.5, 4096);
        } catch (Exception e) {
            log.warn("Synthesis failed: {}", e.getMessage());
            return generateFallbackAnswer(results);
        }
    }

    /**
     * 生成降级答案
     */
    private String generateFallbackAnswer(List<SubTaskResult> results) {
        StringBuilder sb = new StringBuilder("根据查询结果：\n\n");
        for (SubTaskResult result : results) {
            if (result.isSuccess()) {
                sb.append("- ").append(result.getDescription()).append("：")
                        .append(truncate(result.getResult(), 200)).append("\n");
            }
        }
        sb.append("\n\n【免责声明】以上信息仅供参考，不构成正式法律意见。");
        return sb.toString();
    }

    private String buildToolsDescription() {
        StringBuilder sb = new StringBuilder();
        List<Map<String, Object>> tools = toolRegistry.getAllTools();
        for (Map<String, Object> tool : tools) {
            Map<String, Object> function = (Map<String, Object>) tool.get("function");
            sb.append("- ").append(function.get("name")).append(": ")
                    .append(function.get("description")).append("\n");
        }
        return sb.toString();
    }

    private String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return "[]";
    }

    private List<String> parseStringList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.size(); i++) {
                list.add(array.getString(i));
            }
        }
        return list;
    }

    private String truncate(String text, int maxLen) {
        return text != null && text.length() > maxLen
                ? text.substring(0, maxLen) + "..."
                : text;
    }

    // ==================== 数据模型 ====================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SubTaskResult {
        private String taskId;
        private String description;
        private String result;
        private boolean success;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PlanExecuteResult {
        private String question;
        private List<SubTask> plan;
        private List<SubTaskResult> results;
        private String finalAnswer;
    }
}
