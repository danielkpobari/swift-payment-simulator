# Cross-Border Payment Simulator

A production-grade simulation of an international payment network built with Spring Boot.
It models how real banks move money across borders — covering SWIFT message routing,
ISO 20022 messaging, FX conversion chains, nostro/vostro account management,
AML compliance, settlement delays, triangular arbitrage detection, and a live dashboard.

---

## Table of Contents

1. [What This Simulates](#what-this-simulates)
2. [Core Concepts Explained](#core-concepts-explained)
3. [Architecture](#architecture)
4. [Project Structure](#project-structure)
5. [Tech Stack](#tech-stack)
6. [Getting Started](#getting-started)
7. [The Dashboard](#the-dashboard)
8. [REST API Reference](#rest-api-reference)
9. [ISO 20022 Messages](#iso-20022-messages)
10. [Payment Flow — Step by Step](#payment-flow--step-by-step)
11. [Seeded Data](#seeded-data)
12. [Configuration](#configuration)
13. [Known Limitations](#known-limitations)

---

## What This Simulates

When you send money from Nigeria to the UK in real life, it does not travel directly.
It passes through a chain of correspondent banks, gets converted through FX markets,
is checked against sanctions lists, and settles on a T+1 or T+2 basis.
This project simulates all of that:

```
NGN Wallet (GTBank Nigeria)
    │
    │  pacs.008 (ISO 20022 credit transfer)
    ▼
Citibank USD Hub (CITIVUS33)   ← correspondent bank
    │
    │  pacs.008
    ▼
Barclays UK (BARCGB22)
    │
    │  pacs.002 ACK at each hop
    ▼
GBP Beneficiary Account
```

Every step generates real ISO 20022 XML, debits the correct nostro account,
checks liquidity, applies fees, and asynchronously settles after a delay.

---

## Core Concepts Explained

### SWIFT Network
SWIFT (Society for Worldwide Interbank Financial Telecommunication) is the messaging
network banks use to instruct each other to move money. Banks are identified by a
BIC (Bank Identifier Code), e.g. `BARCGB22` = Barclays, Great Britain.

This simulator models 8 banks across 4 countries connected in a correspondent topology.

### ISO 20022
The global standard for financial messaging XML. Three message types are implemented:

| Message | Full Name | When Used |
|---|---|---|
| `pacs.008.001.08` | FI-to-FI Customer Credit Transfer | Instructing a payment hop |
| `pacs.002.001.10` | Payment Status Report | ACK/NACK response per hop |
| `camt.056.001.08` | Payment Cancellation Request | Recalling a payment |

Each message contains mandatory fields: `MsgId`, `CreDtTm`, `NbOfTxs`, `SttlmInf`,
`EndToEndId`, `TxId`, `IntrBkSttlmAmt`, `IntrBkSttlmDt`, `ChrgBr`, agent BICs,
debtor/creditor names and account IDs.

### Nostro / Vostro Accounts
These are how banks hold money at each other to facilitate cross-border payments.

- **Nostro** = "our money, held at your bank" (from the sending bank's perspective)
- **Vostro** = "your money, held at our bank" (from the receiving bank's perspective)

Example: GTBank holds a USD nostro account at Citibank. When GTBank sends a USD
payment, Citibank debits that nostro account. This simulator tracks those balances
and debits them on every payment hop.

### FX Engine
Payments are converted through a two-leg chain:
```
Source Currency → USD (intermediate) → Target Currency
```
Each conversion applies a ±2% random fluctuation on top of the base rate to simulate
live market conditions. Rates themselves drift ±1% every 5 seconds via a scheduler.

### Compliance Engine (AML Simulation)
Every payment is checked before processing:
- **Sanctions screening** — sender name checked against a blacklist
- **AML threshold** — amounts over 10,000 are flagged and rejected

### Settlement Engine
Real cross-border payments settle on T+1 or T+2 (next business day or two days later).
This is simulated with a 5-second async delay after which the transaction status
changes from `PENDING` to `SETTLED`.

### Arbitrage Engine
Triangular arbitrage is when you exploit rate inconsistencies across three currencies
to make a risk-free profit. For example:
```
Start with $1 → convert to EUR → convert to GBP → convert back to USD
If you end up with more than $1, that gap is an arbitrage opportunity.
```
The engine scans three triangles every 5 seconds and records any opportunity
where the profit exceeds 0.1%.

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    REST API Layer                    │
│              CrossBorderController                   │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│              SAGA Orchestrator                       │
│              CrossBorderService                      │
└──┬──────────┬──────────┬──────────┬─────────────────┘
   │          │          │          │
   ▼          ▼          ▼          ▼
Compliance  FxService  Liquidity  FeeService
Service               Service
   │
   ▼
IsoMessageBuilder  →  pacs.008 XML
   │
   ▼
SwiftNetworkService  →  pacs.008 + pacs.002 per hop
   │                    NostroVostroService (debit accounts)
   ▼
SettlementService  →  async T+1/T+2 delay → SETTLED
   │
   ▼
CrossBorderTxnRepository  →  H2 Database

Parallel:
ArbitrageEngine    →  scans every 5s  →  ArbitrageRepository
FxService          →  drifts rates every 5s
RateStreamService  →  broadcasts rates every 2s via WebSocket → Dashboard
```

---

## Project Structure

```
src/main/java/com/baghadom/cross_border_payment_simulator/
│
├── config/
│   ├── WebSocketConfig.java        STOMP WebSocket broker setup
│   └── KafkaConfig.java            Placeholder (Kafka ready when broker available)
│
├── controller/
│   └── CrossBorderController.java  All REST endpoints
│
├── dto/
│   ├── CrossBorderRequest.java     Incoming payment request body
│   └── CrossBorderResponse.java    Payment response with ISO message + SWIFT hops
│
├── entity/
│   ├── CrossBorderTxn.java         Payment transaction record
│   ├── FxRate.java                 Currency pair rate (e.g. NGN_USD)
│   ├── LiquidityPool.java          Per-currency liquidity balance
│   ├── Bank.java                   Bank node with BIC, country, home currency
│   ├── NostroVostroAccount.java    Correspondent account between two banks
│   ├── SwiftMessage.java           ISO 20022 message record per hop
│   └── ArbitrageOpportunity.java   Detected triangular arbitrage window
│
├── repository/                     Spring Data JPA repositories (one per entity)
│
├── service/
│   ├── CrossBorderService.java     SAGA orchestrator — main payment pipeline
│   ├── FxService.java              FX conversion + rate fluctuation scheduler
│   ├── FeeService.java             1.5% + $2 flat fee calculation
│   ├── LiquidityService.java       Checks currency pool has enough balance
│   ├── ComplianceService.java      AML threshold + sanctions blacklist
│   ├── IsoMessageBuilder.java      Generates pacs.008, pacs.002, camt.056 XML
│   ├── SwiftNetworkService.java    Routes payment through correspondent chain
│   ├── NostroVostroService.java    Debits/credits nostro accounts per hop
│   ├── SettlementService.java      Async settlement with T+1/T+2 delay
│   ├── ArbitrageEngine.java        Triangular arbitrage scanner
│   └── RateStreamService.java      WebSocket rate broadcaster (every 2s)
│
└── CrossBorderPaymentSimulatorApplication.java
```

---

## Tech Stack

| Component | Technology |
|---|---|
| Framework | Spring Boot 3.4.4 |
| Language | Java 17 |
| Database | H2 (in-memory) |
| ORM | Spring Data JPA + Hibernate 6 |
| Real-time | Spring WebSocket (STOMP over SockJS) |
| Messaging standard | ISO 20022 (hand-crafted XML) |
| Build tool | Maven |
| Utilities | Lombok |
| Frontend | Vanilla HTML/JS + Chart.js + SockJS + STOMP.js |

---

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven (or use the included `mvnw` wrapper)
- No database setup needed — H2 runs in memory
- No Kafka broker needed — WebSocket handles real-time streaming

### Run

```bash
# Clone and enter the project
cd cross-border-payment-simulator

# Run with Maven wrapper
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
```

Or run `CrossBorderPaymentSimulatorApplication.java` directly from IntelliJ.

The app starts on **http://localhost:8080**

### Verify it started

You should see in the logs:
```
Tomcat started on port 8080
SimpleBrokerMessageHandler: Started
H2 console available at '/h2-console'
```

---

## The Dashboard

Open **http://localhost:8080** in your browser.

```
┌─────────────────────────────────────────────────────────────┐
│  🌍 Cross-Border Payment Simulator    [SWIFT + ISO 20022]   │
│                                              ● Live         │
├──────────────────────────────────────────────────────────────┤
│  📡 Live FX Rates (WebSocket — updates every 2s)            │
│  NGN/USD ▲  USD/GBP ▼  USD/EUR ▲  EUR/GBP ...              │
├─────────────────────────┬────────────────────────────────────┤
│  📈 Rate Chart          │  💸 Send Payment                  │
│  (switchable pair)      │  Sender / Receiver / BICs         │
│                         │  Amount / Source / Target CCY     │
│                         │  [Send Payment →]                 │
├─────────────────────────┴────────────────────────────────────┤
│  [Transactions] [SWIFT Route] [Nostro/Vostro] [Arbitrage]   │
│                                                              │
│  Transactions tab: live table, auto-refreshes every 6s      │
│  SWIFT Route tab:  select a txn → see hop chain + XML msgs  │
│  Nostro/Vostro tab: account balances (red = low balance)    │
│  Arbitrage tab:    detected opportunities with profit %     │
└──────────────────────────────────────────────────────────────┘
```

### Sending a payment from the dashboard

1. Fill in Sender Name (e.g. `Alice`) and Receiver Name (e.g. `Bob`)
2. Select Sender BIC — the originating bank (e.g. `GTBINGLA` for GTBank Nigeria)
3. Select Receiver BIC — the destination bank (e.g. `BARCGB22` for Barclays UK)
4. Enter Amount (e.g. `5000`)
5. Set Source Currency to `NGN`, Target Currency to `GBP`
6. Click **Send Payment →**
7. The response box shows the converted amount, fee, FX rate, and the full pacs.008 XML
8. Switch to the **SWIFT Route** tab, select the transaction, and see every hop

---

## REST API Reference

Base URL: `http://localhost:8080/api`

### Send a cross-border payment

```
POST /api/cross-border/send
Content-Type: application/json
```

Request body:
```json
{
  "sender": "Alice",
  "receiver": "Bob",
  "senderBic": "GTBINGLA",
  "receiverBic": "BARCGB22",
  "amount": 5000,
  "sourceCurrency": "NGN",
  "targetCurrency": "GBP"
}
```

Response:
```json
{
  "txnId": "3f2a1b4c-...",
  "status": "PENDING",
  "convertedAmount": 2.54,
  "fee": 77.00,
  "fxRate": 0.000508,
  "isoMessage": "<?xml version=\"1.0\"...pacs.008...",
  "swiftHops": [
    { "messageType": "pacs.008", "senderBic": "GTBINGLA", "receiverBic": "CITIVUS33", "status": "SENT" },
    { "messageType": "pacs.002", "senderBic": "CITIVUS33", "receiverBic": "GTBINGLA", "status": "ACSP" },
    { "messageType": "pacs.008", "senderBic": "CITIVUS33", "receiverBic": "BARCGB22", "status": "SENT" },
    { "messageType": "pacs.002", "senderBic": "BARCGB22",  "receiverBic": "CITIVUS33", "status": "ACSP" }
  ]
}
```

After ~5 seconds the transaction status changes to `SETTLED` in the database.

---

### Other endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/transactions` | All payment transactions |
| `GET` | `/api/rates` | All current FX rates |
| `GET` | `/api/banks` | All banks in the network |
| `GET` | `/api/nostro` | All nostro/vostro accounts |
| `GET` | `/api/nostro/{bic}` | Accounts for a specific bank BIC |
| `GET` | `/api/arbitrage` | Last 20 arbitrage opportunities |
| `GET` | `/api/swift/{txnId}` | All SWIFT messages for a transaction |

---

### Compliance test cases

```json
// Triggers AML flag (amount > 10,000)
{ "sender": "Alice", "amount": 15000, ... }

// Triggers sanctions block
{ "sender": "sanctioned_user", "amount": 100, ... }
```

Both return a 500 with the reason. In production these would return 422 with a
structured error body — the compliance engine is intentionally simple here.

---

## ISO 20022 Messages

Every payment generates a `pacs.008.001.08` stored on the transaction record.
Every SWIFT hop generates a `pacs.008` (instruction) and `pacs.002` (status ACK).

### pacs.008 example (abbreviated)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08">
  <FIToFICstmrCdtTrf>
    <GrpHdr>
      <MsgId>a1b2c3d4-...</MsgId>
      <CreDtTm>2026-04-03T04:00:00+01:00</CreDtTm>
      <NbOfTxs>1</NbOfTxs>
      <SttlmInf>
        <SttlmMtd>INDA</SttlmMtd>
      </SttlmInf>
    </GrpHdr>
    <CdtTrfTxInf>
      <PmtId>
        <EndToEndId>E2E-3f2a1b4c</EndToEndId>
        <TxId>3f2a1b4c-...</TxId>
      </PmtId>
      <IntrBkSttlmAmt Ccy="GBP">2.54</IntrBkSttlmAmt>
      <IntrBkSttlmDt>2026-04-03</IntrBkSttlmDt>
      <ChrgBr>SHA</ChrgBr>
      <InstgAgt><FinInstnId><BICFI>GTBINGLA</BICFI></FinInstnId></InstgAgt>
      <InstdAgt><FinInstnId><BICFI>BARCGB22</BICFI></FinInstnId></InstdAgt>
      <Dbtr><Nm>Alice</Nm></Dbtr>
      <DbtrAcct><Id><Othr><Id>ACC-ALICE</Id></Othr></Id></DbtrAcct>
      <DbtrAgt><FinInstnId><BICFI>GTBINGLA</BICFI></FinInstnId></DbtrAgt>
      <CdtrAgt><FinInstnId><BICFI>BARCGB22</BICFI></FinInstnId></CdtrAgt>
      <Cdtr><Nm>Bob</Nm></Cdtr>
      <CdtrAcct><Id><Othr><Id>ACC-BOB</Id></Othr></Id></CdtrAcct>
    </CdtTrfTxInf>
  </FIToFICstmrCdtTrf>
</Document>
```

### pacs.002 TxSts codes used

| Code | Meaning |
|---|---|
| `ACSP` | Accepted, Settlement In Process |
| `ACCP` | Accepted, Compliance Passed |
| `RJCT` | Rejected |
| `PDNG` | Pending |

---

## Payment Flow — Step by Step

Here is exactly what happens when you POST to `/api/cross-border/send`:

```
1. COMPLIANCE CHECK
   └─ Is sender on sanctions list? → reject
   └─ Is amount > 10,000? → flag for AML review

2. FX CONVERSION LEG 1
   └─ Convert NGN → USD using NGN_USD rate + ±2% market fluctuation

3. LIQUIDITY CHECK
   └─ Does the USD liquidity pool have enough balance?
   └─ If not → reject with "Insufficient liquidity"

4. FX CONVERSION LEG 2
   └─ Convert USD → GBP using USD_GBP rate + ±2% fluctuation

5. FEE CALCULATION
   └─ Fee = (amount × 1.5%) + $2 flat

6. PERSIST TRANSACTION
   └─ Save to cross_border_txn table with status = PENDING

7. BUILD ISO 20022 pacs.008
   └─ Full XML with EndToEndId = txnId, all mandatory fields
   └─ Stored on the transaction record

8. SWIFT NETWORK ROUTING
   └─ Resolve correspondent path: GTBINGLA → CITIVUS33 → BARCGB22
   └─ For each hop:
       a. Debit sender's nostro account at next bank
       b. Emit pacs.008 (instruction) → saved to swift_message table
       c. Emit pacs.002 ACSP (ACK) → saved to swift_message table

9. ASYNC SETTLEMENT (background thread)
   └─ Wait 5 seconds (simulates T+1/T+2)
   └─ Update transaction status → SETTLED

10. RETURN RESPONSE
    └─ txnId, status, convertedAmount, fee, fxRate, isoMessage, swiftHops[]
```

---

## Seeded Data

The following data is loaded automatically on every startup from `data.sql`:

### Banks

| BIC | Name | Country | Currency |
|---|---|---|---|
| `GTBINGLA` | Guaranty Trust Bank | Nigeria | NGN |
| `ZENITHNG` | Zenith Bank | Nigeria | NGN |
| `CITIVUS33` | Citibank N.A. (USD Hub) | United States | USD |
| `CHASUS33` | JPMorgan Chase | United States | USD |
| `BARCGB22` | Barclays Bank UK | United Kingdom | GBP |
| `HSBCGB2L` | HSBC UK | United Kingdom | GBP |
| `DEUTDEDB` | Deutsche Bank | Germany | EUR |
| `BNPAFRPP` | BNP Paribas | France | EUR |

### FX Rates (starting values, drift every 5s)

| Pair | Rate |
|---|---|
| NGN_USD | 0.00065 |
| USD_GBP | 0.79 |
| USD_EUR | 0.92 |
| EUR_GBP | 0.86 |
| GBP_USD | 1.265 |
| EUR_USD | 1.087 |

### Nostro/Vostro Accounts

| Owner | Correspondent | Currency | Balance |
|---|---|---|---|
| GTBINGLA | CITIVUS33 | USD | $2,000,000 |
| GTBINGLA | BARCGB22 | GBP | £1,000,000 |
| ZENITHNG | CHASUS33 | USD | $1,500,000 |
| CITIVUS33 | BARCGB22 | GBP | £3,000,000 |
| CITIVUS33 | DEUTDEDB | EUR | €2,500,000 |
| CHASUS33 | HSBCGB2L | GBP | £2,000,000 |
| BARCGB22 | BNPAFRPP | EUR | €1,800,000 |

---

## Configuration

All config lives in `src/main/resources/application.properties`:

```properties
# H2 in-memory database (resets on every restart)
spring.datasource.url=jdbc:h2:mem:paymentdb;DB_CLOSE_DELAY=-1
spring.jpa.hibernate.ddl-auto=create-drop

# H2 web console (useful for inspecting tables)
spring.h2.console.enabled=true
# Access at: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:paymentdb  |  User: sa  |  Password: (blank)
```

To persist data across restarts, change the datasource to a file-based H2:
```properties
spring.datasource.url=jdbc:h2:file:./paymentdb
spring.jpa.hibernate.ddl-auto=update
```

To run on a different port:
```properties
server.port=9090
```

---

## Known Limitations

These are intentional simplifications for a simulator — not bugs:

| Limitation | Real-world equivalent |
|---|---|
| AML threshold is a hard number (10,000) | Real AML uses ML models + transaction history |
| Sanctions list is hardcoded | Real systems query OFAC/UN/EU sanctions APIs |
| FX rates are seeded and drift randomly | Real rates come from Reuters/Bloomberg feeds |
| Settlement delay is a fixed 5s sleep | Real T+1/T+2 involves cut-off times and RTGS |
| Nostro balances don't replenish | Real banks top up nostro accounts via treasury ops |
| No authentication or authorisation | Production systems use OAuth2 / mTLS |
| H2 is in-memory (data lost on restart) | Production uses PostgreSQL / Oracle |
| Kafka is stubbed out | Can be re-enabled when a broker is available |
| Account IDs are generated from names | Real systems use IBANs |
#   s w i f t - p a y m e n t - s i m u l a t o r  
 