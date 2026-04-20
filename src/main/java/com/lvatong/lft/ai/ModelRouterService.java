package com.lvatong.lft.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ModelRouterService {

    public enum TaskType {
        LEGAL_QA,       // 法律问答 -> GLM-4-Flash
        CONTRACT_ANALYSIS, // 合同分析 -> GLM-4-Long
        DEEP_REASONING,   // 深度推理 -> GLM-4-Plus (Phase 2)
        EMBEDDING         // 向量化 -> BGE-M3
    }

    public String getModelForTask(TaskType taskType) {
        return switch (taskType) {
            case LEGAL_QA -> "glm-4-flash";
            case CONTRACT_ANALYSIS -> "glm-4-long";
            case DEEP_REASONING -> "glm-4-flash"; // Phase 2: glm-4-plus
            case EMBEDDING -> "bge-m3";
        };
    }

    public String getFallbackModel(String primaryModel) {
        return "glm-4-flash";
    }
}
