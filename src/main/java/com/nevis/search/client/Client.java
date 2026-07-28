package com.nevis.search.client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "clients")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column
    private String description;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "social_links", columnDefinition = "text[]", nullable = false)
    private String[] socialLinks = new String[0];

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // search_blob is a STORED generated column, populated by Postgres; it is never
    // written from Java and is only read by the native search queries.

    public Client(String firstName, String lastName, String email,
                  String description, String[] socialLinks) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.description = description;
        this.socialLinks = socialLinks == null ? new String[0] : socialLinks;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
