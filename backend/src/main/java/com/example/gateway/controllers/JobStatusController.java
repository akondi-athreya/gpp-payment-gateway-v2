package com.example.gateway.controllers;

import com.example.gateway.jobs.JobServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class JobStatusController {
    
    @Autowired
    private JobServiceImpl jobService;
    
    /**
     * GET /api/v1/test/jobs/status - Get job queue statistics (NO AUTHENTICATION REQUIRED)
     * Test endpoint for evaluation purposes
     */
    @GetMapping("/api/v1/test/jobs/status")
    public ResponseEntity<?> getJobStatus() {
        try {
            // Get job queue statistics from Redis
            long pendingCount = jobService.getPendingJobsCount();
            long processingCount = jobService.getProcessingJobsCount();
            long completedCount = jobService.getCompletedJobsCount();
            long failedCount = jobService.getFailedJobsCount();
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("pending", pendingCount);
            response.put("processing", processingCount);
            response.put("completed", completedCount);
            response.put("failed", failedCount);
            response.put("worker_status", "running");
            
            return ResponseEntity.status(HttpStatus.OK).body(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
