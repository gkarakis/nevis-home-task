package com.nevis.search.document;

import com.nevis.search.common.exception.EmbeddingUnavailableException;
import com.nevis.search.common.exception.NotFoundException;
import com.nevis.search.document.dto.DocumentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean DocumentService documentService;

    private static final UUID CLIENT_ID = UUID.randomUUID();

    @Test
    void validDocumentReturns201WithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.create(eq(CLIENT_ID), any())).thenReturn(
                new DocumentResponse(id, CLIENT_ID, "Utility bill", 3, Instant.now()));

        mockMvc.perform(post("/clients/{clientId}/documents", CLIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Utility bill","content":"Electricity bill for January 2026."}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "http://localhost/clients/" + CLIENT_ID + "/documents/" + id))
                .andExpect(jsonPath("$.title").value("Utility bill"))
                .andExpect(jsonPath("$.chunk_count").value(3));
    }

    @Test
    void embeddingFailureReturns503() throws Exception {
        when(documentService.create(eq(CLIENT_ID), any()))
                .thenThrow(new EmbeddingUnavailableException("model down", new RuntimeException()));

        mockMvc.perform(post("/clients/{clientId}/documents", CLIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Utility bill","content":"Electricity bill for January 2026."}"""))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EMBEDDING_UNAVAILABLE"));
    }

    @Test
    void unknownClientReturns404() throws Exception {
        when(documentService.create(eq(CLIENT_ID), any()))
                .thenThrow(new NotFoundException("CLIENT_NOT_FOUND", "no such client"));

        mockMvc.perform(post("/clients/{clientId}/documents", CLIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Orphan","content":"Points at a client that does not exist."}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));
    }

    @Test
    void blankContentReturns400WithFieldDetails() throws Exception {
        mockMvc.perform(post("/clients/{clientId}/documents", CLIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Utility bill","content":"   "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("content"));
    }
}
