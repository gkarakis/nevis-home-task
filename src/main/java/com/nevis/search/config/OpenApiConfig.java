package com.nevis.search.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI nevisOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Nevis Search API")
                .version("1.0.0")
                .description("""
                        Clients and their documents, returned as one ranked list.

                        Document relevance fuses a semantic (vector) and a lexical
                        (full-text + identifier) signal with reciprocal rank fusion.
                        Cross-type ordering (documents vs. clients) follows an explicit
                        tier rule, not a fabricated score comparison: directly matched
                        clients, then documents by fused relevance, then fuzzily matched
                        clients. `score` is relevance within a result's own type, so array
                        order is not globally monotonic."""));
    }
}
