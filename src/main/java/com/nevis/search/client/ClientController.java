package com.nevis.search.client;

import com.nevis.search.client.dto.ClientResponse;
import com.nevis.search.client.dto.CreateClientRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/clients")
@Tag(name = "Clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    @Operation(summary = "Create a client",
            description = "201 with Location header; 400 on validation; 409 on duplicate email")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody CreateClientRequest req,
                                                 UriComponentsBuilder uriBuilder) {
        ClientResponse created = clientService.create(req);
        URI location = uriBuilder.path("/clients/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List all clients")
    public List<ClientResponse> list() {
        return clientService.findAll();
    }
}
