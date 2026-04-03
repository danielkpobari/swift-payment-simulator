package com.baghadom.cross_border_payment_simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class CrossBorderPaymentSimulatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrossBorderPaymentSimulatorApplication.class, args);
	}

}
