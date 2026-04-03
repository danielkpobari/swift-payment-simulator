package com.baghadom.cross_border_payment_simulator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "swift_message")
@Data
public class SwiftMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID txnId;
    private String messageType; // pacs.008, pacs.002, camt.056
    private String senderBic;
    private String receiverBic;
    private BigDecimal amount;
    private String currency;
    private String status;       // ACCP, ACSP, RJCT, PDNG
    @Column(columnDefinition = "TEXT")
    private String isoXml;       // full ISO 20022 XML payload
    private LocalDateTime sentAt = LocalDateTime.now();
}
