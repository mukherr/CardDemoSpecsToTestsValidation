# Specification-Driven Code Validation via Generated Tests

**Audience:** Scientists and senior engineers evaluating spec-to-code pipelines  
**System Under Study:** CardDemo — a credit card management application  
**Date:** 2026-05-14

---

## 1. Overview

This document describes a reproducible methodology for validating AI-generated code against formal specifications using independently generated tests as the compliance oracle. The key insight: when tests and code are both generated from the same specification but **independently** (without seeing each other), test failures become a precise signal that the code violates the specification.

### The Pipeline

```
┌──────────────────┐     ┌──────────────────┐
│  EARS Spec       │     │  Design Document │
│  (Requirements)  │     │  (API Contracts) │
└────────┬─────────┘     └────────┬─────────┘
         │                        │
         ├────────────────────────┤
         │                        │
    ┌────▼────┐              ┌────▼────┐
    │  Test   │              │  Code   │
    │  Agent  │              │  Agent  │
    └────┬────┘              └────┬────┘
         │                        │
         │   (hidden from         │
         │    each other)         │
         │                        │
    ┌────▼────┐              ┌────▼────┐
    │  Tests  │              │  Source │
    │  (101)  │              │  Code   │
    └────┬────┘              └────┬────┘
         │                        │
         └──────────┬─────────────┘
                    │
              ┌─────▼─────┐
              │  Execute   │
              │  Tests on  │
              │  Source    │
              └─────┬─────┘
                    │
         ┌──────────┴──────────┐
         │                     │
    ┌────▼────┐          ┌────▼────┐
    │  PASS   │          │  FAIL   │
    │  Code   │          │  Code   │
    │  is     │          │  violates│
    │  spec-  │          │  spec   │
    │  compliant│         └────┬────┘
    └─────────┘               │
                         ┌────▼────┐
                         │Auto-fix │
                         │  Code   │
                         └─────────┘
```

---

## 2. Input Artifacts

### 2.1 EARS Specification (Requirements)

Requirements are written in EARS (Easy Approach to Requirements Syntax) format, organized by functional domain. Each requirement has a unique identifier (e.g., `REQ-F-097`) and a behavioral pattern (Event-driven, Ubiquitous, Unwanted, Complex).

**Example — REQ-F-097 (TransactionProcessing):**
```
REQ-F-097: [Event-driven] When the user confirms the transaction entry by 
entering 'Y' or 'y', the system shall assemble the complete transaction record 
with the generated transaction identifier, type code, category code, source, 
description, amount, card number, merchant identifier, merchant name, merchant 
city, merchant zip code, and timestamps, and write it to the transaction keyed 
dataset using the transaction identifier as the key.
```

**Example — REQ-F-125 (TransactionProcessing):**
```
REQ-F-125: [Event-driven] When a transaction passes all validation checks, the 
system shall copy the transaction ID, type code, category code, source, 
description, amount, merchant ID, merchant name, merchant city, merchant ZIP 
code, card number, and original timestamp from the daily transaction record to 
the transaction keyed dataset, and stamp the record with a processing timestamp 
formatted as YYYY-MM-DD HH.MM.SS.MIL0000 using the current system date and time.
```

### 2.2 Design Document (Shared API Contract)

The design document defines the technical architecture, API contracts, entity schemas, and response formats. Both test and code agents consume this document to ensure they align at the interface level without knowledge of each other's internals.

**Key elements both agents share:**

1. **Entity schema** — column types and lengths:
```java
@Column(name = "orig_timestamp", length = 26)
private String origTimestamp; // YYYY-MM-DD-HH.MM.SS.mmm0000

@Column(name = "proc_timestamp", length = 26)
private String procTimestamp; // YYYY-MM-DD-HH.MM.SS.mmm0000
```

2. **API contract** — request/response shapes:
```json
POST /api/v1/transactions
{
  "origTimestamp": "2026-05-13",
  "procTimestamp": "2026-05-13",
  "confirm": true
}

Response (201):
{
  "success": true,
  "data": { "transactionId": "0000000000000002" },
  "message": "Transaction added successfully"
}
```

3. **HTTP status codes** — 201 for created, 400 for validation, 500 for server error

---

## 3. Step 1 — Generating Tests from Spec + Design

The test agent receives:
- All EARS requirement files (16 domains)
- The design document (API contracts, entities, response formats)
- **No source code**

The agent generates integration tests and end-to-end tests that exercise the API contract and validate requirement behaviors.

### Example: Test generated from REQ-F-097

The test agent reads REQ-F-097 ("assemble the complete transaction record...and write it") and the design document's API contract for `POST /api/v1/transactions`, then generates:

```java
// --- REQ-F-097 (AddTransaction): Create transaction with confirm ---
@Test
@DisplayName("POST create transaction with confirm=true returns 201 - covers TransactionProcessing REQ-F-097")
void testCreateTransaction() {
    // Intent: Verify that creating a confirmed transaction succeeds and returns generated ID
    HttpHeaders headers = loginAsAdmin();

    Map<String, Object> request = new HashMap<>();
    request.put("cardNumber", "4111111111111111");
    request.put("accountId", "00000000001");
    request.put("typeCode", "01");
    request.put("categoryCode", "0001");
    request.put("source", "POS TERM");
    request.put("description", "Purchase at store");
    request.put("amount", 100.00);
    request.put("merchantId", "123456789");
    request.put("merchantName", "STORE");
    request.put("merchantCity", "NEW YORK");
    request.put("merchantZip", "10001");
    request.put("origTimestamp", "2026-05-13");
    request.put("procTimestamp", "2026-05-13");
    request.put("confirm", true);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

    ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl() + "/transactions", entity, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Map body = response.getBody();
    assertThat(body.get("success")).isEqualTo(true);
    Map data = (Map) body.get("data");
    assertThat(data.get("transactionId")).isNotNull();
    assertThat((String) body.get("message")).contains("Transaction added successfully");
}
```

**Key observations:**
- The test uses `"2026-05-13"` as a valid date input (per REQ-F-090: "original date and processing date conform to the format YYYY-MM-DD")
- It expects `201 CREATED` (per design doc: "Response when confirm is true (201)")
- It validates the response structure matches the API contract
- The test knows **nothing** about how timestamps are expanded or stored internally

---

## 4. Step 2 — Generating Code from Spec + Design

The code agent receives:
- All EARS requirement files
- The design document
- **No tests**

The code agent generates the full application including entities, services, controllers, and repositories.

### Example: Code generated from REQ-F-097 + REQ-F-125

The code agent reads REQ-F-097 ("assemble...and write it to the transaction keyed dataset") and REQ-F-125 ("stamp the record with a processing timestamp formatted as YYYY-MM-DD HH.MM.SS.MIL0000"), then generates:

**Entity** (from design document):
```java
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @Column(name = "transaction_id", length = 16, nullable = false)
    private String transactionId;

    @Column(name = "orig_timestamp", length = 26)
    private String origTimestamp;

    @Column(name = "proc_timestamp", length = 26)
    private String procTimestamp;
    // ... other fields
}
```

**Service** (from requirements):
```java
public class TransactionServiceImpl implements TransactionService {

    private static final String TIMESTAMP_SUFFIX = "-00.00.00.0000000"; // BUG: 7 fractional digits

    private String expandTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return timestamp;
        }
        if (timestamp.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return timestamp + TIMESTAMP_SUFFIX;  // "2026-05-13" + "-00.00.00.0000000" = 27 chars
        }
        return timestamp;
    }
}
```

**The bug:** The code agent interpreted REQ-F-125's format `YYYY-MM-DD HH.MM.SS.MIL0000` and produced 7 fractional digits (`0000000`) instead of the correct 6 (`000` milliseconds + `000` padding). This produces a 27-character string that overflows the entity's `@Column(length = 26)` constraint.

---

## 5. Step 3 — Running Tests on Generated Code

Tests are executed against the source code using Maven:

```bash
./mvnw test
```

### Result: Failure detected

```
[ERROR] TransactionControllerIntegrationTest.testCreateTransaction:147
expected: 201 CREATED
 but was: 500 INTERNAL_SERVER_ERROR

[ERROR] Tests run: 101, Failures: 24, Errors: 0, Skipped: 0
```

The server log reveals the root cause:

```
SQL Error: 22001, SQLState: 22001
Value too long for column "ORIG_TIMESTAMP CHARACTER VARYING(26)": 
"'2026-05-13-00.00.00.0000000' (27)"; SQL statement:
insert into transactions (...) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
```

---

## 6. Step 4 — Interpreting Failure as Spec Violation

### The violation chain

```
REQ-F-125 specifies: "processing timestamp formatted as YYYY-MM-DD HH.MM.SS.MIL0000"
                                                                          └── 26 chars total
         │
         ▼
Design document specifies: @Column(name = "orig_timestamp", length = 26)
         │                                                       └── enforced by DDL
         ▼
Code generates: "2026-05-13" + "-00.00.00.0000000" = 27 chars
         │                                    └── 7 fractional digits (should be 6)
         ▼
Database rejects: VARCHAR(26) overflow → DataIntegrityViolationException
         │
         ▼
HTTP response: 500 INTERNAL_SERVER_ERROR (instead of 201 CREATED)
         │
         ▼
Test assertion fails: expected 201, got 500
         │
         ▼
VERDICT: Code violates REQ-F-097 (transaction write must succeed for valid input)
```

### Why the test catches this

The test does not know about `expandTimestamp()`, `TIMESTAMP_SUFFIX`, or the internal storage format. It only knows:
1. The input `"2026-05-13"` is a valid date (per REQ-F-090)
2. A confirmed transaction with valid inputs must return 201 (per REQ-F-097)
3. The response must contain a generated `transactionId` (per REQ-F-098)

The 500 response definitively proves the code cannot fulfill REQ-F-097 for valid inputs.

---

## 7. Step 5 — Auto-Fixing Code from Test Failures

Given the test failure and server error log, an automated repair agent can:

1. **Identify the constraint:** `VARCHAR(26)` from `@Column(length = 26)` in `Transaction.java:46`
2. **Identify the violating value:** `'2026-05-13-00.00.00.0000000' (27)` — one character too long
3. **Locate the source:** `TransactionServiceImpl.TIMESTAMP_SUFFIX = "-00.00.00.0000000"` (17 chars)
4. **Compute the fix:** 10 (date) + suffix must equal 26, so suffix = 16 chars → `"-00.00.00.000000"`
5. **Apply the fix:**

```diff
- private static final String TIMESTAMP_SUFFIX = "-00.00.00.0000000";
+ private static final String TIMESTAMP_SUFFIX = "-00.00.00.000000";
```

6. **Verify:** Re-run `testCreateTransaction` → 201 CREATED → PASS

The same fix resolves the parallel failure in `BillPaymentServiceImpl`:

```diff
- String timestamp = now.format(TIMESTAMP_FORMATTER) + "0000";   // 23 + 4 = 27
+ String timestamp = now.format(TIMESTAMP_FORMATTER) + "000";    // 23 + 3 = 26
```

### Auto-fix traceability

| Step | Signal | Source |
|------|--------|--------|
| Detection | `expected: 201 but was: 500` | Test output |
| Root cause | `Value too long for column "ORIG_TIMESTAMP VARCHAR(26)": (27)` | Server error log |
| Constraint | `@Column(length = 26)` | `Transaction.java:46` |
| Violator | `TIMESTAMP_SUFFIX = "-00.00.00.0000000"` | `TransactionServiceImpl.java:31` |
| Spec authority | `YYYY-MM-DD HH.MM.SS.MIL0000` = 26 chars | REQ-F-125 |
| Fix | Remove 1 trailing digit | Arithmetic: 26 - 10 = 16 char suffix |
| Validation | Test passes | `./mvnw test -Dtest=...testCreateTransaction` |

---

## 8. Reproducibility

### Prerequisites
- Java 17+
- Maven 3.9+

### Steps

```bash
# 1. Clone the project
cd card-demo-specs-may7-codegen-cc

# 2. Verify tests are in the correct location
ls src/test/java/com/carddemo/integration/
ls src/test/java/com/carddemo/e2e/

# 3. Run all tests
./mvnw clean test

# 4. Observe results
# Tests run: 101, Failures: 0, Errors: 0, Skipped: 0

# 5. To reproduce the original failure, revert the timestamp fix:
# In TransactionServiceImpl.java, change:
#   "-00.00.00.000000" → "-00.00.00.0000000"
# Then run:
./mvnw test -Dtest=TransactionControllerIntegrationTest#testCreateTransaction

# Expected: 500 INTERNAL_SERVER_ERROR (spec violation detected)
```

### Project structure

```
card-demo-specs-may7-codegen-cc/
├── TransactionProcessingandAuthorization/
│   └── requirements.md          ← EARS spec (input to both agents)
├── DESIGN.md                    ← Shared API contract (input to both agents)
├── src/main/java/com/carddemo/ ← Generated source code
├── test/java/com/carddemo/     ← Generated tests (original location)
├── src/test/java/com/carddemo/ ← Tests (Maven-standard location)
└── pom.xml
```

---

## 9. Properties of This Approach

### 9.1 Information Hiding Prevents Leakage

The test agent never sees:
- `TIMESTAMP_SUFFIX`
- `expandTimestamp()` method
- Internal storage decisions

The code agent never sees:
- Test assertions
- Test data values
- Expected HTTP status codes

Both agents only see the **specification** (requirements + design). This eliminates the risk of tests being written to match buggy code, or code being written to pass specific test cases.

### 9.2 Failure Modes Map to Violation Types

| Test Outcome | Interpretation |
|---|---|
| All tests pass | Code is compliant with the spec at the API contract level |
| Test expects 201, gets 500 | Code has an internal error that prevents spec fulfillment |
| Test expects 400, gets 200 | Code fails to enforce a validation requirement |
| Test expects 400, gets 409 | Code checks constraints in wrong order vs. spec intent |
| Test expects specific message, gets different | Code deviates from spec's user-facing behavior |

### 9.3 Auto-Fix Feasibility

Test failures that produce structured error signals (SQL exceptions, type mismatches, clear assertion failures) are amenable to automated repair because:

1. **The constraint is machine-readable:** `@Column(length = 26)` is parseable
2. **The violation is quantified:** `(27)` vs the limit `(26)`
3. **The fix is arithmetically derivable:** reduce by 1 character
4. **The verification is automated:** re-run the specific test

---

## 10. Conclusion

This methodology provides a **closed-loop validation system** where:

1. Specifications are the single source of truth
2. Tests are an executable encoding of spec requirements
3. Code is an implementation that must satisfy those requirements
4. Test failures are **proof of spec violation** (not test bugs), because both artifacts derive from the same specification independently
5. Structured failures enable automated repair, closing the loop back to spec compliance

The timestamp overflow example demonstrates that even subtle off-by-one errors in internal implementation details are caught when they manifest as API-level contract violations — precisely because the test doesn't need to know about internals to detect the failure.
