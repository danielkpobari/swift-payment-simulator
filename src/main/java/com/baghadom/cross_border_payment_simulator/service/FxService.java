package com.baghadom.cross_border_payment_simulator.service;

import com.baghadom.cross_border_payment_simulator.entity.FxRate;
import com.baghadom.cross_border_payment_simulator.repository.FxRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class FxService {

    private final FxRateRepository repo;

    public BigDecimal convert(String from, String to, BigDecimal amount) {
        FxRate fxRate = repo.findById(from + "_" + to)
                .orElseThrow(() -> new RuntimeException("No FX rate for " + from + "_" + to));

        double fluctuation = ThreadLocalRandom.current().nextDouble(-0.02, 0.02);
        BigDecimal finalRate = fxRate.getRate().add(
                fxRate.getRate().multiply(BigDecimal.valueOf(fluctuation))
        );
        return amount.multiply(finalRate);
    }

    public BigDecimal getEffectiveRate(String from, String to) {
        FxRate fxRate = repo.findById(from + "_" + to)
                .orElseThrow(() -> new RuntimeException("No FX rate for " + from + "_" + to));
        double fluctuation = ThreadLocalRandom.current().nextDouble(-0.02, 0.02);
        return fxRate.getRate().add(fxRate.getRate().multiply(BigDecimal.valueOf(fluctuation)));
    }

    @Scheduled(fixedRate = 5000)
    public void updateRates() {
        repo.findAll().forEach(r -> {
            double change = ThreadLocalRandom.current().nextDouble(-0.01, 0.01);
            r.setRate(r.getRate().add(r.getRate().multiply(BigDecimal.valueOf(change))));
            r.setLastUpdated(LocalDateTime.now());
            repo.save(r);
        });
    }
}
