package com.example.gateway.jobs;

import java.io.Serializable;
import java.time.OffsetDateTime;

public class ProcessPaymentJob implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String jobId;
    private String paymentId;
    private OffsetDateTime createdAt;
    private String status;

    public ProcessPaymentJob() {
    }

    public ProcessPaymentJob(String jobId, String paymentId) {
        this.jobId = jobId;
        this.paymentId = paymentId;
        this.createdAt = OffsetDateTime.now();
        this.status = JobConstants.JOB_STATUS_PENDING;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
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
        return "ProcessPaymentJob [jobId=" + jobId + ", paymentId=" + paymentId + ", createdAt=" + createdAt
                + ", status=" + status + "]";
    }
}
