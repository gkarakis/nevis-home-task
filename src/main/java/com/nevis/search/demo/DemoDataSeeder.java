package com.nevis.search.demo;

import com.nevis.search.client.ClientRepository;
import com.nevis.search.client.ClientService;
import com.nevis.search.client.dto.ClientResponse;
import com.nevis.search.client.dto.CreateClientRequest;
import com.nevis.search.document.DocumentService;
import com.nevis.search.document.dto.CreateDocumentRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the reviewer's first thirty seconds. Guarded by the {@code demo} profile
 * and a row-count check, and run as an {@link ApplicationRunner} rather than a SQL
 * migration because chunk embeddings cannot be computed in SQL.
 *
 * <p>Every seed row exercises a specific behaviour called out in the plan:
 * the {@code NevisWealth} substring, the {@code address proof} semantic case, the
 * apostrophe/accent normalisation cases, the {@code ACC-889134} identifier case,
 * and an unrelated portfolio note so the relevance floor is visibly doing work.
 */
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final ClientRepository clientRepository;
    private final ClientService clientService;
    private final DocumentService documentService;

    @Override
    public void run(ApplicationArguments args) {
        if (clientRepository.count() > 0) {
            log.info("Demo data already present ({} clients); skipping seed.",
                    clientRepository.count());
            return;
        }
        log.info("Seeding demo data...");

        // 1. John Doe — the "NevisWealth" substring inside the email, plus the
        //    address-proof document and an unrelated note for the floor.
        ClientResponse john = clientService.create(new CreateClientRequest(
                "John", "Doe", "john.doe@neviswealth.com",
                "Long-standing wealth management client.",
                List.of("https://www.linkedin.com/in/johndoe")));
        documentService.create(john.id(), new CreateDocumentRequest(
                "John's utility bill",
                "Electricity bill for 123 Main Street dated January 2026. "
                        + "Account holder John Doe. Total amount due 84.20 GBP."));
        documentService.create(john.id(), new CreateDocumentRequest(
                "Portfolio note",
                "Quarterly portfolio review. Equities up three percent, bonds flat. "
                        + "No rebalancing action required this quarter."));

        // 2. Jack O'Hara — curly apostrophe; and the ACC-889134 identifier document.
        ClientResponse jack = clientService.create(new CreateClientRequest(
                "Jack", "O’Hara", "jack.ohara@example.com",
                "Onboarded through the referral programme.",
                List.of()));
        documentService.create(jack.id(), new CreateDocumentRequest(
                "Account reference",
                "Client account ACC-889134 was opened on 2025-11-02 during onboarding."));

        // 3. José Álvarez — accented name; passport document whose body barely uses
        //    the word, proving the title-prefixed embedding surfaces it.
        ClientResponse jose = clientService.create(new CreateClientRequest(
                "José", "Álvarez", "jose.alvarez@example.com",
                "Prefers correspondence in Spanish.",
                List.of()));
        documentService.create(jose.id(), new CreateDocumentRequest(
                "Passport Copy",
                "Identity verification document held on file for compliance purposes."));

        log.info("Demo data seeded: {} clients.", clientRepository.count());
    }
}
