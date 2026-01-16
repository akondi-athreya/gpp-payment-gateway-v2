package com.example.gateway.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

public class DeliverWebhookJob implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String jobId;
    private UUID merchantId;
    private String event;
    private JsonNode payload;
    private OffsetDateTime createdAt;
    private String status;

    public DeliverWebhookJob() {
    }

    public DeliverWebhookJob(String jobId, UUID merchantId, String event, JsonNode payload) {
        this.jobId = jobId;
        this.merchantId = merchantId;
        this.event = event;
        this.payload = payload;
        this.createdAt = OffsetDateTime.now();
        this.status = JobConstants.JOB_STATUS_PENDING;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "DeliverWebhookJob [jobId=" + jobId + ", merchantId=" + merchantId + ", event=" + event
                + ", createdAt=" + createdAt + ", status=" + status + "]";
    }
}
