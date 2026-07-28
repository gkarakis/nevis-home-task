package com.nevis.search.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-scenario state shared across step-definition classes: it issues the HTTP calls,
 * holds the last response, and remembers ids created earlier in the same scenario
 * (e.g. a client id needed by a later document step). A fresh instance is created for
 * every scenario, so scenarios never leak state into one another.
 *
 * <p>{@link TestRestTemplate} is already bound to the app's random port and does not
 * throw on 4xx/5xx, so error responses are asserted like any other.
 */
@Component
@ScenarioScope
public class World {

    private final TestRestTemplate rest;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, UUID> clientIds = new HashMap<>();

    private ResponseEntity<String> response;

    @Autowired
    World(TestRestTemplate rest) {
        this.rest = rest;
    }

    void post(String path, String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        response = rest.postForEntity(path, new HttpEntity<>(jsonBody, headers), String.class);
    }

    void get(String path, Object... uriVariables) {
        response = rest.getForEntity(path, String.class, uriVariables);
    }

    int status() {
        return response.getStatusCode().value();
    }

    String body() {
        return response.getBody();
    }

    JsonNode json() {
        try {
            return mapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Response body was not JSON: " + response.getBody(), e);
        }
    }

    void rememberClient(String alias, UUID id) {
        clientIds.put(alias, id);
    }

    UUID clientId(String alias) {
        UUID id = clientIds.get(alias);
        if (id == null) {
            throw new IllegalStateException("No client remembered under alias '" + alias + "'");
        }
        return id;
    }
}
