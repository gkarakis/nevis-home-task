package com.nevis.search.llm;

/**
 * Shared summary text rules. Both the extractive fallback and the LLM path use
 * the same cap so the public document.snippet field stays predictable.
 */
public final class SummaryText {

    private SummaryText() {
    }

    public static String fromContent(String content, int maxChars) {
        if (content == null) {
            return "";
        }
        return clamp(content.strip().replaceAll("\\s+", " "), maxChars);
    }

    public static String clamp(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String stripped = text.strip();
        if (stripped.length() <= maxChars) {
            return stripped;
        }
        return stripped.substring(0, maxChars).stripTrailing() + "…";
    }
}
