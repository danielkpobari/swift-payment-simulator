package com.baghadom.cross_border_payment_simulator.service;

import com.baghadom.cross_border_payment_simulator.entity.ArbitrageOpportunity;
import com.baghadom.cross_border_payment_simulator.repository.ArbitrageRepository;
import com.baghadom.cross_border_payment_simulator.repository.FxRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArbitrageEngine {

    private final FxRateRepository fxRateRepository;
    private final ArbitrageRepository arbitrageRepository;

    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal THRESHOLD = new BigDecimal("0.001"); // 0.1% min profit

    // Triangular paths to check (all via USD as base)
    private static final String[][] TRIANGLES = {
            {"USD", "EUR", "GBP", "USD"},
            {"USD", "GBP", "EUR", "USD"},
            {"USD", "NGN", "GBP", "USD"},
    };

    @Scheduled(fixedRate = 5000)
    public void scan() {
        for (String[] triangle : TRIANGLES) {
            checkTriangle(triangle);
        }
    }

    private void checkTriangle(String[] path) {
        BigDecimal product = ONE;
        StringBuilder pathStr = new StringBuilder();

        for (int i = 0; i < path.length - 1; i++) {
            String pair = path[i] + "_" + path[i + 1];
            pathStr.append(path[i]).append("→");

            var rateOpt = fxRateRepository.findById(pair);
            if (rateOpt.isEmpty()) return;

            product = product.multiply(rateOpt.get().getRate(), MathContext.DECIMAL64);
        }
        pathStr.append(path[path.length - 1]);

        BigDecimal profit = product.subtract(ONE).setScale(6, RoundingMode.HALF_UP);

        if (profit.compareTo(THRESHOLD) > 0) {
            ArbitrageOpportunity opp = new ArbitrageOpportunity();
            opp.setPath(pathStr.toString());
            opp.setProfitPct(profit);
            opp.setStartRate(ONE);
            opp.setEndRate(product.setScale(6, RoundingMode.HALF_UP));
            opp.setExploited(false);
            arbitrageRepository.save(opp);
            log.info("Arbitrage detected: {} | profit: {}%", pathStr, profit.multiply(BigDecimal.valueOf(100)));
        }
    }

    public List<ArbitrageOpportunity> getRecent() {
        return arbitrageRepository.findTop20ByOrderByDetectedAtDesc();
    }
}
