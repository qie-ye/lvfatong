package com.lvatong.lft.contract;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ClauseExtractor {

    private static final List<Pattern> CLAUSE_PATTERNS = List.of(
            Pattern.compile("(?m)^\\s*第[一二三四五六七八九十百零\\d]+[条章节]\\s*[、：:]?(.*)"),
            Pattern.compile("(?m)^\\s*[（(][一二三四五六七八九十\\d]+[）)]\\s*(.*)"),
            Pattern.compile("(?m)^\\s*[一二三四五六七八九十]+[、.]\\s*(.*)"),
            Pattern.compile("(?m)^\\s*\\d+[、.．]\\s*(.*)"),
            Pattern.compile("(?m)^\\s*(?:甲方|乙方|丙方).*[:：](.*)"),
            Pattern.compile("(?m)^\\s*(?:合同|协议|条款)(?:名称|编号|期限|标的|价款|违约|争议|保密|不可抗力|生效).*")
    );

    /**
     * 从合同文本中提取条款结构
     */
    public List<Clause> extract(String text) {
        if (text == null || text.isBlank()) return List.of();

        List<Clause> clauses = new ArrayList<>();
        String[] lines = text.split("\\n");
        StringBuilder currentContent = new StringBuilder();
        String currentTitle = null;
        int clauseIndex = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            boolean isClauseHeader = false;
            for (Pattern pattern : CLAUSE_PATTERNS) {
                Matcher matcher = pattern.matcher(trimmed);
                if (matcher.find()) {
                    if (currentTitle != null && currentContent.length() > 0) {
                        clauseIndex++;
                        Clause clause = new Clause();
                        clause.setIndex(clauseIndex);
                        clause.setTitle(currentTitle);
                        clause.setContent(currentContent.toString().trim());
                        clauses.add(clause);
                    }
                    currentTitle = trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
                    currentContent = new StringBuilder();
                    isClauseHeader = true;
                    break;
                }
            }

            if (!isClauseHeader) {
                currentContent.append(trimmed).append("\n");
            }
        }

        if (currentTitle != null && currentContent.length() > 0) {
            clauseIndex++;
            Clause clause = new Clause();
            clause.setIndex(clauseIndex);
            clause.setTitle(currentTitle);
            clause.setContent(currentContent.toString().trim());
            clauses.add(clause);
        }

        if (clauses.isEmpty() && !text.isBlank()) {
            Clause whole = new Clause();
            whole.setIndex(1);
            whole.setTitle("合同全文");
            whole.setContent(text.length() > 5000 ? text.substring(0, 5000) : text);
            clauses.add(whole);
        }

        log.debug("Extracted {} clauses from contract text", clauses.size());
        return clauses;
    }

    @Data
    public static class Clause {
        private int index;
        private String title;
        private String content;
    }
}
