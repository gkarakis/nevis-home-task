package com.nevis.search.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Response assertions shared by every feature. */
public class CommonStepDefinitions {

    @Autowired
    private World world;

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expected) {
        assertThat(world.status())
                .as("HTTP status; body was: %s", world.body())
                .isEqualTo(expected);
    }

    @Then("the response field {string} should be {string}")
    public void theResponseFieldShouldBe(String path, String expected) {
        JsonNode node = world.json().at(toPointer(path));
        assertThat(node.isMissingNode()).as("field '%s' is present", path).isFalse();
        assertThat(node.asText()).as("field '%s'", path).isEqualTo(expected);
    }

    @Then("the response body should contain {string}")
    public void theResponseBodyShouldContain(String text) {
        assertThat(world.body()).contains(text);
    }

    @Then("the response should be an empty JSON array")
    public void theResponseShouldBeAnEmptyJsonArray() {
        JsonNode node = world.json();
        assertThat(node.isArray()).as("response is a JSON array").isTrue();
        assertThat(node).isEmpty();
    }

    /** Dotted path to a JSON Pointer: {@code details.0.field} -> {@code /details/0/field}. */
    private static String toPointer(String dottedPath) {
        return "/" + dottedPath.replace('.', '/');
    }
}
