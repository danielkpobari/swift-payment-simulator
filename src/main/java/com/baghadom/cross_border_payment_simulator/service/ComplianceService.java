package com.baghadom.cross_border_payment_simulator.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Set;

@Service
public class ComplianceService {

    private static final BigDecimal AML_THRESHOLD = new BigDecimal("10000");
    private static final Set<String> BLACKLIST = Set.of("sanctioned_user", "blocked_entity");

    public void validate(String sender, BigDecimal amount) {
        if (BLACKLIST.contains(sender)) {
            throw new RuntimeException("Sender is on sanctions list: " + sender);
        }
        if (amount.compareTo(AML_THRESHOLD) > 0) {
            throw new RuntimeException("Transaction flagged for AML review — amount exceeds threshold");
        }
    }
}
