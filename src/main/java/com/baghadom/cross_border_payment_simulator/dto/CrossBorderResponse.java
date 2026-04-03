package com.baghadom.cross_border_payment_simulator.dto;

import com.baghadom.cross_border_payment_simulator.entity.SwiftMessage;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CrossBorderResponse {
    private UUID txnId;
    private String status;
    private BigDecimal convertedAmount;
    private BigDecimal fee;
    private BigDecimal fxRate;
    private String isoMessage;
    private List<SwiftMessage> swiftHops;
}
