package com.nevis.search.embedding;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits document content into chunks small enough for the model's input window.
 * Deliberately plain: accumulate sentences until roughly {@value #TARGET_CHARS}
 * characters, then start a new chunk. No tokenizer introspection, no overlap
 * tuning, no dependencies.
 */
@Component
public class TextChunker {

    static final int TARGET_CHARS = 800;

    // Split on sentence-ending punctuation followed by whitespace.
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?])\\s+");

    public record Chunk(int index, String text) {}

    public List<Chunk> chunk(String content) {
        List<Chunk> chunks = new ArrayList<>();
        String trimmed = content == null ? "" : content.strip();
        if (trimmed.isEmpty()) {
            return chunks;
        }

        StringBuilder current = new StringBuilder();
        int index = 0;
        for (String sentence : SENTENCE_BOUNDARY.split(trimmed)) {
            String piece = sentence.strip();
            if (piece.isEmpty()) {
                continue;
            }
            // A single oversized sentence is hard-split so no chunk blows the window.
            if (piece.length() > TARGET_CHARS) {
                if (!current.isEmpty()) {
                    chunks.add(new Chunk(index++, current.toString().strip()));
                    current.setLength(0);
                }
                for (int start = 0; start < piece.length(); start += TARGET_CHARS) {
                    int end = Math.min(start + TARGET_CHARS, piece.length());
                    chunks.add(new Chunk(index++, piece.substring(start, end).strip()));
                }
                continue;
            }
            if (!current.isEmpty() && current.length() + 1 + piece.length() > TARGET_CHARS) {
                chunks.add(new Chunk(index++, current.toString().strip()));
                current.setLength(0);
            }
            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(piece);
        }
        if (!current.isEmpty()) {
            chunks.add(new Chunk(index, current.toString().strip()));
        }
        return chunks;
    }
}
