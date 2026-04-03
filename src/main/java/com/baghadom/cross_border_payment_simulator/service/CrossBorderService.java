package com.baghadom.cross_border_payment_simulator.service;

import com.baghadom.cross_border_payment_simulator.dto.CrossBorderRequest;
import com.baghadom.cross_border_payment_simulator.dto.CrossBorderResponse;
import com.baghadom.cross_border_payment_simulator.entity.CrossBorderTxn;
import com.baghadom.cross_border_payment_simulator.entity.SwiftMessage;
import com.baghadom.cross_border_payment_simulator.repository.CrossBorderTxnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrossBorderService {

    private final FxService fxService;
    private final FeeService feeService;
    private final LiquidityService liquidityService;
    private final ComplianceService complianceService;
    private final IsoMessageBuilder isoMessageBuilder;
    private final SettlementService settlementService;
    private final SwiftNetworkService swiftNetworkService;
    private final CrossBorderTxnRepository txnRepository;

    public CrossBorderResponse process(CrossBorderRequest req) {

        // 1. Compliance
        complianceService.validate(req.getSender(), req.getAmount());

        // 2. FX: source → USD
        BigDecimal usd = fxService.convert(req.getSourceCurrency(), "USD", req.getAmount());

        // 3. Liquidity check on USD nostro
        liquidityService.checkLiquidity("USD", usd);

        // 4. FX: USD → target
        BigDecimal converted = fxService.convert("USD", req.getTargetCurrency(), usd);

        // 5. Fee
        BigDecimal fee = feeService.calculate(req.getAmount());

        // 6. Effective composite rate
        BigDecimal effectiveRate = converted.divide(req.getAmount(), 6, RoundingMode.HALF_UP);

        // 7. Persist txn first to obtain UUID
        CrossBorderTxn txn = new CrossBorderTxn();
        txn.setSourceCurrency(req.getSourceCurrency());
        txn.setTargetCurrency(req.getTargetCurrency());
        txn.setAmount(req.getAmount());
        txn.setFee(fee);
        txn.setFxRate(effectiveRate);
        txn.setStatus("PENDING");
        txn.setSender(req.getSender());
        txn.setReceiver(req.getReceiver());
        txn.setSenderBic(req.getSenderBic());
        txn.setReceiverBic(req.getReceiverBic());
        CrossBorderTxn saved = txnRepository.save(txn);

        // 8. Build ISO 20022 pacs.008 with real txnId as EndToEndId / TxId
        String isoMsg = isoMessageBuilder.buildPacs008(req, saved.getId());
        saved.setIsoMessage(isoMsg);
        txnRepository.save(saved);

        // 9. SWIFT network routing — emits pacs.008 + pacs.002 per hop
        String senderBic = req.getSenderBic() != null ? req.getSenderBic() : "GTBINGLA";
        String receiverBic = req.getReceiverBic() != null ? req.getReceiverBic() : "BARCGB22";
        List<SwiftMessage> hops = swiftNetworkService.route(
                saved.getId(), senderBic, receiverBic, converted, req.getTargetCurrency());

        // 10. Async settlement (T+1/T+2)
        settlementService.settle(saved.getId());

        return CrossBorderResponse.builder()
                .txnId(saved.getId())
                .status("PENDING")
                .convertedAmount(converted.setScale(2, RoundingMode.HALF_UP))
                .fee(fee.setScale(2, RoundingMode.HALF_UP))
                .fxRate(effectiveRate)
                .isoMessage(isoMsg)
                .swiftHops(hops)
                .build();
    }
}
