package com.baghadom.cross_border_payment_simulator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "nostro_vostro_account")
@Data
public class NostroVostroAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String ownerBic;        // bank that owns this account
    private String correspondentBic; // bank where the account is held
    private String currency;
    private BigDecimal balance;

    // NOSTRO = "our account at their bank" (from ownerBic perspective)
    // VOSTRO = "their account at our bank" (from correspondentBic perspective)
    private String accountType; // NOSTRO or VOSTRO
}
