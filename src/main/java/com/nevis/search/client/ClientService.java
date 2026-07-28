package com.nevis.search.client;

import com.nevis.search.client.dto.ClientResponse;
import com.nevis.search.client.dto.CreateClientRequest;
import com.nevis.search.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    @Transactional
    public ClientResponse create(CreateClientRequest req) {
        // Cheap pre-check for a friendly message; the unique index is the real guard
        // and is enforced even under a race (handled in the exception handler).
        if (clientRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS",
                    "A client with email '" + req.email() + "' already exists");
        }
        String[] links = req.socialLinks() == null
                ? new String[0]
                : req.socialLinks().toArray(new String[0]);
        Client client = new Client(
                req.firstName().strip(),
                req.lastName().strip(),
                req.email().strip(),
                req.description(),
                links);
        try {
            return ClientResponse.from(clientRepository.saveAndFlush(client));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS",
                    "A client with email '" + req.email() + "' already exists");
        }
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {
        return clientRepository.findAll().stream()
                .map(ClientResponse::from)
                .toList();
    }
}
