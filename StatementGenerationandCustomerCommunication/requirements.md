# Statement Generation and Customer Communication — Requirements

## 1. Global Preconditions

- All operations require valid input data and appropriate authorization.
- Processing constraints and scheduling dependencies are documented in the Job Dependencies section.


> **Note:** The legacy system implements some of this processing as scheduled batch jobs. The modernized system may implement these requirements as batch, event-driven, or real-time processing provided the requirements below are met.

---

## 2. Monthly Customer Statement Generation


As a card operations team, I want monthly customer statements generated in both print and electronic (HTML) formats so that cardholders receive comprehensive account summaries showing their transactions, balances, and credit information for review and record-keeping.

**Restart/Recovery:** The statement generation process recreates all output from source data on each run; prior statement output files are cleared before generation begins, making the process idempotent.

### Requirements

**Transaction Preparation**

REQ-F-001: [Event-driven] When the monthly statement generation process executes, the system shall sort all transaction records from the transaction keyed dataset (legacy: AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS) by card number (primary key, ascending) and transaction identifier (secondary key, ascending), producing a chronologically ordered transaction set for statement inclusion.

REQ-F-002: [Ubiquitous] The system shall make the sorted transaction records available in an indexed data store keyed by a 32-byte composite key (card number + transaction identifier) with 350-byte fixed-length records to support keyed lookups during statement generation.

**Statement Generation — Cross-Reference Iteration**

REQ-F-003: [Event-driven] When the statement generation process begins, the system shall sequentially read all card-to-account cross-reference records from the card cross-reference data store (legacy: AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS), processing each card number, customer identifier, and account identifier to drive statement creation for every active card.

REQ-F-004: [Event-driven] When a cross-reference record is retrieved, the system shall look up the corresponding account record from the account keyed dataset (legacy: AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS) using the account identifier as the search key.

REQ-F-005: [Event-driven] When a cross-reference record is retrieved, the system shall look up the corresponding customer record from the customer keyed dataset (legacy: AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS) using the customer identifier as the search key.

**Statement Generation — Transaction Matching**

REQ-F-006: [Event-driven] When account and customer records have been retrieved for a card, the system shall search the sorted transaction data store for all transactions matching that card number and include each matching transaction's identifier, description, and amount in the statement output.

REQ-F-007: [Event-driven] When all transactions for a card have been processed, the system shall calculate and include the accumulated transaction total for that card in the statement output.

**Print Statement Output**

REQ-F-008: [Ubiquitous] The system shall produce a print-format statement file (legacy: AWS.M2.CARDDEMO.STATEMNT.PS) containing, for each card account: the formatted customer name (first, middle, and last name concatenated with spaces, trailing spaces trimmed), the customer mailing address (address lines 1 and 2 copied directly; address line 3 combined with state code, country code, and ZIP code separated by single spaces with trailing spaces trimmed), the account identifier, the current balance, the FICO credit score, and individual transaction lines showing transaction identifier, description, and amount.

**HTML Statement Output**

REQ-F-009: [Ubiquitous] The system shall produce an HTML-format statement file (legacy: AWS.M2.CARDDEMO.STATEMNT.HTML) as a valid HTML document containing: a document header with character encoding and title; bank header information (name and address); the customer name formatted as an HTML paragraph with 16-pixel font size; customer address lines as HTML paragraphs; the FICO credit score as an HTML paragraph; the account identifier in the statement header; the current balance labeled "Current Balance"; and a transaction summary table.

REQ-F-010: [Ubiquitous] The HTML transaction summary table shall contain column headers for Transaction ID (25% width), Transaction Details (55% width), and Amount (20% width), with each transaction rendered as a table row containing the transaction identifier (left-aligned), transaction description (left-aligned), and transaction amount (right-aligned).

REQ-F-011: [Ubiquitous] The system shall format the customer name in the HTML statement by concatenating first name, middle name, and last name with single spaces between components, trimming trailing spaces from each component.

REQ-F-012: [Ubiquitous] The system shall format customer address lines in the HTML statement identically to the print statement: address lines 1 and 2 copied directly, address line 3 combined with state code, country code, and ZIP code separated by single spaces with trailing spaces trimmed.

**Data Access and Routing**

REQ-F-013: [Event-driven] When a keyed read is requested for the account data store using an account identifier, the system shall return the matching account record containing account active status, current balance, credit limit, cash credit limit, open date, expiration date, reissue date, current cycle credit, current cycle debit, address ZIP code, and group identifier; if no matching record exists, the system shall return a not-found status.

REQ-F-014: [Event-driven] When a keyed read is requested for the customer data store using a customer identifier, the system shall return the matching customer record containing first name, middle name, last name, address lines, state code, country code, ZIP code, phone numbers, SSN, government-issued ID, date of birth, EFT account ID, primary card holder indicator, and FICO credit score; if no matching record exists, the system shall return a not-found status.

REQ-F-015: [Event-driven] When a sequential read is requested for the cross-reference data store, the system shall return the next cross-reference record containing card number, customer identifier, and account identifier; if no more records exist, the system shall return an end-of-data status.

REQ-F-016: [Event-driven] When a sequential read is requested for the transaction data store, the system shall return the next transaction record; if no more records exist, the system shall return an end-of-data status.

REQ-F-017: [Event-driven] When the transaction data store returns an end-of-data status during the initial transaction loading phase, the system shall transition to processing cross-reference records for statement generation.

**Processing Completion**

REQ-F-018: [Event-driven] When all cross-reference records have been processed (end-of-data reached), the system shall close the transaction, cross-reference, customer, and account data stores in sequence.

REQ-F-019: [Unwanted] If the transaction data store read returns an error status (neither success nor end-of-data), the system shall halt statement generation and propagate the error condition.

### Non-Functional Requirements

REQ-N-001: [Unwanted] If the statement generation process is interrupted, the system shall be capable of re-execution from the beginning, producing identical output, since all prior output is cleared before generation starts.

REQ-N-002: [Ubiquitous] The system shall write the print-format statement file and the HTML-format statement file as a coordinated unit; both files shall be produced completely or neither shall be considered valid output.

### Open Questions

OQ-1: The sort specification reformats the transaction record layout (moving card number to positions 1–16 and transaction ID to positions 17–262). Is the output record layout a business requirement for downstream consumers, or is it purely an implementation artifact of the legacy sort step? — Owner: statement delivery team

OQ-2: The rules reference a "bank name and address" in the HTML header but do not specify the actual values. Where are the bank identity details sourced from (configuration, hard-coded, or a reference data store)? — Owner: business operations

OQ-3: The process generates statements for every card in the cross-reference file without filtering by statement cycle date or account status. Should statements be generated only for active accounts or accounts within a specific billing cycle? — Owner: billing operations

---

## 3. Statement Text-to-PDF Conversion


As a statement delivery team, I want text-based customer card statements converted into PDF documents so that customers receive professionally formatted statements suitable for electronic distribution or archival.

### Requirements

REQ-F-020: [Event-driven] When the statement generation process completes successfully, the system shall convert the statement sequential file (legacy: AWS.M2.CARDDEMO.STATEMNT.PS) from text format into a PDF document, using the text-to-PDF conversion script (legacy: AWS.M2.LBD.TXT2PDF.EXEC).

REQ-F-021: [Ubiquitous] The system shall read the source statement text file in a non-destructive manner, ensuring the original statement data remains unchanged during the conversion process.

REQ-F-022: [Ubiquitous] The system shall produce a PDF output file containing the converted statement content suitable for electronic customer delivery or archival.

### Non-Functional Requirements

REQ-N-003: [Unwanted] If the preceding statement generation step fails, the system shall skip the text-to-PDF conversion entirely to prevent processing of invalid or incomplete statement data.

### Open Questions

OQ-4: What are the formatting and layout requirements for the generated PDF (e.g., page size, margins, fonts, branding elements)? — Owner: Customer Communications team

OQ-5: Where is the PDF output stored and what is its retention policy? The legacy output dataset name is not specified in the available rules. — Owner: Statement Delivery team

---

