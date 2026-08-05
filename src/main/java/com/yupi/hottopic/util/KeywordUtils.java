package com.yupi.hottopic.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 关键词纯文本工具(需求 AI-1/AI-7 的降级与预匹配)
 */
public final class KeywordUtils {

    private KeywordUtils() {
    }

    /**
     * 从关键词中提取核心词(纯文本方式,不依赖 AI)。
     * 按空格/连字符/下划线/斜杠等分割,并做相邻两两组合。
     */
    public static List<String> extractCoreTerms(String keyword) {
        Set<String> terms = new LinkedHashSet<>();
        String[] parts = keyword.split("[\\s\\-_/\\\\·]+");
        List<String> valid = new ArrayList<>();
        for (String p : parts) {
            if (p.length() >= 2) {
                valid.add(p);
            }
        }
        if (valid.size() > 1) {
            terms.addAll(valid);
            for (int i = 0; i < valid.size() - 1; i++) {
                terms.add(valid.get(i) + " " + valid.get(i + 1));
            }
        }
        // 排除原始关键词本身
        terms.removeIf(t -> t.equalsIgnoreCase(keyword));
        return new ArrayList<>(terms);
    }

    /**
     * 检查文本是否包含任一扩展关键词(不区分大小写)。
     *
     * @return 匹配结果与命中的词
     */
    public static PreMatchResult preMatchKeyword(String text, List<String> expandedKeywords) {
        if (text == null || expandedKeywords == null || expandedKeywords.isEmpty()) {
            return new PreMatchResult(false, List.of());
        }
        String lowerText = text.toLowerCase();
        List<String> matched = new ArrayList<>();
        for (String kw : expandedKeywords) {
            if (kw != null && !kw.isBlank() && lowerText.contains(kw.toLowerCase())) {
                matched.add(kw);
            }
        }
        return new PreMatchResult(!matched.isEmpty(), matched);
    }

    /** 预匹配结果 */
    public record PreMatchResult(boolean matched, List<String> matchedTerms) {
    }
}
