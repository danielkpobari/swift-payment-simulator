package com.baghadom.cross_border_payment_simulator.service;

import com.baghadom.cross_border_payment_simulator.repository.LiquidityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class LiquidityService {

    private final LiquidityRepository repo;

    public void checkLiquidity(String currency, BigDecimal amount) {
        BigDecimal available = repo.findById(currency)
                .orElseThrow(() -> new RuntimeException("No liquidity pool for " + currency))
                .getAvailableBalance();

        if (available.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient liquidity for " + currency);
        }
    }
}
