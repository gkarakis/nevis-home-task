package com.nevis.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nevis.search.common.SearchNormalizer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack golden cases against real Postgres (pgvector) and the real embedding
 * model. Flyway runs against the container, so the migrations — including the
 * IMMUTABLE function and the generated columns — are under test too.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("demo")
@Testcontainers
@Tag("integration")
// DJL ships no working tokenizer native library for macOS Intel (osx-x86_64) — it
// fails with "Unexpected flavor: cpu". The Docker target is linux/amd64, and this
// suite runs there, on Linux CI, and on Apple Silicon. Verify the golden cases on
// this host with `docker compose up` and the curl commands in the README.
@DisabledOnOs(value = OS.MAC, architectures = "x86_64",
        disabledReason = "DJL tokenizer native lib unavailable on macOS Intel; "
                + "run the integration suite on linux/amd64 (Docker/CI) or Apple Silicon")
class SearchIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> DB =
            new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DB::getJdbcUrl);
        registry.add("spring.datasource.username", DB::getUsername);
        registry.add("spring.datasource.password", DB::getPassword);
    }

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired SearchNormalizer normalizer;
    @LocalServerPort int port;

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode search(String q) throws Exception {
        ResponseEntity<String> resp = rest.getForEntity(
                "http://localhost:" + port + "/search?q={q}", String.class, q);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        return mapper.readTree(resp.getBody());
    }

    private List<String> titles(JsonNode array) {
        List<String> out = new ArrayList<>();
        array.forEach(hit -> {
            if (hit.path("document").has("title")) {
                out.add(hit.path("document").path("title").asText());
            }
        });
        return out;
    }

    private List<String> clientEmails(JsonNode array) {
        List<String> out = new ArrayList<>();
        array.forEach(hit -> {
            if ("CLIENT".equals(hit.path("type").asText())) {
                out.add(hit.path("client").path("email").asText());
            }
        });
        return out;
    }

    /** The CLIENT hit for the given email, or {@code null} if the query did not return it. */
    private JsonNode clientHit(JsonNode array, String email) {
        for (JsonNode hit : array) {
            if ("CLIENT".equals(hit.path("type").asText())
                    && email.equals(hit.path("client").path("email").asText())) {
                return hit;
            }
        }
        return null;
    }

    private List<String> channels(JsonNode hit) {
        List<String> out = new ArrayList<>();
        hit.path("matched_by").forEach(c -> out.add(c.asText()));
        return out;
    }

    @Test
    void nevisWealthSubstringFindsJohnDoe() throws Exception {
        JsonNode result = search("NevisWealth");
        assertThat(clientEmails(result)).contains("john.doe@neviswealth.com");
    }

    @Test
    void clientSearchIsCaseInsensitive() throws Exception {
        List<String> upper = clientEmails(search("NEVISWEALTH"));
        List<String> lower = clientEmails(search("neviswealth"));
        assertThat(upper).containsExactlyElementsOf(lower);
        assertThat(upper).contains("john.doe@neviswealth.com");
    }

    @Test
    void addressProofRanksUtilityBillAbovePortfolioNote() throws Exception {
        List<String> titles = titles(search("address proof"));
        assertThat(titles).contains("John's utility bill");
        int utility = titles.indexOf("John's utility bill");
        int portfolio = titles.indexOf("Portfolio note");
        // Portfolio note may be dropped by the floor; if present it must rank lower.
        if (portfolio >= 0) {
            assertThat(utility).isLessThan(portfolio);
        }
    }

    @Test
    void descriptionSubstringIsFuzzyNotDirectIdentityMatch() throws Exception {
        // "long-standing" appears only in John Doe's description — never in his name,
        // email, or any document. A description substring is not an identity signal,
        // so it must be a fuzzy-tier hit (matched_by FUZZY), not a tier-1 direct match.
        // The FUZZY channel is what routes the client below documents in ResultOrdering,
        // so this assertion also pins the cross-type ordering (README §3).
        JsonNode john = clientHit(search("long-standing"), "john.doe@neviswealth.com");
        assertThat(john).as("John Doe should still be retrieved via his description").isNotNull();
        assertThat(channels(john)).containsExactly("FUZZY");
    }

    @Test
    void nameOrEmailSubstringRemainsDirectIdentityMatch() throws Exception {
        // Control for the case above: an email substring is still a tier-1 identity match.
        JsonNode john = clientHit(search("NevisWealth"), "john.doe@neviswealth.com");
        assertThat(john).as("John Doe should be found by the email substring").isNotNull();
        assertThat(channels(john)).containsExactly("DIRECT");
    }

    @Test
    void johnDoeMatchesAcrossFirstAndLastName() throws Exception {
        assertThat(clientEmails(search("John Doe"))).contains("john.doe@neviswealth.com");
    }

    @Test
    void allApostropheAndAccentVariantsResolve() throws Exception {
        for (String q : List.of("O'Hara", "O’Hara", "OHara", "o hara")) {
            assertThat(clientEmails(search(q)))
                    .as("query: %s", q)
                    .contains("jack.ohara@example.com");
        }
        assertThat(clientEmails(search("Jose Alvarez"))).contains("jose.alvarez@example.com");
    }

    @Test
    void identifierWithPunctuationVariationMatches() throws Exception {
        assertThat(titles(search("ACC 889134"))).contains("Account reference");
    }

    @Test
    void offTopicQueryReturnsEmptyArray() throws Exception {
        JsonNode result = search("elephant breeding habits");
        assertThat(result.isArray()).isTrue();
        assertThat(result).isEmpty();
    }

    @Test
    void missingQueryParameterReturns400() {
        ResponseEntity<String> resp = rest.getForEntity(
                "http://localhost:" + port + "/search", String.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void punctuationOnlyQueryReturns400NotATableScan() {
        ResponseEntity<String> resp = rest.getForEntity(
                "http://localhost:" + port + "/search?q={q}", String.class, "---");
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
        assertThat(resp.getBody()).contains("INVALID_QUERY");
    }

    @Test
    void oversizedLimitIsClampedNotRejected() throws Exception {
        ResponseEntity<String> resp = rest.getForEntity(
                "http://localhost:" + port + "/search?q={q}&limit=5000", String.class, "John");
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        JsonNode array = mapper.readTree(resp.getBody());
        assertThat(array.isArray()).isTrue();
        assertThat(array.size()).isLessThanOrEqualTo(100);
    }

    @Test
    void javaAndSqlNormalisationAgree() {
        for (String[] fixture : com.nevis.search.common.NormalizationFixtures.all()) {
            String input = fixture[0];
            String dbResult = jdbc.queryForObject(
                    "SELECT search_normalize(?)", String.class, input);
            assertThat(normalizer.normalize(input))
                    .as("normalisation of: %s", input)
                    .isEqualTo(dbResult);
        }
    }
}
