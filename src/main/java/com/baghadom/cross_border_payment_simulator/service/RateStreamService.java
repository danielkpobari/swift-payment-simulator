package com.baghadom.cross_border_payment_simulator.service;

import com.baghadom.cross_border_payment_simulator.repository.FxRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateStreamService {

    private final FxRateRepository fxRateRepository;
    private final SimpMessagingTemplate wsTemplate;

    @Scheduled(fixedRate = 2000)
    public void streamRates() {
        fxRateRepository.findAll().forEach(rate -> {
            try {
                String json = "{\"pair\":\"" + rate.getCurrencyPair() + "\","
                        + "\"rate\":" + rate.getRate() + ","
                        + "\"timestamp\":\"" + rate.getLastUpdated() + "\"}";
                wsTemplate.convertAndSend("/topic/rates", json);
            } catch (Exception e) {
                log.debug("Rate stream error: {}", e.getMessage());
            }
        });
    }
}
