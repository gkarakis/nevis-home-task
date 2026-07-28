package com.nevis.search.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientStepDefinitions {

    @Autowired
    private World world;

    @When("I create a client with:")
    public void iCreateAClientWith(String jsonBody) {
        world.post("/clients", jsonBody);
    }

    /**
     * Precondition step for other features: creates a client with a unique email so it
     * can be re-run across scenarios without colliding, and remembers its id under the
     * given alias for later steps (e.g. attaching a document).
     */
    @Given("a client named {string} exists")
    public void aClientNamedExists(String alias) {
        String email = alias.toLowerCase().replaceAll("[^a-z0-9]", "")
                + "-" + UUID.randomUUID() + "@example.com";
        world.post("/clients", """
                { "first_name": "%s", "last_name": "Owner", "email": "%s" }
                """.formatted(alias, email));
        assertThat(world.status()).as("creating precondition client '%s'", alias).isEqualTo(201);
        world.rememberClient(alias, UUID.fromString(world.json().get("id").asText()));
    }

    @When("I list all clients")
    public void iListAllClients() {
        world.get("/clients");
    }

    @Then("the client list should include email {string}")
    public void theClientListShouldIncludeEmail(String email) {
        boolean found = false;
        for (JsonNode client : world.json()) {
            if (email.equals(client.path("email").asText())) {
                found = true;
                break;
            }
        }
        assertThat(found).as("client list contains %s", email).isTrue();
    }
}
