package com.baghadom.cross_border_payment_simulator.repository;

import com.baghadom.cross_border_payment_simulator.entity.LiquidityPool;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiquidityRepository extends JpaRepository<LiquidityPool, String> {}
