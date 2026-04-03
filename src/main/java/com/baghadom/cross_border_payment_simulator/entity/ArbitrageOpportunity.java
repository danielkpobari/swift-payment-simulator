package com.baghadom.cross_border_payment_simulator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "arbitrage_opportunity")
@Data
public class ArbitrageOpportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String path;           // e.g. USD→EUR→GBP→USD
    private BigDecimal profitPct;  // e.g. 0.0031 = 0.31%
    private BigDecimal startRate;
    private BigDecimal endRate;
    private LocalDateTime detectedAt = LocalDateTime.now();
    private boolean exploited;
}
