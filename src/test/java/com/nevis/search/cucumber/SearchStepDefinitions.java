package com.nevis.search.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchStepDefinitions {

    @Autowired
    private World world;

    @When("I search for {string}")
    public void iSearchFor(String query) {
        world.get("/search?q={q}", query);
    }

    @When("I search for {string} with limit {int}")
    public void iSearchForWithLimit(String query, int limit) {
        world.get("/search?q={q}&limit={limit}", query, limit);
    }

    @When("I search with no query parameter")
    public void iSearchWithNoQueryParameter() {
        world.get("/search");
    }

    @Then("the results should include a client with email {string}")
    public void theResultsShouldIncludeAClientWithEmail(String email) {
        assertThat(clientEmails(world.json())).contains(email);
    }

    @Then("the results should include a document titled {string}")
    public void theResultsShouldIncludeADocumentTitled(String title) {
        assertThat(documentTitles(world.json())).contains(title);
    }

    @Then("document {string} should rank above document {string}")
    public void documentShouldRankAboveDocument(String higher, String lower) {
        List<String> titles = documentTitles(world.json());
        assertThat(titles).contains(higher);
        int higherIndex = titles.indexOf(higher);
        int lowerIndex = titles.indexOf(lower);
        // The lower document may be dropped by the relevance floor; if present it must
        // rank below the higher one.
        if (lowerIndex >= 0) {
            assertThat(higherIndex).as("'%s' ranks above '%s'", higher, lower).isLessThan(lowerIndex);
        }
    }

    @Then("the result count should be at most {int}")
    public void theResultCountShouldBeAtMost(int max) {
        assertThat(world.json().size()).isLessThanOrEqualTo(max);
    }

    private static List<String> clientEmails(JsonNode results) {
        List<String> emails = new ArrayList<>();
        results.forEach(hit -> {
            if ("CLIENT".equals(hit.path("type").asText())) {
                emails.add(hit.path("client").path("email").asText());
            }
        });
        return emails;
    }

    private static List<String> documentTitles(JsonNode results) {
        List<String> titles = new ArrayList<>();
        results.forEach(hit -> {
            if (hit.path("document").has("title")) {
                titles.add(hit.path("document").path("title").asText());
            }
        });
        return titles;
    }
}
