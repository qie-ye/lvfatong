package com.lvatong.lft.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ModelRouterService {

    public enum TaskType {
        LEGAL_QA,          // 法律问答 -> GLM-4-Flash
        CONTRACT_ANALYSIS, // 合同分析 -> GLM-4-Long
        DEEP_REASONING,    // 深度推理/复杂法律 -> GLM-4-Plus
        CASE_SEARCH,       // 案例检索 -> GLM-4-Flash
        EMBEDDING          // 向量化 -> BGE-M3
    }

    private static final Map<TaskType, String> PRIMARY_MODELS = Map.ofEntries(
            Map.entry(TaskType.LEGAL_QA, "glm-4-flash"),
            Map.entry(TaskType.CONTRACT_ANALYSIS, "glm-4-long"),
            Map.entry(TaskType.DEEP_REASONING, "glm-4-plus"),
            Map.entry(TaskType.CASE_SEARCH, "glm-4-flash"),
            Map.entry(TaskType.EMBEDDING, "embedding-3")
    );

    private static final String FALLBACK_MODEL = "glm-4-flash";

    private final Map<String, Long> modelFailureTime = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 60_000;

    public String getModelForTask(TaskType taskType) {
        String primary = PRIMARY_MODELS.get(taskType);
        if (isModelCoolingDown(primary)) {
            log.info("Model {} is in cooldown, using fallback {}", primary, FALLBACK_MODEL);
            return FALLBACK_MODEL;
        }
        return primary;
    }

    public String getFallbackModel(String primaryModel) {
        return FALLBACK_MODEL;
    }

    public void markModelFailure(String model) {
        modelFailureTime.put(model, System.currentTimeMillis());
        log.warn("Model {} marked as failed, cooldown for {}ms", model, COOLDOWN_MS);
    }

    private boolean isModelCoolingDown(String model) {
        Long failureTime = modelFailureTime.get(model);
        if (failureTime == null) return false;
        return System.currentTimeMillis() - failureTime < COOLDOWN_MS;
    }
}
