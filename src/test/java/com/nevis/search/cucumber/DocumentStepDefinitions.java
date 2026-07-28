package com.nevis.search.cucumber;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class DocumentStepDefinitions {

    @Autowired
    private World world;

    @When("I add a document to client {string} with:")
    public void iAddADocumentToClientWith(String alias, String jsonBody) {
        world.post("/clients/" + world.clientId(alias) + "/documents", jsonBody);
    }

    @When("I add a document to a non-existent client with:")
    public void iAddADocumentToANonExistentClientWith(String jsonBody) {
        world.post("/clients/" + UUID.randomUUID() + "/documents", jsonBody);
    }

    @Then("the document should have at least {int} chunk")
    public void theDocumentShouldHaveAtLeastChunk(int minChunks) {
        assertThat(world.json().path("chunk_count").asInt())
                .as("chunk_count")
                .isGreaterThanOrEqualTo(minChunks);
    }
}
