package com.nevis.search.client;

import com.nevis.search.client.dto.ClientResponse;
import com.nevis.search.client.dto.CreateClientRequest;
import com.nevis.search.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ClientService clientService;

    @Test
    void validClientReturns201WithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(clientService.create(any())).thenReturn(new ClientResponse(
                id, "John", "Doe", "john.doe@neviswealth.com", null, List.of(), Instant.now()));

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"first_name":"John","last_name":"Doe",
                                 "email":"john.doe@neviswealth.com"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/clients/" + id))
                .andExpect(jsonPath("$.first_name").value("John"))
                .andExpect(jsonPath("$.email").value("john.doe@neviswealth.com"));
    }

    @Test
    void invalidEmailReturns400WithFieldDetails() throws Exception {
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"first_name":"John","last_name":"Doe","email":"not-an-email"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("email"));
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        when(clientService.create(any(CreateClientRequest.class)))
                .thenThrow(new ConflictException("EMAIL_ALREADY_EXISTS", "duplicate"));

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"first_name":"John","last_name":"Doe",
                                 "email":"john.doe@neviswealth.com"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }
}
