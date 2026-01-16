package com.example.gateway.controllers;

import com.example.gateway.dto.ErrorResponse;
import com.example.gateway.jobs.JobConstants;
import com.example.gateway.jobs.DeliverWebhookJob;
import com.example.gateway.jobs.JobServiceImpl;
import com.example.gateway.models.Merchant;
import com.example.gateway.models.WebhookLog;
import com.example.gateway.repositories.WebhookLogRepository;
import com.example.gateway.services.AuthenticationService;
import com.example.gateway.services.IDGeneratorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class WebhookController {
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private IDGeneratorService idGeneratorService;
    
    @Autowired
    private JobServiceImpl jobService;
    
    @Autowired
    private WebhookLogRepository webhookLogRepository;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * GET /api/v1/webhooks - List webhook logs with pagination (Authenticated)
     */
    @GetMapping("/api/v1/webhooks")
    public ResponseEntity<?> listWebhooks(
            @RequestHeader("X-Api-Key") String apiKey,
            @RequestHeader("X-Api-Secret") String apiSecret,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            // Authenticate merchant
            Merchant merchant = authenticationService.authenticateMerchant(apiKey, apiSecret);
            
            // Fetch paginated webhook logs
            Pageable pageable = PageRequest.of(offset / limit, limit);
            List<WebhookLog> logs = webhookLogRepository.findByMerchantId(merchant.getId());
            
            // Apply pagination manually (simple approach)
            int start = offset;
            int end = Math.min(start + limit, logs.size());
            List<WebhookLog> paginatedLogs = logs.subList(start, end);
            
            // Build response
            List<Map<String, Object>> data = new ArrayList<>();
            for (WebhookLog log : paginatedLogs) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", log.getId());
                item.put("event", log.getEvent());
                item.put("status", log.getStatus());
                item.put("attempts", log.getAttempts());
                item.put("created_at", log.getCreatedAt());
                item.put("last_attempt_at", log.getLastAttemptAt());
                item.put("response_code", log.getResponseCode());
                data.add(item);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            response.put("total", logs.size());
            response.put("limit", limit);
            response.put("offset", offset);
            
            return ResponseEntity.status(HttpStatus.OK).body(response);
            
        } catch (IllegalArgumentException e) {
            return handleIllegalArgument(e);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse("BAD_REQUEST_ERROR", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    /**
     * POST /api/v1/webhooks/{webhook_id}/retry - Retry webhook delivery (Authenticated)
     */
    @PostMapping("/api/v1/webhooks/{webhook_id}/retry")
    public ResponseEntity<?> retryWebhook(
            @RequestHeader("X-Api-Key") String apiKey,
            @RequestHeader("X-Api-Secret") String apiSecret,
            @PathVariable("webhook_id") String webhookId) {
        
        try {
            // Authenticate merchant
            Merchant merchant = authenticationService.authenticateMerchant(apiKey, apiSecret);
            
            // Fetch webhook log
            Optional<WebhookLog> webhookOpt = webhookLogRepository.findById(webhookId);
            if (!webhookOpt.isPresent()) {
                ErrorResponse errorResponse = new ErrorResponse("NOT_FOUND_ERROR", "Webhook not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            WebhookLog webhook = webhookOpt.get();
            
            // Verify merchant ownership
            if (!webhook.getMerchant().getId().equals(merchant.getId())) {
                ErrorResponse errorResponse = new ErrorResponse("BAD_REQUEST_ERROR", "Webhook does not belong to this merchant");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Reset webhook for retry
            webhook.setAttempts(0);
            webhook.setStatus(JobConstants.JOB_STATUS_PENDING);
            webhook.setNextRetryAt(OffsetDateTime.now());
            webhookLogRepository.save(webhook);
            
            // Enqueue DeliverWebhookJob
            String jobId = idGeneratorService.generateWebhookJobId();
            DeliverWebhookJob job = new DeliverWebhookJob(jobId, merchant.getId(), webhook.getEvent(), webhook.getPayload());
            jobService.enqueueJob(JobConstants.WEBHOOK_QUEUE, job, jobId);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("id", webhook.getId());
            response.put("status", webhook.getStatus());
            response.put("message", "Webhook retry scheduled");
            
            return ResponseEntity.status(HttpStatus.OK).body(response);
            
        } catch (IllegalArgumentException e) {
            return handleIllegalArgument(e);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse("BAD_REQUEST_ERROR", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    private ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        String message = e.getMessage();
        
        if (message == null) {
            ErrorResponse errorResponse = new ErrorResponse("BAD_REQUEST_ERROR", "Invalid request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        if (message.contains("|")) {
            String[] parts = message.split("\\|", 2);
            String code = parts[0];
            String description = parts[1];
            ErrorResponse errorResponse = new ErrorResponse(code, description);

            if (code.equals("AUTHENTICATION_ERROR")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            } else if (code.equals("NOT_FOUND_ERROR")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
        }

        ErrorResponse errorResponse = new ErrorResponse("BAD_REQUEST_ERROR", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
