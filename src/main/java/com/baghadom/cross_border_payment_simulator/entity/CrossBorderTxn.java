package com.baghadom.cross_border_payment_simulator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cross_border_txn")
@Data
public class CrossBorderTxn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String sourceCurrency;
    private String targetCurrency;
    private BigDecimal amount;
    private String status;
    private BigDecimal fxRate;
    private BigDecimal fee;
    private String isoMessage;
    private String senderBic;
    private String receiverBic;
    private String sender;
    private String receiver;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
