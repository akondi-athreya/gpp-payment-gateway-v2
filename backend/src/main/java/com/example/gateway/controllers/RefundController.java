package com.example.gateway.controllers;

import com.example.gateway.dto.ErrorResponse;
import com.example.gateway.dto.RefundResponse;
import com.example.gateway.jobs.JobConstants;
import com.example.gateway.jobs.ProcessRefundJob;
import com.example.gateway.models.Merchant;
import com.example.gateway.models.Payment;
import com.example.gateway.models.Refund;
import com.example.gateway.repositories.PaymentRepository;
import com.example.gateway.repositories.RefundRepository;
import com.example.gateway.services.AuthenticationService;
import com.example.gateway.services.IDGeneratorService;
import com.example.gateway.jobs.JobServiceImpl;
import com.example.gateway.jobs.JobConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class RefundController {
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private IDGeneratorService idGeneratorService;
    
    @Autowired
    private JobServiceImpl jobService;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private RefundRepository refundRepository;
    
    /**
     * POST /api/v1/payments/{payment_id}/refunds - Create refund (Authenticated)
     */
    @PostMapping("/api/v1/payments/{payment_id}/refunds")
    public ResponseEntity<?> createRefund(
            @RequestHeader("X-Api-Key") String apiKey,
            @RequestHeader("X-Api-Secret") String apiSecret,
            @PathVariable("payment_id") String paymentId,
            @RequestBody Map<String, Object> request) {
        
        try {
            // Authenticate merchant
            Merchant merchant = authenticationService.authenticateMerchant(apiKey, apiSecret);
            
            // Fetch payment
            Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
            if (!paymentOpt.isPresent()) {
                ErrorResponse errorResponse = new ErrorResponse("NOT_FOUND_ERROR", "Payment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            Payment payment = paymentOpt.get();
            
            // Verify merchant ownership
            if (!payment.getMerchant().getId().equals(merchant.getId())) {
                ErrorResponse errorResponse = new ErrorResponse("BAD_REQUEST_ERROR", "Payment does not belong to this merchant");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Verify payment is refundable (status must be 'success')
            if (!payment.getStatus().equals("success")) {
                ErrorResponse errorResponse = new ErrorResponse("BAD_REQUEST_ERROR", "Refund amount exceeds available amount");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Extract refund amount and reason
            if (!request.containsKey("amount")) {
                ErrorResponse errorResponse = new ErrorResponse("BAD_REQUEST_ERROR", "amount is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            Long refundAmount = Long.valueOf(request.get("amount").toString());
            String reason = request.containsKey("reason") ? request.get("reason").toString() : null;
            
            // Calculate total already refunded
            List<Refund> existingRefunds = refundRepository.findByPaymentId(paymentId);
            long totalRefunded = 0;
            for (Refund r : existingRefunds) {
                if (!"failed".equals(r.getStatus())) {
                    totalRefunded += r.getAmount();
                }
            }
            
            // Validate refund amount
            if (refundAmount + totalRefunded > payment.getAmount()) {
                ErrorResponse errorResponse = new ErrorResponse("BAD_REQUEST_ERROR", "Refund amount exceeds available amount");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Generate refund ID
            String refundId = idGeneratorService.generateRefundId();
            
            // Create refund record
            Refund refund = new Refund();
            refund.setId(refundId);
            refund.setPayment(payment);
            refund.setMerchant(merchant);
            refund.setAmount(refundAmount);
            refund.setReason(reason);
            refund.setStatus("pending");
            refund.setCreatedAt(OffsetDateTime.now());
            
            refundRepository.save(refund);
            
            // Enqueue ProcessRefundJob
            String jobId = "job_" + idGeneratorService.generateRandomString(12);
            ProcessRefundJob job = new ProcessRefundJob(jobId, refundId);
            jobService.enqueueJob(JobConstants.REFUND_QUEUE, job, jobId);
            
            // Build response
            RefundResponse response = mapRefundToResponse(refund);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            return handleIllegalArgument(e);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse("BAD_REQUEST_ERROR", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    /**
     * GET /api/v1/refunds/{refund_id} - Get refund details (Authenticated)
     */
    @GetMapping("/api/v1/refunds/{refund_id}")
    public ResponseEntity<?> getRefund(
            @RequestHeader("X-Api-Key") String apiKey,
            @RequestHeader("X-Api-Secret") String apiSecret,
            @PathVariable("refund_id") String refundId) {
        
        try {
            // Authenticate merchant
            Merchant merchant = authenticationService.authenticateMerchant(apiKey, apiSecret);
            
            // Fetch refund
            Optional<Refund> refundOpt = refundRepository.findById(refundId);
            if (!refundOpt.isPresent()) {
                ErrorResponse errorResponse = new ErrorResponse("NOT_FOUND_ERROR", "Refund not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
            Refund refund = refundOpt.get();
            
            // Verify merchant ownership
            if (!refund.getMerchant().getId().equals(merchant.getId())) {
                ErrorResponse errorResponse = new ErrorResponse("BAD_REQUEST_ERROR", "Refund does not belong to this merchant");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Build response
            RefundResponse response = mapRefundToResponse(refund);
            return ResponseEntity.status(HttpStatus.OK).body(response);
            
        } catch (IllegalArgumentException e) {
            return handleIllegalArgument(e);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse("BAD_REQUEST_ERROR", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    private RefundResponse mapRefundToResponse(Refund refund) {
        RefundResponse response = new RefundResponse();
        response.setId(refund.getId());
        response.setPaymentId(refund.getPayment().getId());
        response.setAmount(refund.getAmount());
        response.setReason(refund.getReason());
        response.setStatus(refund.getStatus());
        response.setCreatedAt(refund.getCreatedAt());
        response.setProcessedAt(refund.getProcessedAt());
        return response;
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
