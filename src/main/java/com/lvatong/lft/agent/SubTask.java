package com.lvatong.lft.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 子任务定义，用于 Plan-and-Execute 模式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTask {

    private String id;
    private String description;
    private TaskType type;
    private Map<String, Object> parameters;
    private TaskStatus status;
    private String result;
    private List<String> dependencies;

    public enum TaskType {
        SEARCH_LAW,           // 搜索法条
        SEARCH_CASE,          // 搜索案例
        FAQ_LOOKUP,           // FAQ查询
        SEARCH_LAWYER,        // 搜索律师
        ANALYZE_CONTRACT,     // 分析合同
        CALCULATE_COMPENSATION, // 计算赔偿
        CHECK_STATUTE,        // 检查时效
        SYNTHESIZE            // 综合分析
    }

    public enum TaskStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}
