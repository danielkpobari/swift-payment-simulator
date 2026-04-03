package com.baghadom.cross_border_payment_simulator.config;

// Kafka support removed — spring-kafka is incompatible with Spring Boot 4.0.5 (Jackson 3.x conflict).
// To re-enable: add spring-kafka back to pom.xml once a compatible version is available,
// then restore KafkaTemplate injection in RateStreamService.
