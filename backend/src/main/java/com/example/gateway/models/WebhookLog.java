package com.example.gateway.models;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "webhook_logs", indexes = {
    @Index(name = "idx_webhook_logs_merchant_id", columnList = "merchant_id"),
    @Index(name = "idx_webhook_logs_status", columnList = "status"),
    @Index(name = "idx_webhook_logs_next_retry", columnList = "next_retry_at")
})
public class WebhookLog {
    
    @Id
    @Column(columnDefinition = "varchar(255)")
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "merchant_id", referencedColumnName = "id", nullable = false)
    private Merchant merchant;
    
    @Column(nullable = false, length = 50)
    private String event;
    
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode payload;
    
    @Column(nullable = false, length = 20)
    private String status = "pending";
    
    @Column(nullable = false)
    private Integer attempts = 0;
    
    @Column(name = "last_attempt_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime lastAttemptAt;
    
    @Column(name = "next_retry_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime nextRetryAt;
    
    @Column(name = "response_code")
    private Integer responseCode;
    
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;
    
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    public WebhookLog() {
    }

    public WebhookLog(String id, Merchant merchant, String event, JsonNode payload, String status,
            Integer attempts, OffsetDateTime lastAttemptAt, OffsetDateTime nextRetryAt, Integer responseCode,
            String responseBody, OffsetDateTime createdAt) {
        this.id = id;
        this.merchant = merchant;
        this.event = event;
        this.payload = payload;
        this.status = status;
        this.attempts = attempts;
        this.lastAttemptAt = lastAttemptAt;
        this.nextRetryAt = nextRetryAt;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.status == null) {
            this.status = "pending";
        }
        if (this.attempts == null) {
            this.attempts = 0;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public OffsetDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(OffsetDateTime lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public OffsetDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(OffsetDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "WebhookLog [id=" + id + ", merchant=" + merchant + ", event=" + event + ", payload=" + payload
                + ", status=" + status + ", attempts=" + attempts + ", lastAttemptAt=" + lastAttemptAt
                + ", nextRetryAt=" + nextRetryAt + ", responseCode=" + responseCode + ", responseBody="
                + responseBody + ", createdAt=" + createdAt + "]";
    }
}
