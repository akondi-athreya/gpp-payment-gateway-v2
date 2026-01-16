package com.example.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RefundResponse {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("payment_id")
    private String paymentId;
    
    @JsonProperty("amount")
    private Long amount;
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    
    @JsonProperty("processed_at")
    private OffsetDateTime processedAt;

    public RefundResponse() {
    }

    public RefundResponse(String id, String paymentId, Long amount, String reason, String status, 
                         OffsetDateTime createdAt, OffsetDateTime processedAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.amount = amount;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }

    @Override
    public String toString() {
        return "RefundResponse [id=" + id + ", paymentId=" + paymentId + ", amount=" + amount + ", reason=" + reason
                + ", status=" + status + ", createdAt=" + createdAt + ", processedAt=" + processedAt + "]";
    }
}
