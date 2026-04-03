package com.baghadom.cross_border_payment_simulator.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class FeeService {

    private static final BigDecimal PERCENTAGE = new BigDecimal("0.015");
    private static final BigDecimal FLAT = new BigDecimal("2");

    public BigDecimal calculate(BigDecimal amount) {
        return amount.multiply(PERCENTAGE).add(FLAT);
    }
}
