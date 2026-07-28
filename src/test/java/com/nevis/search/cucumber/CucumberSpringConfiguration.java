package com.nevis.search.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The single Spring context shared by every Cucumber scenario. Boots the full app on
 * a random port against a real Postgres (pgvector) container and the real embedding
 * model — the same footprint as {@code SearchIntegrationTest} — so the feature files
 * exercise the application end to end. The {@code demo} profile seeds the fixture
 * clients and documents the search scenarios rely on.
 *
 * <p>Like the rest of the integration suite, this cannot run on macOS Intel (the DJL
 * tokenizer native library is unavailable there); run it on linux/amd64, in Docker,
 * on CI, or on Apple Silicon.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("demo")
@Import(World.class)
public class CucumberSpringConfiguration {

    // Singleton container: started once for the whole run and reused across scenarios;
    // Testcontainers' Ryuk sidecar stops it when the JVM exits.
    static final PostgreSQLContainer<?> DB =
            new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"));

    static {
        DB.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DB::getJdbcUrl);
        registry.add("spring.datasource.username", DB::getUsername);
        registry.add("spring.datasource.password", DB::getPassword);
    }
}
