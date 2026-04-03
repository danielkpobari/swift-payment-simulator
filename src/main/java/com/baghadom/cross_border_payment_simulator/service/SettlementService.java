package com.baghadom.cross_border_payment_simulator.service;

import com.baghadom.cross_border_payment_simulator.entity.CrossBorderTxn;
import com.baghadom.cross_border_payment_simulator.repository.CrossBorderTxnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final CrossBorderTxnRepository txnRepository;

    @Async
    public void settle(UUID txnId) {
        try {
            Thread.sleep(5000); // simulate T+1/T+2 settlement delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        txnRepository.findById(txnId).ifPresent(txn -> {
            txn.setStatus("SETTLED");
            txnRepository.save(txn);
            log.info("Settlement complete for txn: {}", txnId);
        });
    }
}
