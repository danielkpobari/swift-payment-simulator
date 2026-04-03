package com.baghadom.cross_border_payment_simulator.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CrossBorderRequest {
    private String sender;
    private String receiver;
    private BigDecimal amount;
    private String sourceCurrency;
    private String targetCurrency;
    private String senderBic;   // e.g. GTBINGLA
    private String receiverBic; // e.g. BARCGB22
}
