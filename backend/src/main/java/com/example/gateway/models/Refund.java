package com.example.gateway.models;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "refunds", indexes = {
    @Index(name = "idx_refunds_payment_id", columnList = "payment_id")
})
public class Refund {
    
    @Id
    @Column(nullable = false, length = 64)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "payment_id", referencedColumnName = "id", nullable = false)
    private Payment payment;
    
    @ManyToOne
    @JoinColumn(name = "merchant_id", referencedColumnName = "id", nullable = false)
    private Merchant merchant;
    
    @Column(nullable = false)
    private Long amount;
    
    @Column(columnDefinition = "TEXT")
    private String reason;
    
    @Column(nullable = false, length = 20)
    private String status = "pending";
    
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;
    
    @Column(name = "processed_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime processedAt;

    public Refund() {
    }

    public Refund(String id, Payment payment, Merchant merchant, Long amount, String reason, String status,
            OffsetDateTime createdAt, OffsetDateTime processedAt) {
        this.id = id;
        this.payment = payment;
        this.merchant = merchant;
        this.amount = amount;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.status == null) {
            this.status = "pending";
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
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
        return "Refund [id=" + id + ", payment=" + payment + ", merchant=" + merchant + ", amount=" + amount
                + ", reason=" + reason + ", status=" + status + ", createdAt=" + createdAt + ", processedAt="
                + processedAt + "]";
    }
}
