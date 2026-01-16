package com.example.gateway.repositories;

import com.example.gateway.models.WebhookLog;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, String> {
    List<WebhookLog> findByMerchantId(UUID merchantId);
    List<WebhookLog> findByStatus(String status);
    List<WebhookLog> findByMerchantIdAndStatus(UUID merchantId, String status);
    List<WebhookLog> findByEvent(String event);
    
    @Query("SELECT w FROM WebhookLog w WHERE w.status = 'pending' AND w.nextRetryAt IS NOT NULL AND w.nextRetryAt <= :now ORDER BY w.nextRetryAt ASC")
    List<WebhookLog> findPendingRetries(@Param("now") OffsetDateTime now);
    
    List<WebhookLog> findByMerchantIdAndEvent(UUID merchantId, String event);
}
