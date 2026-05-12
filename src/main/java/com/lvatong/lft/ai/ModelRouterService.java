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

    /**
     * 模型参数配置：temperature 和 top_p
     *
     * temperature：控制输出的随机性
     *   - 0.0：最确定，适合事实性问答
     *   - 0.5：平衡，适合大多数场景
     *   - 1.0：最随机，适合创意写作
     *
     * top_p：核采样，控制输出的多样性
     *   - 0.8：较少多样性
     *   - 0.9：平衡（推荐）
     *   - 0.95：较多多样性
     *
     * 法律场景建议：
     *   - 法律问答：temperature=0.3, top_p=0.9（确定性高，减少幻觉）
     *   - 合同分析：temperature=0.5, top_p=0.85（需要一定创造性分析）
     *   - 深度推理：temperature=0.6, top_p=0.85（需要推理灵活性）
     *   - 案例检索：temperature=0.3, top_p=0.9（事实性检索）
     */
    private static final Map<TaskType, ModelParams> MODEL_PARAMS = Map.ofEntries(
            Map.entry(TaskType.LEGAL_QA, new ModelParams(0.3, 0.9, 4096)),
            Map.entry(TaskType.CONTRACT_ANALYSIS, new ModelParams(0.5, 0.85, 16384)),
            Map.entry(TaskType.DEEP_REASONING, new ModelParams(0.6, 0.85, 8192)),
            Map.entry(TaskType.CASE_SEARCH, new ModelParams(0.3, 0.9, 4096)),
            Map.entry(TaskType.EMBEDDING, new ModelParams(0.0, 1.0, 0))
    );

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

    /**
     * 获取模型参数（temperature, top_p, max_tokens）
     */
    public ModelParams getParamsForTask(TaskType taskType) {
        return MODEL_PARAMS.getOrDefault(taskType, new ModelParams(0.7, 0.9, 4096));
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

    /**
     * 模型参数封装类
     */
    public record ModelParams(double temperature, double topP, int maxTokens) {}
}
