package com.baghadom.cross_border_payment_simulator.service;

import com.baghadom.cross_border_payment_simulator.entity.SwiftMessage;
import com.baghadom.cross_border_payment_simulator.repository.BankRepository;
import com.baghadom.cross_border_payment_simulator.repository.SwiftMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SwiftNetworkService {

    private final SwiftMessageRepository swiftMessageRepository;
    private final BankRepository bankRepository;
    private final NostroVostroService nostroVostroService;
    private final IsoMessageBuilder isoMessageBuilder;

    /**
     * Routes a payment through a correspondent chain.
     * Each hop: debit sender's nostro at next bank, send pacs.008, receive pacs.002 ACK.
     */
    public List<SwiftMessage> route(UUID txnId, String senderBic, String receiverBic,
                                    BigDecimal amount, String currency) {

        List<String> path = resolvePath(senderBic, receiverBic);
        List<SwiftMessage> messages = new ArrayList<>();

        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to = path.get(i + 1);

            // Debit nostro account of 'from' bank held at 'to' bank
            try {
                nostroVostroService.debitNostro(from, to, currency, amount);
            } catch (RuntimeException e) {
                log.warn("Nostro debit skipped (no account): {}", e.getMessage());
            }

            // pacs.008 — credit transfer instruction
            SwiftMessage pacs008 = buildMessage(txnId, "pacs.008", from, to, amount, currency, "SENT");
            pacs008.setIsoXml(isoMessageBuilder.buildPacs002(txnId, txnId, from, to, "ACSP", "G000"));
            swiftMessageRepository.save(pacs008);
            messages.add(pacs008);

            // pacs.002 — status ACK: ACSP = Accepted, Settlement In Process
            SwiftMessage pacs002 = buildMessage(txnId, "pacs.002", to, from, amount, currency, "ACSP");
            pacs002.setIsoXml(isoMessageBuilder.buildPacs002(txnId, txnId, from, to, "ACSP", "G000"));
            swiftMessageRepository.save(pacs002);
            messages.add(pacs002);

            log.info("SWIFT hop: {} → {} | {} {}", from, to, amount, currency);
        }

        return messages;
    }

    public List<SwiftMessage> getMessagesForTxn(UUID txnId) {
        return swiftMessageRepository.findByTxnIdOrderBySentAtAsc(txnId);
    }

    /**
     * Resolves a correspondent path between two banks.
     * Real SWIFT uses a routing table; here we use a fixed topology:
     * NGN banks → USD correspondent (CITIVUS33) → GBP/EUR banks
     */
    private List<String> resolvePath(String senderBic, String receiverBic) {
        List<String> path = new ArrayList<>();
        path.add(senderBic);

        boolean senderIsNGN = bankRepository.findById(senderBic)
                .map(b -> "NGN".equals(b.getCurrency())).orElse(false);
        boolean receiverIsUSD = bankRepository.findById(receiverBic)
                .map(b -> "USD".equals(b.getCurrency())).orElse(false);

        if (senderIsNGN && !receiverIsUSD) {
            path.add("CITIVUS33"); // USD correspondent hub
        }

        path.add(receiverBic);
        return path;
    }

    private SwiftMessage buildMessage(UUID txnId, String type, String from, String to,
                                      BigDecimal amount, String currency, String status) {
        SwiftMessage msg = new SwiftMessage();
        msg.setTxnId(txnId);
        msg.setMessageType(type);
        msg.setSenderBic(from);
        msg.setReceiverBic(to);
        msg.setAmount(amount);
        msg.setCurrency(currency);
        msg.setStatus(status);
        return msg;
    }
}
