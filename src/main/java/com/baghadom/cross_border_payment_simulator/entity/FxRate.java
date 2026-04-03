package com.baghadom.cross_border_payment_simulator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fx_rates")
@Data
public class FxRate {

    @Id
    private String currencyPair;

    private BigDecimal rate;
    private LocalDateTime lastUpdated = LocalDateTime.now();
}
