package com.example.gateway.jobs;

public class JobConstants {
    
    // Queue names
    public static final String PAYMENT_QUEUE = "payment-jobs";
    public static final String WEBHOOK_QUEUE = "webhook-jobs";
    public static final String REFUND_QUEUE = "refund-jobs";
    
    // Job types
    public static final String PROCESS_PAYMENT_JOB = "process_payment";
    public static final String DELIVER_WEBHOOK_JOB = "deliver_webhook";
    public static final String PROCESS_REFUND_JOB = "process_refund";
    
    // Job statuses
    public static final String JOB_STATUS_PENDING = "pending";
    public static final String JOB_STATUS_PROCESSING = "processing";
    public static final String JOB_STATUS_COMPLETED = "completed";
    public static final String JOB_STATUS_FAILED = "failed";
    
    // Webhook retry intervals (production)
    public static final int[] WEBHOOK_RETRY_DELAYS_SECONDS = {0, 60, 300, 1800, 7200};
    
    // Webhook retry intervals (test mode)
    public static final int[] WEBHOOK_RETRY_DELAYS_SECONDS_TEST = {0, 5, 10, 15, 20};
    
    // Max retry attempts
    public static final int MAX_WEBHOOK_ATTEMPTS = 5;
}
