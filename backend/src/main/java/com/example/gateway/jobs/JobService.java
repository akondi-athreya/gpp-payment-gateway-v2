package com.example.gateway.jobs;

import java.io.Serializable;

public interface JobService {
    
    /**
     * Enqueue a job in the specified queue
     * @param queueName Name of the queue
     * @param jobData Job data to be queued
     * @param jobId Unique job identifier
     * @return Job ID for tracking
     */
    String enqueueJob(String queueName, Serializable jobData, String jobId);
    
    /**
     * Get job status by job ID
     * @param jobId Job identifier
     * @return Job status (pending, processing, completed, failed)
     */
    String getJobStatus(String jobId);
    
    /**
     * Get total pending jobs count
     * @return Number of pending jobs across all queues
     */
    long getPendingJobsCount();
    
    /**
     * Get total processing jobs count
     * @return Number of jobs currently being processed
     */
    long getProcessingJobsCount();
    
    /**
     * Get total completed jobs count
     * @return Number of completed jobs
     */
    long getCompletedJobsCount();
    
    /**
     * Get total failed jobs count
     * @return Number of failed jobs
     */
    long getFailedJobsCount();
    
    /**
     * Check if worker service is running
     * @return true if worker is active, false otherwise
     */
    boolean isWorkerRunning();
}
