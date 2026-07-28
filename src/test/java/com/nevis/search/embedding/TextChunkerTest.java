package com.nevis.search.embedding;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void shortContentIsOneChunk() {
        List<TextChunker.Chunk> chunks = chunker.chunk("A short document. Two sentences.");
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).index()).isZero();
    }

    @Test
    void emptyContentProducesNoChunks() {
        assertThat(chunker.chunk("")).isEmpty();
        assertThat(chunker.chunk("   ")).isEmpty();
        assertThat(chunker.chunk(null)).isEmpty();
    }

    @Test
    void longContentIsSplitAndIndexedSequentially() {
        String sentence = "This sentence has some words in it. ";
        String content = sentence.repeat(100); // ~3600 chars

        List<TextChunker.Chunk> chunks = chunker.chunk(content);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(c ->
                assertThat(c.text().length()).isLessThanOrEqualTo(TextChunker.TARGET_CHARS));
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).index()).isEqualTo(i);
        }
    }

    @Test
    void aSingleOversizedSentenceIsHardSplit() {
        String giant = "word ".repeat(400); // ~2000 chars, no sentence boundary
        List<TextChunker.Chunk> chunks = chunker.chunk(giant);
        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(c ->
                assertThat(c.text().length()).isLessThanOrEqualTo(TextChunker.TARGET_CHARS));
    }
}
