package com.baghadom.cross_border_payment_simulator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "liquidity_pool")
@Data
public class LiquidityPool {

    @Id
    private String currency;

    private BigDecimal availableBalance;
}
