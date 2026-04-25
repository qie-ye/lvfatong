package com.lvatong.lft.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DocumentChunker {

    private static final int CHUNK_SIZE = 512;
    private static final int OVERLAP = 128;
    private static final int CHARS_PER_TOKEN = 2;

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) return List.of();

        List<String> paragraphs = splitByParagraphs(text);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentTokens = 0;

        for (String paragraph : paragraphs) {
            int paraTokens = estimateTokens(paragraph);
            if (currentTokens + paraTokens > CHUNK_SIZE && current.length() > 0) {
                chunks.add(current.toString().trim());
                String overlapText = getOverlapText(current.toString());
                current = new StringBuilder(overlapText);
                currentTokens = estimateTokens(overlapText);
            }
            if (paraTokens > CHUNK_SIZE) {
                if (current.length() > 0) {
                    chunks.add(current.toString().trim());
                    current = new StringBuilder();
                    currentTokens = 0;
                }
                chunks.addAll(splitLongParagraph(paragraph));
            } else {
                current.append(paragraph).append("\n");
                currentTokens += paraTokens;
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }

        log.debug("Chunked text into {} chunks", chunks.size());
        return chunks;
    }

    private List<String> splitByParagraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        String[] parts = text.split("\\n\\s*\\n|\\r\\n\\s*\\r\\n");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                paragraphs.add(trimmed);
            }
        }
        if (paragraphs.isEmpty() && !text.isBlank()) {
            paragraphs.add(text.trim());
        }
        return paragraphs;
    }

    private List<String> splitLongParagraph(String paragraph) {
        List<String> chunks = new ArrayList<>();
        int chunkChars = CHUNK_SIZE * CHARS_PER_TOKEN;
        int overlapChars = OVERLAP * CHARS_PER_TOKEN;
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(start + chunkChars, paragraph.length());
            chunks.add(paragraph.substring(start, end));
            start = end - overlapChars;
            if (start >= paragraph.length()) break;
            if (end == paragraph.length()) break;
        }
        return chunks;
    }

    private String getOverlapText(String text) {
        int overlapChars = OVERLAP * CHARS_PER_TOKEN;
        if (text.length() <= overlapChars) return text;
        int lastNewline = text.lastIndexOf('\n', text.length() - overlapChars);
        if (lastNewline > 0) {
            return text.substring(lastNewline);
        }
        return text.substring(text.length() - overlapChars);
    }

    private int estimateTokens(String text) {
        return text.length() / CHARS_PER_TOKEN;
    }
}
