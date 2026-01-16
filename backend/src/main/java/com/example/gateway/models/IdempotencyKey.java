package com.example.gateway.models;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"key", "merchant_id"})
})
public class IdempotencyKey {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(nullable = false, length = 255)
    private String key;
    
    @ManyToOne
    @JoinColumn(name = "merchant_id", referencedColumnName = "id", nullable = false)
    private Merchant merchant;
    
    @Column(name = "response", nullable = false, columnDefinition = "jsonb")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode response;
    
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime expiresAt;

    public IdempotencyKey() {
    }

    public IdempotencyKey(String key, Merchant merchant, JsonNode response, OffsetDateTime createdAt,
            OffsetDateTime expiresAt) {
        this.key = key;
        this.merchant = merchant;
        this.response = response;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.expiresAt == null) {
            // Default to 24 hours from creation
            this.expiresAt = this.createdAt.plusHours(24);
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public JsonNode getResponse() {
        return response;
    }

    public void setResponse(JsonNode response) {
        this.response = response;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public String toString() {
        return "IdempotencyKey [id=" + id + ", key=" + key + ", merchant=" + merchant + ", response=" + response
                + ", createdAt=" + createdAt + ", expiresAt=" + expiresAt + "]";
    }
}
