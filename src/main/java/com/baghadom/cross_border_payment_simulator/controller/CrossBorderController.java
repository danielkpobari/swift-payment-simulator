package com.baghadom.cross_border_payment_simulator.controller;

import com.baghadom.cross_border_payment_simulator.dto.CrossBorderRequest;
import com.baghadom.cross_border_payment_simulator.dto.CrossBorderResponse;
import com.baghadom.cross_border_payment_simulator.entity.*;
import com.baghadom.cross_border_payment_simulator.repository.BankRepository;
import com.baghadom.cross_border_payment_simulator.repository.CrossBorderTxnRepository;
import com.baghadom.cross_border_payment_simulator.repository.FxRateRepository;
import com.baghadom.cross_border_payment_simulator.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CrossBorderController {

    private final CrossBorderService crossBorderService;
    private final BankRepository bankRepository;
    private final FxRateRepository fxRateRepository;
    private final CrossBorderTxnRepository txnRepository;
    private final NostroVostroService nostroVostroService;
    private final ArbitrageEngine arbitrageEngine;
    private final SwiftNetworkService swiftNetworkService;

    @PostMapping("/cross-border/send")
    public ResponseEntity<CrossBorderResponse> send(@RequestBody CrossBorderRequest request) {
        return ResponseEntity.ok(crossBorderService.process(request));
    }

    @GetMapping("/banks")
    public ResponseEntity<List<Bank>> getBanks() {
        return ResponseEntity.ok(bankRepository.findAll());
    }

    @GetMapping("/rates")
    public ResponseEntity<List<FxRate>> getRates() {
        return ResponseEntity.ok(fxRateRepository.findAll());
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<CrossBorderTxn>> getTransactions() {
        return ResponseEntity.ok(txnRepository.findAll());
    }

    @GetMapping("/nostro")
    public ResponseEntity<List<NostroVostroAccount>> getNostroAccounts() {
        return ResponseEntity.ok(nostroVostroService.getAll());
    }

    @GetMapping("/nostro/{bic}")
    public ResponseEntity<List<NostroVostroAccount>> getNostroByBank(@PathVariable String bic) {
        return ResponseEntity.ok(nostroVostroService.getAccountsForBank(bic));
    }

    @GetMapping("/arbitrage")
    public ResponseEntity<List<ArbitrageOpportunity>> getArbitrage() {
        return ResponseEntity.ok(arbitrageEngine.getRecent());
    }

    @GetMapping("/swift/{txnId}")
    public ResponseEntity<List<SwiftMessage>> getSwiftMessages(@PathVariable UUID txnId) {
        return ResponseEntity.ok(swiftNetworkService.getMessagesForTxn(txnId));
    }
}
