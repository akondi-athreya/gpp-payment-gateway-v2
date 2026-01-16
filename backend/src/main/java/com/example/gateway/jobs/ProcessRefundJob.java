package com.example.gateway.jobs;

import java.io.Serializable;
import java.time.OffsetDateTime;

public class ProcessRefundJob implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String jobId;
    private String refundId;
    private OffsetDateTime createdAt;
    private String status;

    public ProcessRefundJob() {
    }

    public ProcessRefundJob(String jobId, String refundId) {
        this.jobId = jobId;
        this.refundId = refundId;
        this.createdAt = OffsetDateTime.now();
        this.status = JobConstants.JOB_STATUS_PENDING;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getRefundId() {
        return refundId;
    }

    public void setRefundId(String refundId) {
        this.refundId = refundId;
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
        return "ProcessRefundJob [jobId=" + jobId + ", refundId=" + refundId + ", createdAt=" + createdAt
                + ", status=" + status + "]";
    }
}
