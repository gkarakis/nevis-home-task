package com.nevis.search.document;

/** A chunk paired with its computed embedding, ready to persist. */
public record EmbeddedChunk(int index, String text, float[] embedding) {
}
