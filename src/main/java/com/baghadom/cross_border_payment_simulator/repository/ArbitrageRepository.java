package com.baghadom.cross_border_payment_simulator.repository;

import com.baghadom.cross_border_payment_simulator.entity.ArbitrageOpportunity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArbitrageRepository extends JpaRepository<ArbitrageOpportunity, UUID> {
    List<ArbitrageOpportunity> findTop20ByOrderByDetectedAtDesc();
}
