package com.example.gateway.repositories;

import com.example.gateway.models.IdempotencyKey;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {
    @Query("SELECT i FROM IdempotencyKey i WHERE i.key = :key AND i.merchant.id = :merchantId AND i.expiresAt > :now")
    Optional<IdempotencyKey> findValidByKeyAndMerchant(@Param("key") String key, @Param("merchantId") UUID merchantId, @Param("now") OffsetDateTime now);
    
    Optional<IdempotencyKey> findByKeyAndMerchantId(String key, UUID merchantId);
}
