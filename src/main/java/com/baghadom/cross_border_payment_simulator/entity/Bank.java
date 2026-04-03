package com.baghadom.cross_border_payment_simulator.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "bank")
@Data
public class Bank {

    @Id
    private String bic; // e.g. GTBINGLA, BARCGB22

    private String name;
    private String country;
    private String currency; // home currency
}
