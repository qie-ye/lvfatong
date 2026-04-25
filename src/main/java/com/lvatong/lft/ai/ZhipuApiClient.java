package com.lvatong.lft.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lvatong.lft.common.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ZhipuApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${zhipu.api-key:}")
    private String apiKey;

    @Value("${zhipu.base-url:https://open.bigmodel.cn/api/paas/v4}")
    private String baseUrl;

    private volatile boolean apiKeyConfigured = false;

    public ZhipuApiClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @jakarta.annotation.PostConstruct
    void checkApiKey() {
        apiKeyConfigured = apiKey != null && !apiKey.isBlank();
        if (!apiKeyConfigured) {
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("  ZHIPU_API_KEY 未配置！AI功能（法律咨询/意见书/合同分析）将不可用");
            log.error("  请设置环境变量 ZHIPU_API_KEY 或在 application.yml 中配置");
            log.error("  获取API Key: https://open.bigmodel.cn/usercenter/apikeys");
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        } else {
            log.info("Zhipu API key configured (length={})", apiKey.length());
        }
    }

    public boolean isApiKeyConfigured() {
        return apiKeyConfigured;
    }

    /**
     * 非流式Chat调用（带指数退避重试）
     */
    @CircuitBreaker(name = "zhipu", fallbackMethod = "chatFallback")
    public String chat(String model, List<Map<String, String>> messages, double temperature, int maxTokens) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);

        ArrayNode msgArray = body.putArray("messages");
        for (Map<String, String> msg : messages) {
            ObjectNode msgNode = msgArray.addObject();
            msgNode.put("role", msg.get("role"));
            msgNode.put("content", msg.get("content"));
        }

        return executeWithRetry(() -> {
            HttpHeaders headers = buildHeaders();
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/chat/completions", HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("error")) {
                throw new BusinessException("智谱API错误: " + root.get("error").get("message").asText());
            }
            return root.get("choices").get(0).get("message").get("content").asText();
        }, "chat-" + model);
    }

    /**
     * SSE流式Chat调用 - 返回SSE事件的原始行
     */
    @CircuitBreaker(name = "zhipu", fallbackMethod = "chatStreamFallback")
    public void chatStream(String model, List<Map<String, String>> messages, double temperature, int maxTokens, SseEventHandler handler) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("stream", true);

        ArrayNode msgArray = body.putArray("messages");
        for (Map<String, String> msg : messages) {
            ObjectNode msgNode = msgArray.addObject();
            msgNode.put("role", msg.get("role"));
            msgNode.put("content", msg.get("content"));
        }

        executeWithRetry(() -> {
            HttpHeaders headers = buildHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

            restTemplate.execute(baseUrl + "/chat/completions", HttpMethod.POST,
                    req -> {
                        req.getHeaders().putAll(headers);
                        req.getBody().write(body.toString().getBytes());
                    },
                    resp -> {
                        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(resp.getBody()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6).trim();
                                    if ("[DONE]".equals(data)) {
                                        handler.onComplete();
                                        return null;
                                    }
                                    JsonNode chunk = objectMapper.readTree(data);
                                    JsonNode delta = chunk.get("choices").get(0).get("delta");
                                    if (delta.has("content")) {
                                        handler.onContent(delta.get("content").asText());
                                    }
                                }
                            }
                            handler.onComplete();
                        }
                        return null;
                    });
            return null;
        }, "chat-stream-" + model);
    }

    /**
     * BGE-M3 Embedding调用
     */
    public List<Float> embed(String text) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", "embedding-3");
        ArrayNode inputArray = body.putArray("input");
        inputArray.add(text);

        return executeWithRetry(() -> {
            HttpHeaders headers = buildHeaders();
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/embeddings", HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("error")) {
                throw new BusinessException("智谱Embedding错误: " + root.get("error").get("message").asText());
            }
            ArrayNode embeddingArray = (ArrayNode) root.get("data").get(0).get("embedding");
            java.util.ArrayList<Float> result = new java.util.ArrayList<>();
            for (JsonNode node : embeddingArray) {
                result.add((float) node.asDouble());
            }
            return result;
        }, "embedding");
    }

    /**
     * 批量Embedding
     */
    public List<List<Float>> embedBatch(List<String> texts) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", "bge-m3");
        ArrayNode inputArray = body.putArray("input");
        for (String text : texts) {
            inputArray.add(text);
        }

        return executeWithRetry(() -> {
            HttpHeaders headers = buildHeaders();
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/embeddings", HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            ArrayNode dataArray = (ArrayNode) root.get("data");
            java.util.ArrayList<List<Float>> result = new java.util.ArrayList<>();
            for (JsonNode item : dataArray) {
                ArrayNode embeddingArray = (ArrayNode) item.get("embedding");
                java.util.ArrayList<Float> embedding = new java.util.ArrayList<>();
                for (JsonNode node : embeddingArray) {
                    embedding.add((float) node.asDouble());
                }
                result.add(embedding);
            }
            return result;
        }, "embedding-batch");
    }

    public record ToolCall(String id, String name, String arguments) {}

    public record ChatWithToolsResult(String content, List<ToolCall> toolCalls) {
        public boolean hasToolCalls() { return toolCalls != null && !toolCalls.isEmpty(); }
    }

    /**
     * 支持 Function Calling 的非流式 Chat（智谱 tool_choice=auto 协议）
     * Messages 使用 Map<String,Object> 以支持 tool_calls 等复杂结构
     */
    @CircuitBreaker(name = "zhipu", fallbackMethod = "chatWithToolsFallback")
    public ChatWithToolsResult chatWithTools(String model, List<Map<String, Object>> messages,
                                              List<Map<String, Object>> tools,
                                              double temperature, int maxTokens) {
        return executeWithRetry(() -> {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("temperature", temperature);
            body.put("max_tokens", maxTokens);
            body.put("tool_choice", "auto");

            // Build messages array
            ArrayNode msgArray = body.putArray("messages");
            for (Map<String, Object> msg : messages) {
                ObjectNode msgNode = msgArray.addObject();
                msgNode.put("role", msg.get("role").toString());
                if (msg.containsKey("content") && msg.get("content") != null) {
                    msgNode.put("content", msg.get("content").toString());
                }
                if (msg.containsKey("tool_call_id")) {
                    msgNode.put("tool_call_id", msg.get("tool_call_id").toString());
                }
                if (msg.containsKey("tool_calls")) {
                    @SuppressWarnings("unchecked")
                    List<ToolCall> tcs = (List<ToolCall>) msg.get("tool_calls");
                    ArrayNode tcArray = msgNode.putArray("tool_calls");
                    for (ToolCall tc : tcs) {
                        ObjectNode tcNode = tcArray.addObject();
                        tcNode.put("id", tc.id());
                        tcNode.put("type", "function");
                        ObjectNode fnNode = tcNode.putObject("function");
                        fnNode.put("name", tc.name());
                        fnNode.put("arguments", tc.arguments());
                    }
                }
            }

            // Build tools array
            ArrayNode toolsArray = body.putArray("tools");
            for (Map<String, Object> tool : tools) {
                toolsArray.add(objectMapper.valueToTree(tool));
            }

            HttpHeaders headers = buildHeaders();
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/chat/completions", HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("error")) {
                throw new BusinessException("智谱API错误: " + root.get("error").get("message").asText());
            }

            JsonNode choice = root.get("choices").get(0);
            String finishReason = choice.has("finish_reason") ? choice.get("finish_reason").asText() : "stop";
            JsonNode message = choice.get("message");

            if ("tool_calls".equals(finishReason) && message.has("tool_calls")) {
                List<ToolCall> toolCallList = new java.util.ArrayList<>();
                for (JsonNode tc : message.get("tool_calls")) {
                    String id = tc.get("id").asText();
                    String name = tc.get("function").get("name").asText();
                    String arguments = tc.get("function").get("arguments").asText();
                    toolCallList.add(new ToolCall(id, name, arguments));
                }
                return new ChatWithToolsResult(null, toolCallList);
            }

            String content = message.has("content") ? message.get("content").asText() : "";
            return new ChatWithToolsResult(content, null);
        }, "chat-tools-" + model);
    }

    // ── Fallback Methods ──────────────────────────────────────────────────────

    String chatFallback(String model, List<Map<String, String>> messages, double temperature, int maxTokens, Throwable t) {
        log.warn("[CircuitBreaker] chat fallback triggered for model={}, reason={}", model, t.getMessage());
        if (!apiKeyConfigured) {
            return "抱歉，AI服务未配置（ZHIPU_API_KEY未设置），请联系管理员配置后再试。";
        }
        return "抱歉，AI服务当前暂时不可用，请稍后再试。如有紧急法律问题，请直接咨询专业律师。";
    }

    void chatStreamFallback(String model, List<Map<String, String>> messages, double temperature, int maxTokens, SseEventHandler handler, Throwable t) {
        log.warn("[CircuitBreaker] chatStream fallback triggered for model={}, reason={}", model, t.getMessage());
        if (!apiKeyConfigured) {
            handler.onContent("抱歉，AI服务未配置（ZHIPU_API_KEY未设置），请联系管理员配置后再试。");
        } else {
            handler.onContent("抱歉，AI服务当前暂时不可用，请稍后再试。");
        }
        handler.onComplete();
    }

    ChatWithToolsResult chatWithToolsFallback(String model, List<Map<String, Object>> messages, List<Map<String, Object>> tools, double temperature, int maxTokens, Throwable t) {
        log.warn("[CircuitBreaker] chatWithToolsFallback triggered for model={}, reason={}", model, t.getMessage());
        if (!apiKeyConfigured) {
            return new ChatWithToolsResult("抱歉，AI服务未配置（ZHIPU_API_KEY未设置），请联系管理员配置后再试。", null);
        }
        return new ChatWithToolsResult("抱歉，AI服务当前暂时不可用，请稍后再试。", null);
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        return headers;
    }

    /**
     * 指数退避重试（最多3次）
     */
    private <T> T executeWithRetry(RetryableTask<T> task, String label) {
        int maxRetries = 3;
        long baseDelayMs = 1000;
        Exception lastException = null;
        for (int i = 0; i <= maxRetries; i++) {
            try {
                return task.execute();
            } catch (Exception e) {
                lastException = e;
                if (i < maxRetries) {
                    long delay = baseDelayMs * (1L << i);
                    log.warn("{} failed (attempt {}/{}), retrying in {}ms: {}", label, i + 1, maxRetries + 1, delay, e.getMessage());
                    try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        log.error("{} failed after {} retries", label, maxRetries);
        throw new BusinessException("AI服务调用失败，请稍后重试");
    }

    @FunctionalInterface
    public interface RetryableTask<T> {
        T execute() throws Exception;
    }

    @FunctionalInterface
    public interface SseEventHandler {
        void onContent(String content);
        default void onComplete() {}
    }
}
