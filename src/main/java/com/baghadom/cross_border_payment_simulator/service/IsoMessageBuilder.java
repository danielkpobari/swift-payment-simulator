package com.baghadom.cross_border_payment_simulator.service;

import com.baghadom.cross_border_payment_simulator.dto.CrossBorderRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Generates ISO 20022 XML messages.
 * Schemas implemented:
 *   pacs.008.001.08 — FI-to-FI Customer Credit Transfer
 *   pacs.002.001.10 — Payment Status Report
 *   camt.056.001.08 — FI-to-FI Payment Cancellation Request
 */
@Service
public class IsoMessageBuilder {

    private static final String PACS008_NS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08";
    private static final String PACS002_NS = "urn:iso:std:iso:20022:tech:xsd:pacs.002.001.10";
    private static final String CAMT056_NS = "urn:iso:std:iso:20022:tech:xsd:camt.056.001.08";

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * pacs.008.001.08 — FI-to-FI Customer Credit Transfer
     * Mandatory elements per ISO 20022 schema:
     *   GrpHdr: MsgId, CreDtTm, NbOfTxs, SttlmInf
     *   CdtTrfTxInf: PmtId (EndToEndId + TxId), IntrBkSttlmAmt, IntrBkSttlmDt,
     *                ChrgBr, InstgAgt, InstdAgt, Dbtr, DbtrAcct, DbtrAgt, CdtrAgt, Cdtr, CdtrAcct
     */
    public String buildPacs008(CrossBorderRequest req, UUID txnId) {
        UUID msgId = UUID.randomUUID();
        String now = OffsetDateTime.now().format(DT_FMT);
        String today = LocalDate.now().format(DATE_FMT);
        String endToEndId = "E2E-" + (txnId != null ? txnId.toString().substring(0, 8) : msgId.toString().substring(0, 8));

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="%s">
                  <FIToFICstmrCdtTrf>
                    <GrpHdr>
                      <MsgId>%s</MsgId>
                      <CreDtTm>%s</CreDtTm>
                      <NbOfTxs>1</NbOfTxs>
                      <SttlmInf>
                        <SttlmMtd>INDA</SttlmMtd>
                      </SttlmInf>
                    </GrpHdr>
                    <CdtTrfTxInf>
                      <PmtId>
                        <EndToEndId>%s</EndToEndId>
                        <TxId>%s</TxId>
                      </PmtId>
                      <IntrBkSttlmAmt Ccy="%s">%s</IntrBkSttlmAmt>
                      <IntrBkSttlmDt>%s</IntrBkSttlmDt>
                      <ChrgBr>SHA</ChrgBr>
                      <InstgAgt>
                        <FinInstnId>
                          <BICFI>%s</BICFI>
                        </FinInstnId>
                      </InstgAgt>
                      <InstdAgt>
                        <FinInstnId>
                          <BICFI>%s</BICFI>
                        </FinInstnId>
                      </InstdAgt>
                      <Dbtr>
                        <Nm>%s</Nm>
                      </Dbtr>
                      <DbtrAcct>
                        <Id>
                          <Othr>
                            <Id>ACC-%s</Id>
                          </Othr>
                        </Id>
                      </DbtrAcct>
                      <DbtrAgt>
                        <FinInstnId>
                          <BICFI>%s</BICFI>
                        </FinInstnId>
                      </DbtrAgt>
                      <CdtrAgt>
                        <FinInstnId>
                          <BICFI>%s</BICFI>
                        </FinInstnId>
                      </CdtrAgt>
                      <Cdtr>
                        <Nm>%s</Nm>
                      </Cdtr>
                      <CdtrAcct>
                        <Id>
                          <Othr>
                            <Id>ACC-%s</Id>
                          </Othr>
                        </Id>
                      </CdtrAcct>
                    </CdtTrfTxInf>
                  </FIToFICstmrCdtTrf>
                </Document>
                """.formatted(
                PACS008_NS,
                msgId,
                now,
                endToEndId,
                txnId != null ? txnId : msgId,
                req.getTargetCurrency(),
                req.getAmount(),
                today,
                req.getSenderBic() != null ? req.getSenderBic() : "GTBINGLA",
                req.getReceiverBic() != null ? req.getReceiverBic() : "BARCGB22",
                req.getSender(),
                req.getSender().toUpperCase().replaceAll("\\s+", ""),
                req.getSenderBic() != null ? req.getSenderBic() : "GTBINGLA",
                req.getReceiverBic() != null ? req.getReceiverBic() : "BARCGB22",
                req.getReceiver(),
                req.getReceiver().toUpperCase().replaceAll("\\s+", "")
        );
    }

    /**
     * pacs.002.001.10 — Payment Status Report (ACK/NACK per hop)
     * TxSts values: ACCP (Accepted), ACSP (Accepted Settlement In Process),
     *               RJCT (Rejected), PDNG (Pending)
     */
    public String buildPacs002(UUID originalMsgId, UUID txnId, String senderBic,
                               String receiverBic, String txStatus, String statusReason) {
        String now = OffsetDateTime.now().format(DT_FMT);
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="%s">
                  <FIToFIPmtStsRpt>
                    <GrpHdr>
                      <MsgId>%s</MsgId>
                      <CreDtTm>%s</CreDtTm>
                    </GrpHdr>
                    <TxInfAndSts>
                      <OrgnlEndToEndId>E2E-%s</OrgnlEndToEndId>
                      <OrgnlTxId>%s</OrgnlTxId>
                      <TxSts>%s</TxSts>
                      <StsRsnInf>
                        <Rsn>
                          <Cd>%s</Cd>
                        </Rsn>
                      </StsRsnInf>
                      <InstgAgt>
                        <FinInstnId>
                          <BICFI>%s</BICFI>
                        </FinInstnId>
                      </InstgAgt>
                      <InstdAgt>
                        <FinInstnId>
                          <BICFI>%s</BICFI>
                        </FinInstnId>
                      </InstdAgt>
                    </TxInfAndSts>
                  </FIToFIPmtStsRpt>
                </Document>
                """.formatted(
                PACS002_NS,
                UUID.randomUUID(),
                now,
                originalMsgId.toString().substring(0, 8),
                txnId,
                txStatus,
                statusReason,
                receiverBic,  // ACK sender = original receiver
                senderBic     // ACK receiver = original sender
        );
    }

    /**
     * camt.056.001.08 — Payment Cancellation Request (recall)
     */
    public String buildCamt056(UUID txnId, String senderBic, String receiverBic,
                               BigDecimal amount, String currency, String cancelReason) {
        String now = OffsetDateTime.now().format(DT_FMT);
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Document xmlns="%s">
                  <FIToFIPmtCxlReq>
                    <Assgnmt>
                      <MsgId>%s</MsgId>
                      <CreDtTm>%s</CreDtTm>
                      <Assgnr>
                        <Agt>
                          <FinInstnId>
                            <BICFI>%s</BICFI>
                          </FinInstnId>
                        </Agt>
                      </Assgnr>
                      <Assgne>
                        <Agt>
                          <FinInstnId>
                            <BICFI>%s</BICFI>
                          </FinInstnId>
                        </Agt>
                      </Assgne>
                    </Assgnmt>
                    <Undrlyg>
                      <TxInf>
                        <OrgnlTxId>%s</OrgnlTxId>
                        <OrgnlIntrBkSttlmAmt Ccy="%s">%s</OrgnlIntrBkSttlmAmt>
                        <CxlRsnInf>
                          <Rsn>
                            <Cd>%s</Cd>
                          </Rsn>
                        </CxlRsnInf>
                      </TxInf>
                    </Undrlyg>
                  </FIToFIPmtCxlReq>
                </Document>
                """.formatted(
                CAMT056_NS,
                UUID.randomUUID(),
                now,
                senderBic,
                receiverBic,
                txnId,
                currency,
                amount,
                cancelReason
        );
    }
}
