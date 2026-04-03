package com.baghadom.cross_border_payment_simulator.repository;

import com.baghadom.cross_border_payment_simulator.entity.SwiftMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SwiftMessageRepository extends JpaRepository<SwiftMessage, UUID> {
    List<SwiftMessage> findByTxnIdOrderBySentAtAsc(UUID txnId);
}
