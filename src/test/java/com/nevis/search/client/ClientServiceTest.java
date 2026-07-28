package com.nevis.search.client;

import com.nevis.search.client.dto.CreateClientRequest;
import com.nevis.search.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock ClientRepository clientRepository;

    @InjectMocks ClientService clientService;

    @Test
    void duplicateEmailPreCheckFailsBeforeSaving() {
        CreateClientRequest request = new CreateClientRequest(
                "John", "Doe", "john@example.com", null, null);
        when(clientRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> clientService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("john@example.com");

        verify(clientRepository, never()).saveAndFlush(any());
    }

    @Test
    void createTrimsIdentityFieldsAndDefaultsMissingSocialLinks() {
        CreateClientRequest request = new CreateClientRequest(
                "  Ada  ",
                "  Lovelace ",
                " ada.lovelace@example.com ",
                "First programmer",
                null);
        when(clientRepository.saveAndFlush(any(Client.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        clientService.create(request);

        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).saveAndFlush(captor.capture());
        Client saved = captor.getValue();
        assertThat(saved.getFirstName()).isEqualTo("Ada");
        assertThat(saved.getLastName()).isEqualTo("Lovelace");
        assertThat(saved.getEmail()).isEqualTo("ada.lovelace@example.com");
        assertThat(saved.getDescription()).isEqualTo("First programmer");
        assertThat(saved.getSocialLinks()).isEmpty();
    }

    @Test
    void dataIntegrityViolationDuringSaveIsReportedAsDuplicateEmail() {
        CreateClientRequest request = new CreateClientRequest(
                "John",
                "Doe",
                "john@example.com",
                null,
                List.of("https://example.com/john"));
        when(clientRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(false);
        when(clientRepository.saveAndFlush(any(Client.class)))
                .thenThrow(new DataIntegrityViolationException("unique index"));

        assertThatThrownBy(() -> clientService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("john@example.com");
    }
}
