package com.nevis.search.embedding;

import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Builds the in-process ONNX embedding model (all-MiniLM-L6-v2, 384 dims).
 *
 * <p>In Docker the model and tokenizer are baked in at build time and pointed at
 * with {@code file:/opt/model/...} URIs, so a cold container never needs network
 * access to serve its first request. When the URIs are left blank (local dev),
 * Spring AI falls back to its bundled default artifacts.
 */
@Configuration
public class EmbeddingConfig {

    @Bean(destroyMethod = "")
    public TransformersEmbeddingModel transformersEmbeddingModel(
            @Value("${spring.ai.embedding.transformer.onnx.model-uri:}") String modelUri,
            @Value("${spring.ai.embedding.transformer.tokenizer.uri:}") String tokenizerUri
    ) throws Exception {
        TransformersEmbeddingModel model = new TransformersEmbeddingModel();
        if (StringUtils.hasText(modelUri)) {
            model.setModelResource(modelUri);
        }
        if (StringUtils.hasText(tokenizerUri)) {
            model.setTokenizerResource(tokenizerUri);
        }
        model.afterPropertiesSet();
        return model;
    }
}
