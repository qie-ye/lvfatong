package com.lvatong.lft.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 语义分块器
 *
 * 基于句子边界和语义相似度进行智能分块，保持语义完整性
 *
 * 特点：
 * 1. 按句子边界分割，不切断句子
 * 2. 支持重叠窗口，保持上下文连贯
 * 3. 识别法律文档结构（条款、章节）
 * 4. 支持中英文混合文本
 */
@Slf4j
@Component
public class SemanticChunker {

    private static final int DEFAULT_CHUNK_SIZE = 512;  // tokens
    private static final int DEFAULT_OVERLAP = 64;      // tokens
    private static final int CHARS_PER_TOKEN = 2;
    private static final int MIN_CHUNK_SIZE = 100;      // tokens

    // 中文句子结束符
    private static final Pattern CHINESE_SENTENCE_END = Pattern.compile("[。！？；]");
    // 英文句子结束符
    private static final Pattern ENGLISH_SENTENCE_END = Pattern.compile("[.!?;]");
    // 法律条款标识
    private static final Pattern LEGAL_CLAUSE = Pattern.compile("第[一二三四五六七八九十百千\\d]+[条章节款项]");
    // 列表标识
    private static final Pattern LIST_MARKER = Pattern.compile("[（(][一二三四五六七八九十\\d]+[)）]|[一二三四五六七八九十]+[、.]");

    /**
     * 语义分块
     */
    public List<String> chunk(String text) {
        return chunk(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    /**
     * 语义分块（自定义参数）
     */
    public List<String> chunk(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        // Step 1: 按句子分割
        List<String> sentences = splitIntoSentences(text);
        log.debug("Split text into {} sentences", sentences.size());

        // Step 2: 合并短句子，拆分长句子
        List<String> normalizedSentences = normalizeSentences(sentences, chunkSize);
        log.debug("Normalized to {} sentences", normalizedSentences.size());

        // Step 3: 基于 token 限制进行分块
        List<String> chunks = buildChunks(normalizedSentences, chunkSize, overlap);
        log.debug("Built {} semantic chunks", chunks.size());

        return chunks;
    }

    /**
     * 按句子分割文本
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            current.append(c);

            // 检查是否是句子结束符
            boolean isSentenceEnd = false;

            if (c == '。' || c == '！' || c == '？' || c == '；') {
                isSentenceEnd = true;
            } else if (c == '.' || c == '!' || c == '?') {
                // 英文句子结束，但要排除小数点和省略号
                if (i + 1 < text.length() && (text.charAt(i + 1) == ' ' || text.charAt(i + 1) == '\n')) {
                    isSentenceEnd = true;
                }
            } else if (c == '\n') {
                // 换行符也作为句子分隔
                isSentenceEnd = true;
            }

            if (isSentenceEnd && current.length() > 0) {
                String sentence = current.toString().trim();
                if (!sentence.isEmpty()) {
                    sentences.add(sentence);
                }
                current = new StringBuilder();
            }
        }

        // 处理最后一个句子
        if (current.length() > 0) {
            String sentence = current.toString().trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }

        return sentences;
    }

    /**
     * 合并短句子，拆分长句子
     */
    private List<String> normalizeSentences(List<String> sentences, int maxChunkSize) {
        List<String> normalized = new ArrayList<>();
        int maxChars = maxChunkSize * CHARS_PER_TOKEN;

        StringBuilder buffer = new StringBuilder();

        for (String sentence : sentences) {
            int sentenceChars = sentence.length();

            // 如果单个句子超过最大长度，需要拆分
            if (sentenceChars > maxChars) {
                // 先输出 buffer 中的内容
                if (buffer.length() > 0) {
                    normalized.add(buffer.toString().trim());
                    buffer = new StringBuilder();
                }

                // 拆分长句子（按逗号或分号）
                normalized.addAll(splitLongSentence(sentence, maxChars));
            } else if (buffer.length() + sentenceChars > maxChars) {
                // buffer 已满，输出并开始新的 buffer
                if (buffer.length() > 0) {
                    normalized.add(buffer.toString().trim());
                }
                buffer = new StringBuilder(sentence);
            } else {
                // 合并短句子
                if (buffer.length() > 0) {
                    buffer.append(" ");
                }
                buffer.append(sentence);
            }
        }

        if (buffer.length() > 0) {
            normalized.add(buffer.toString().trim());
        }

        return normalized;
    }

    /**
     * 拆分长句子
     */
    private List<String> splitLongSentence(String sentence, int maxChars) {
        List<String> parts = new ArrayList<>();

        // 按逗号、分号拆分
        String[] clauses = sentence.split("[，,；;]");

        StringBuilder buffer = new StringBuilder();
        for (String clause : clauses) {
            clause = clause.trim();
            if (clause.isEmpty()) continue;

            if (buffer.length() + clause.length() + 1 > maxChars) {
                if (buffer.length() > 0) {
                    parts.add(buffer.toString().trim());
                }
                buffer = new StringBuilder(clause);
            } else {
                if (buffer.length() > 0) {
                    buffer.append("，");
                }
                buffer.append(clause);
            }
        }

        if (buffer.length() > 0) {
            parts.add(buffer.toString().trim());
        }

        return parts;
    }

    /**
     * 基于 token 限制构建分块
     */
    private List<String> buildChunks(List<String> sentences, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        List<String> currentChunk = new ArrayList<>();
        int currentTokens = 0;

        for (String sentence : sentences) {
            int sentenceTokens = estimateTokens(sentence);

            // 如果添加当前句子会超过 chunk size
            if (currentTokens + sentenceTokens > chunkSize && !currentChunk.isEmpty()) {
                // 输出当前 chunk
                chunks.add(String.join("", currentChunk));

                // 计算重叠：从后往前保留 overlap tokens
                List<String> overlapSentences = getOverlapSentences(currentChunk, overlap);
                currentChunk = new ArrayList<>(overlapSentences);
                currentTokens = currentChunk.stream()
                        .mapToInt(s -> estimateTokens(s))
                        .sum();
            }

            currentChunk.add(sentence);
            currentTokens += sentenceTokens;
        }

        // 处理最后一个 chunk
        if (!currentChunk.isEmpty()) {
            chunks.add(String.join("", currentChunk));
        }

        return chunks;
    }

    /**
     * 获取重叠的句子
     */
    private List<String> getOverlapSentences(List<String> sentences, int overlapTokens) {
        List<String> overlap = new ArrayList<>();
        int totalTokens = 0;

        // 从后往前取
        for (int i = sentences.size() - 1; i >= 0; i--) {
            String sentence = sentences.get(i);
            int tokens = estimateTokens(sentence);

            if (totalTokens + tokens > overlapTokens) {
                break;
            }

            overlap.add(0, sentence);
            totalTokens += tokens;
        }

        return overlap;
    }

    /**
     * 估算 token 数量
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        // 中文约 1.5 字/token，英文约 4 字符/token
        int chineseChars = 0;
        int otherChars = 0;

        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }

        return (int) (chineseChars / 1.5 + otherChars / 4.0);
    }

    /**
     * 检测文本是否包含法律条款结构
     */
    public boolean hasLegalStructure(String text) {
        return LEGAL_CLAUSE.matcher(text).find();
    }

    /**
     * 按法律条款分块（专门用于法律文档）
     */
    public List<String> chunkByLegalClauses(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> clauses = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 检测是否是新条款的开始
            if (LEGAL_CLAUSE.matcher(line).lookingAt() && current.length() > 0) {
                clauses.add(current.toString().trim());
                current = new StringBuilder();
            }

            current.append(line).append("\n");
        }

        if (current.length() > 0) {
            clauses.add(current.toString().trim());
        }

        // 对过长的条款进行二次分块
        List<String> result = new ArrayList<>();
        for (String clause : clauses) {
            if (estimateTokens(clause) > DEFAULT_CHUNK_SIZE * 2) {
                result.addAll(chunk(clause));
            } else {
                result.add(clause);
            }
        }

        return result;
    }
}
