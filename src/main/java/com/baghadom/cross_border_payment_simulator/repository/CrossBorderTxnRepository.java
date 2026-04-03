package com.baghadom.cross_border_payment_simulator.repository;

import com.baghadom.cross_border_payment_simulator.entity.CrossBorderTxn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CrossBorderTxnRepository extends JpaRepository<CrossBorderTxn, UUID> {}
