package com.example.gateway.repositories;

import com.example.gateway.models.Payment;
import com.example.gateway.models.Refund;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundRepository extends JpaRepository<Refund, String> {
    List<Refund> findByPaymentId(String paymentId);
    List<Refund> findByMerchantId(java.util.UUID merchantId);
    List<Refund> findByStatus(String status);
    List<Refund> findByPayment(Payment payment);
}
