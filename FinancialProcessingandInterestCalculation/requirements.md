# Financial Processing and Interest Calculation — Requirements

## 1. Global Preconditions

- All operations require valid input data and appropriate authorization.
- Processing constraints and scheduling dependencies are documented in the Job Dependencies section.


> **Note:** The legacy system implements some of this processing as scheduled batch jobs. The modernized system may implement these requirements as batch, event-driven, or real-time processing provided the requirements below are met.

---

## 2. Monthly Interest Calculation and Posting


As a billing operations team, I want monthly interest charges calculated on outstanding account balances using applicable interest rates and posted as transactions so that customer accounts accurately reflect accrued interest charges for the billing cycle.

**Restart/Recovery:** The process updates account records in place and writes interest transaction records; if interrupted, partial updates may exist with no automatic rollback.

### Requirements

REQ-F-001: [Event-driven] When the monthly interest calculation job executes with a processing date parameter, the system shall process all transaction category balance records from the transaction catalog keyed dataset (legacy: AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS), calculate interest for each eligible account, update account balances, and generate interest transaction records.

REQ-F-002: [Event-driven] When a new account identifier is encountered in the transaction category balance stream, the system shall retrieve the account master record from the account keyed dataset (legacy: AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS) using the account identifier to obtain account status, group identifier, current balance, and cycle-to-date amounts.

REQ-F-003: [Event-driven] When a new account identifier is encountered, the system shall retrieve the card cross-reference record from the card xref keyed dataset (legacy: AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS) using the account identifier to obtain the associated card number and customer identifier for transaction recording.

REQ-F-004: [Complex] While a transaction category record is being processed, when the interest rate lookup is requested, the system shall retrieve the discount group record from the discount group keyed dataset (legacy: AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS) using a composite key of account group identifier, transaction type code, and transaction category code.

REQ-F-005: [Event-driven] When the discount group record is not found for the account's group identifier, the system shall retrieve the default discount group record using 'DEFAULT' as the account group identifier with the same transaction type code and transaction category code to obtain a fallback interest rate.

REQ-F-006: [Event-driven] When the interest rate for a transaction category is non-zero, the system shall calculate the monthly interest as (transaction category balance × interest rate) ÷ 1200 and add the result to the total interest accumulator for the account.

REQ-F-007: [Event-driven] When all transaction categories for an account have been processed, the system shall add the accumulated interest to the account current balance, set the current cycle credit amount to zero, set the current cycle debit amount to zero, and persist the updated account record to the account keyed dataset.

REQ-F-008: [Event-driven] When monthly interest is calculated for a transaction category, the system shall generate an interest transaction record with: a unique transaction identifier composed of the processing date and an incrementing suffix; transaction type code '01'; transaction category code '05'; source 'System'; description 'Int. for a/c' concatenated with the account identifier; amount equal to the calculated monthly interest; zero merchant identifier; blank merchant details; the card number from the cross-reference record; and the current system timestamp formatted as YYYY-MM-DD-HH.MM.SS.mmm0000 for both original and processing timestamps.

REQ-F-009: [Event-driven] When an interest transaction record is constructed, the system shall write it to the system transaction file (legacy: AWS.M2.CARDDEMO.SYSTRAN) as a new version for downstream processing.

REQ-F-010: [Ubiquitous] The system shall format the current system timestamp as YYYY-MM-DD-HH.MM.SS.mmm0000 (hyphens between date components, dots between time components, followed by the fixed suffix '0000') for use in interest transaction records.

REQ-F-011: [Ubiquitous] The system shall process transaction category balance records ordered by account identifier, grouping all transaction categories belonging to the same account before updating that account's balance and generating transaction records.

### Non-Functional Requirements

REQ-N-001: [Unwanted] If the interest calculation process writes to both the account keyed dataset and the system transaction file for the same account, the system shall ensure that both writes succeed completely or fail completely to prevent inconsistency between posted interest and account balance updates.

### Open Questions

OQ-1: The processing date parameter in the legacy system is hardcoded as '2022071800'. Should the modernized system accept the processing date as a configurable runtime parameter, and does the time component (hours) affect processing logic? — Owner: billing operations team

OQ-2: The interest rate formula divides by 1200, implying the rate stored in the discount group file is an annual percentage rate. Should the system validate that the rate is within an acceptable range before applying the calculation? — Owner: finance/product team

OQ-3: When the discount group record is not found for either the account's group or the 'DEFAULT' group, should the system skip interest calculation for that category, reject the record, or raise an error? — Owner: billing operations team

---

