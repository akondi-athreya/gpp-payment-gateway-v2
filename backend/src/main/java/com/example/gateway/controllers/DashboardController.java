package com.example.gateway.controllers;

import com.example.gateway.dto.ErrorResponse;
import com.example.gateway.models.Merchant;
import com.example.gateway.repositories.MerchantRepository;
import com.example.gateway.services.AuthenticationService;
import com.example.gateway.services.IDGeneratorService;
import com.example.gateway.services.ValidationService;
import com.example.gateway.services.WebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class DashboardController {

    private final AuthenticationService authenticationService;
    private final ValidationService validationService;
    private final MerchantRepository merchantRepository;
    private final IDGeneratorService idGeneratorService;
    private final WebhookService webhookService;
    private static final ObjectMapper mapper = new ObjectMapper();

    public DashboardController(AuthenticationService authenticationService,
                               ValidationService validationService,
                               MerchantRepository merchantRepository,
                               IDGeneratorService idGeneratorService,
                               WebhookService webhookService) {
        this.authenticationService = authenticationService;
        this.validationService = validationService;
        this.merchantRepository = merchantRepository;
        this.idGeneratorService = idGeneratorService;
        this.webhookService = webhookService;
    }

    @GetMapping("/dashboard/webhooks")
    public String webhooksPage() {
        return "dashboard-webhooks";
    }

    @GetMapping("/dashboard/docs")
    public String docsPage() {
        return "dashboard-docs";
    }

    @PostMapping("/dashboard/api/webhooks/config")
    @ResponseBody
    public ResponseEntity<?> updateWebhookConfig(
            @RequestHeader("X-Api-Key") String apiKey,
            @RequestHeader("X-Api-Secret") String apiSecret,
            @RequestBody ObjectNode body) {
        try {
            Merchant merchant = authenticationService.authenticateMerchant(apiKey, apiSecret);
            String url = body.hasNonNull("webhook_url") ? body.get("webhook_url").asText() : null;

            if (!validationService.validateWebhookUrl(url)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("BAD_REQUEST_ERROR", "Invalid webhook_url"));
            }

            merchant.setWebhookUrl(url);
            merchantRepository.save(merchant);

            ObjectNode resp = mapper.createObjectNode();
            resp.put("webhook_url", merchant.getWebhookUrl());
            return ResponseEntity.status(HttpStatus.OK).body(resp);
        } catch (IllegalArgumentException e) {
            return handleIllegalArgument(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/dashboard/api/webhooks/secret/regenerate")
    @ResponseBody
    public ResponseEntity<?> regenerateSecret(
            @RequestHeader("X-Api-Key") String apiKey,
            @RequestHeader("X-Api-Secret") String apiSecret) {
        try {
            Merchant merchant = authenticationService.authenticateMerchant(apiKey, apiSecret);
            String newSecret = idGeneratorService.generateRandomString(32);
            merchant.setWebhookSecret(newSecret);
            merchantRepository.save(merchant);

            ObjectNode resp = mapper.createObjectNode();
            resp.put("webhook_secret", newSecret);
            return ResponseEntity.status(HttpStatus.OK).body(resp);
        } catch (IllegalArgumentException e) {
            return handleIllegalArgument(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/dashboard/api/webhooks/test")
    @ResponseBody
    public ResponseEntity<?> sendTestWebhook(
            @RequestHeader("X-Api-Key") String apiKey,
            @RequestHeader("X-Api-Secret") String apiSecret) {
        try {
            Merchant merchant = authenticationService.authenticateMerchant(apiKey, apiSecret);
            ObjectNode payload = mapper.createObjectNode();
            payload.put("event", "webhook.test");
            payload.put("message", "Test webhook from dashboard");
            payload.put("timestamp", System.currentTimeMillis());

            String logId = webhookService.enqueueCustomWebhook(merchant.getId(), "webhook.test", payload);

            ObjectNode resp = mapper.createObjectNode();
            resp.put("webhook_log_id", logId);
            return ResponseEntity.status(HttpStatus.OK).body(resp);
        } catch (IllegalArgumentException e) {
            return handleIllegalArgument(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST_ERROR", e.getMessage()));
        }
    }

    private ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        String message = e.getMessage();
        if (message == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST_ERROR", "Invalid request"));
        }
        if (message.contains("|")) {
            String[] parts = message.split("\\|", 2);
            String code = parts[0];
            String description = parts[1];
            HttpStatus status = HttpStatus.BAD_REQUEST;
            if (code.equals("AUTHENTICATION_ERROR")) {
                status = HttpStatus.UNAUTHORIZED;
            } else if (code.equals("NOT_FOUND_ERROR")) {
                status = HttpStatus.NOT_FOUND;
            }
            return ResponseEntity.status(status).body(new ErrorResponse(code, description));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST_ERROR", message));
    }
}
