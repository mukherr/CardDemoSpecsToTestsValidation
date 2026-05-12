# Credit Card Account Management — Requirements

## 1. Global Preconditions

- Users must be authenticated via the sign-on service (COSGN00C) before accessing any account management function.
- A valid session context must be established and passed between screens during online operations.
- Only authorized users may invoke the account update function; read-only inquiry may have separate access controls.
- Batch operations (data store initialization, multi-format extract) require successful completion of prior scheduling prerequisites before execution.


> **Note:** The legacy system implements some of this processing as scheduled batch jobs. The modernized system may implement these requirements as batch, event-driven, or real-time processing provided the requirements below are met.


### Navigation Context

- Users access account management functions by selecting the appropriate option from the main application menu, which transfers control to the relevant screen.
- Account View Inquiry is invoked via menu selection, presenting a screen where the user enters an account number to retrieve details.
- Account Update is invoked via menu selection, presenting a search screen; upon locating the account, the user is transferred to the update screen for modification.
- The account inquiry subroutine (CBACT01C) is invoked by online programs to retrieve account data from the account master data store (ACCTDAT) on behalf of the calling function.
- Batch functions (data store initialization, multi-format extract) operate independently of online navigation and are triggered through batch scheduling.

OQ-1: What menu option codes or selection criteria route users to Account View versus Account Update? Owner: Business/UI team.

---

## 2. Account Inquiry Service


As an integrated system, I want to submit account inquiry requests and receive detailed account information in response so that downstream processes can access current account data without direct data store access.

### Requirements

REQ-F-001: [Event-driven] When an account inquiry request is received from the input queue, the system shall validate that the request function is 'INQA' and that the account key is greater than zero before attempting account retrieval.

REQ-F-002: [Event-driven] When a valid account inquiry request is received (function 'INQA' with account key greater than zero), the system shall retrieve the account record from the account master file (legacy: AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS) using the provided account key.

REQ-F-003: [Event-driven] When the account record is successfully retrieved, the system shall populate a reply message containing: account identifier, account active status, account current balance, account credit limit, account cash credit limit, account open date, account expiration date, account reissue date, account current cycle credit, account current cycle debit, and account group identifier.

REQ-F-004: [Event-driven] When the account record is not found for the provided account key, the system shall create a reply message indicating invalid account ID and deliver it to the reply queue.

REQ-F-005: [Event-driven] When the request function is not 'INQA' or the account key is not greater than zero, the system shall create a reply message indicating invalid request parameters and deliver it to the reply queue.

REQ-F-006: [Event-driven] When the system retrieves a message from the input queue, the system shall wait up to 5 seconds for a message to become available before determining that no further messages exist.

REQ-F-007: [Event-driven] When a message is successfully retrieved from the input queue, the system shall extract the message ID, correlation ID, reply-to queue name, and message content, then increment the message counter.

REQ-F-008: [Event-driven] When no message is available within the 5-second wait interval, the system shall cease processing and initiate termination.

REQ-F-009: [Event-driven] When a reply message has been prepared, the system shall configure the message descriptor with the original message ID and correlation ID, set the message length to 1000 bytes, and deliver the reply to the output queue named 'CARD.DEMO.REPLY.ACCT'.

REQ-F-010: [Event-driven] When a system error occurs during processing (such as a failed account retrieval or queue operation failure), the system shall send an error message containing the error source identifier, application return message, condition code, reason code, and queue name to the error queue named 'CARD.DEMO.ERROR'.

REQ-F-011: [Ubiquitous] The system shall open the input queue for shared input operations, the reply queue 'CARD.DEMO.REPLY.ACCT' for output operations, and the error queue 'CARD.DEMO.ERROR' for output operations before beginning message processing.

REQ-F-012: [Unwanted] If any queue fails to open, the system shall record the error condition code, reason code, and queue name, then transition to error handling without processing messages.

REQ-F-013: [State-driven] While the system is terminating, the system shall close each queue that is marked as open.

### Non-Functional Requirements

REQ-N-001: [State-driven] While a reply message has been sent successfully, the system shall commit the current transaction to ensure message retrieval and reply delivery are synchronized before processing the next message.

REQ-N-002: [Unwanted] If the reply message delivery to the output queue fails, the system shall record the error details and transition to error handling without committing the transaction.

REQ-N-003: [Unwanted] If the error message delivery to the error queue fails, the system shall record the error details for diagnostic purposes.

### Open Questions

OQ-1: The reply message length is fixed at 1000 bytes regardless of actual content size. Should the modernized system use a variable-length message format sized to the actual reply content? — Owner: integration architecture team

OQ-2: The input queue name is retrieved dynamically at startup (from trigger queue information) while the reply and error queue names are hardcoded ('CARD.DEMO.REPLY.ACCT' and 'CARD.DEMO.ERROR'). Should all queue names be externally configurable in the modernized system? — Owner: integration architecture team

---

## 3. Credit Card Account Update


As an authorized user, I want to search for a credit card account by its identifier and update account and customer information so that account master data remains current and accurate.

### Requirements

**Account Lookup and Data Retrieval**

REQ-F-014: [Complex] While the program is in the initial state with no account details fetched, when the user provides an account identifier, the system shall validate that the account identifier is an 11-digit non-zero numeric value and reject it with an error message if invalid.

REQ-F-015: [Event-driven] When a valid account identifier is provided, the system shall read the card cross-reference file (legacy: AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS) to obtain the associated customer identifier, then retrieve the account record from the account keyed dataset (legacy: AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS) and the customer record from the customer keyed dataset (legacy: AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS).

REQ-F-016: [Unwanted] If the account identifier is not found in the card cross-reference file, or the account record is not found in the account keyed dataset, or the customer record is not found in the customer keyed dataset, the system shall display an appropriate error message and remain on the account update screen.

REQ-F-017: [Unwanted] If any data retrieval operation fails due to a system error, the system shall display an error message including the response code and remain on the account update screen.

**Input Parsing**

REQ-F-018: [Event-driven] When the user submits the screen with input data, the system shall parse all account and customer fields such that if the user entered an asterisk ('*') or left a field blank, the field value is cleared; otherwise the user's input is stored for processing.

REQ-F-019: [Event-driven] When the user submits numeric fields (credit limit, cash credit limit, current balance, current cycle credit, current cycle debit), the system shall validate that each value is numeric and convert it to internal numeric format, rejecting non-numeric input with an error message.

**Field Validation**

REQ-F-020: [State-driven] While the program has received and parsed user input, the system shall validate that the account active status is either 'Y' or 'N' and reject any other value with an error message.

REQ-F-021: [State-driven] While the program has received and parsed user input, the system shall validate that all date fields (account open date, account expiration date, account reissue date, customer date of birth) are valid calendar dates in YYYY-MM-DD format.

REQ-F-022: [State-driven] While the program has received and parsed user input, the system shall validate that credit limit, cash credit limit, current balance, current cycle credit, and current cycle debit are valid signed decimal numbers.

REQ-F-023: [State-driven] While the program has received and parsed user input, the system shall validate that customer first name, middle name, and last name contain only alphabetic characters and spaces.

REQ-F-024: [State-driven] While the program has received and parsed user input, the system shall validate that the customer address state code is a valid US state code.

REQ-F-025: [State-driven] While the program has received and parsed user input, the system shall validate that the customer address zip code is numeric and consistent with the state code.

REQ-F-026: [State-driven] While the program has received and parsed user input, the system shall validate that customer phone numbers are in valid North American format with valid area codes.

REQ-F-027: [State-driven] While the program has received and parsed user input, the system shall validate that the customer FICO credit score is between 300 and 850 inclusive.

REQ-F-028: [State-driven] While the program has received and parsed user input, the system shall validate that the customer Social Security Number follows the format XXX-XX-XXXX with valid component values.

REQ-F-029: [Unwanted] If any field fails validation, the system shall set an error indicator, display an error message identifying the first invalid field, and remain on the account update screen without saving.

**Date Validation Rules**

REQ-F-030: [Complex] While date validation is in progress, when the year component is evaluated, the system shall verify the year is supplied, is a 4-digit numeric value, and has a century value of 19 or 20, rejecting the date with an error message if any check fails.

REQ-F-031: [Complex] While date validation is in progress and the year has been validated, when the month component is evaluated, the system shall verify the month is supplied, is numeric, and is between 1 and 12 inclusive, rejecting the date with an error message if any check fails.

REQ-F-032: [Complex] While date validation is in progress and the month has been validated, when the day component is evaluated, the system shall verify the day is supplied, is numeric, and is between 1 and 31 inclusive, rejecting the date with an error message if any check fails.

REQ-F-033: [Complex] While all date component validations have passed and the day is 31, when the month is not a 31-day month (January, March, May, July, August, October, or December), the system shall reject the date with the error message 'Cannot have 31 days in this month.'

REQ-F-034: [Complex] While all date component validations have passed and the month is February, when the day is 30 or 31, the system shall reject the date with an appropriate error message indicating the day is invalid for February.

REQ-F-035: [Complex] While all date component validations have passed and the month is February and the day is 29, the system shall determine leap year status by dividing the year by 400 if the year ends in '00', otherwise by 4; if the remainder is zero the date is valid, otherwise the system shall reject the date with the error message 'Not a leap year. Cannot have 29 days in this month.'

REQ-F-036: [State-driven] While all internal date component and cross-field validations have completed without errors, the system shall mark the date as valid.

**Change Detection**

REQ-F-037: [Event-driven] When the user submits validated data, the system shall compare all account and customer fields from the current input against the previously retrieved values using case-insensitive comparison with whitespace trimming, and set a 'no changes detected' indicator if all fields match or a 'changes have occurred' indicator if any field differs.

**Confirmation Workflow**

REQ-F-038: [Event-driven] When the system detects that validated changes have occurred, the system shall transition to a confirmation state, display the updated data on screen, and prompt the user to press F5 to confirm or F12 to cancel the changes.

**Persisting Updates**

REQ-F-039: [Complex] While the user has confirmed changes and all data is validated, when the system receives confirmation (F5) to save, the system shall lock the account record for update, assemble the updated account data (account identifier, active status, current balance, credit limit, cash credit limit, open date, expiry date, reissue date, current cycle credit, current cycle debit, and account group identifier), and rewrite the account record with the updated values.

REQ-F-040: [Complex] While the user has confirmed changes and all data is validated, when the system receives confirmation (F5) to save, the system shall lock the customer record for update, assemble the updated customer data (customer identifier, first name, middle name, last name, address lines 1–3, state code, country code, zip code, phone numbers 1–2, Social Security Number, government-issued identifier, date of birth, EFT account identifier, primary cardholder indicator, and FICO credit score), and rewrite the customer record with the updated values.

REQ-F-041: [Unwanted] If the lock on the account record or customer record cannot be acquired because another user is updating the record, the system shall display a lock error message and not persist the changes.

REQ-F-042: [Unwanted] If the rewrite of the account record or customer record fails after the lock is acquired, the system shall display an update failure message.

REQ-F-043: [Unwanted] If the account record or customer record was changed by another user between the initial read and the rewrite, the system shall display a concurrency error message and not persist the stale changes.

**Navigation**

REQ-F-044: [Complex] While the program is awaiting user input, when the user presses a function key, the system shall accept Enter (proceed with input), F3 (exit to prior program or menu), F5 (confirm changes — valid only when changes are pending confirmation), and F12 (refresh/cancel — valid only when account details have been fetched); any other function key shall be treated as Enter.

REQ-F-045: [Event-driven] When the user presses F3, the system shall transfer control to the prior program recorded in session context (or to the menu program if none is recorded), passing the updated session context containing the current program identifier, user type, and entry mode indicator.

### Non-Functional Requirements

REQ-N-004: [Unwanted] If the system updates both the account keyed dataset and the customer keyed dataset during a confirmed save operation, the writes shall be performed as a transaction that either succeeds completely or fails completely.

### Open Questions

OQ-3: The validation rules reference "validate zip code matches state" and "validate phone numbers have valid area codes." Are there specific reference tables or external services that define valid zip-to-state mappings and valid area codes, or should these be configurable reference data? — Owner: Business domain expert

OQ-4: The rules state that an external date validation service is invoked after internal date checks pass. Should the modernized system replicate this two-stage validation (internal structural checks followed by an external service call), or is the internal validation sufficient? — Owner: Technical architect

---

## 4. Account View Inquiry


As a credit card operations user, I want to view comprehensive account details by entering an account number so that I can review account profiles including balances, limits, status, and customer information.

### Requirements

REQ-F-046: [Event-driven] When the program is invoked without an active session context (fresh entry), the system shall clear all working data and display the account view screen prompting the user to enter an account number.

REQ-F-047: [Event-driven] When the program is invoked with an active session context passed from a calling program, the system shall restore the passed session data into the working context and continue processing from the preserved state.

REQ-F-048: [State-driven] While the account view screen is displayed for the first time, the system shall present a prompt message guiding the user to enter an account number.

REQ-F-049: [Event-driven] When the user submits the account view screen, the system shall validate the entered account number by rejecting input that is blank, contains an asterisk, is non-numeric, or equals zero, and shall accept only valid 11-digit non-zero numeric account numbers.

REQ-F-050: [Event-driven] When account number validation fails, the system shall redisplay the account view screen with an error indication identifying the invalid input.

REQ-F-051: [Event-driven] When account number validation succeeds, the system shall retrieve the account data and redisplay the account view screen populated with the account details, clearing the prompt message.

REQ-F-052: [Unwanted] If an unexpected condition occurs during account data retrieval or processing, the system shall apply error handling and redisplay the account view screen with an appropriate error message.

REQ-F-053: [Ubiquitous] The system shall normalize user key input by mapping extended function keys (PF13–PF24) to their base equivalents (PF01–PF12) and shall accept only Enter and PF3 as valid keys on the account view screen; any other key shall be treated as Enter to re-prompt the user.

REQ-F-054: [Event-driven] When the user presses PF3 (exit), the system shall determine the navigation destination: if a calling program is recorded in the session context, navigate to that program; otherwise, navigate to the main menu, passing the updated session context containing this program's identifier as the calling program, the user type set to standard user, a fresh-entry indicator, and the current screen information.

---

## 5. Account Master Data Store Initialization


As a batch operations team, I want the account master data store rebuilt from the authoritative sequential source so that the Card Demo application has a clean, fully indexed account repository available for online transaction processing and account inquiries.

**Category:** setup
**Data flow:** Reads account sequential file (legacy: AWS.M2.CARDDEMO.ACCTDATA.PS), writes account keyed dataset (legacy: AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS).
**Migration relevance:** Defines the initial/refreshed state of the account master data store. The business need is a fully populated, indexed account data store keyed by account identifier; the rebuild-from-source pattern is an implementation choice.

### Requirements

REQ-F-055: [Event-driven] When the account data initialization process executes, the system shall replace the contents of the account keyed dataset with all account records from the account sequential file, making each record accessible by its 11-character numeric account identifier as the primary key.

REQ-F-056: [Ubiquitous] The account keyed dataset shall store each account record containing: account identifier, account active status, account current balance, account credit limit, account cash credit limit, account open date, account expiration date, account reissue date, account current cycle credit, account current cycle debit, account address zip code, and account group identifier.

REQ-F-057: [Unwanted] If the account keyed dataset does not exist prior to initialization, the system shall proceed with creation and loading without raising an error.

### Non-Functional Requirements

REQ-N-005: [Ubiquitous] The initialization process shall be idempotent — re-executing it shall produce the same resulting account keyed dataset regardless of the prior state of that data store.

### Open Questions

OQ-5: The legacy job is a setup/refresh job that rebuilds the entire account keyed dataset. Should the modernized system support incremental synchronization from the sequential source, or is a full replacement acceptable for ongoing operations? — Owner: data architecture team

OQ-6: The legacy specification defines concurrent access sharing (multiple readers, multiple writers). What are the concurrency requirements for the modernized account data store during and after initialization? — Owner: platform engineering team

---

## 6. Account Data Multi-Format Extract


As a batch operations team, I want account master data extracted and transformed into multiple output formats so that downstream business applications, reporting systems, and data exchange processes can consume account information in their required data structures.

### Requirements

REQ-F-058: [Event-driven] When the account data extract job executes, the system shall read all account records from the account keyed dataset (legacy: AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS) and produce three output files: the account compressed file, the account array dataset, and the account variable block file.

REQ-F-059: [Ubiquitous] The system shall produce the account compressed file (legacy: AWS.M2.CARDDEMO.ACCTDATA.PSCOMP) containing one record per account with the following fields mapped from the source: account identifier, account active status, account current balance, account credit limit, account cash credit limit, account open date, account expiration date, account reissue date, account current cycle credit, account current cycle debit, account address zip code, and account group identifier.

REQ-F-060: [Event-driven] When populating the account compressed file and the account current cycle debit is zero, the system shall substitute a default cycle debit value of 2525.00.

REQ-F-061: [Ubiquitous] The system shall produce the account array dataset (legacy: AWS.M2.CARDDEMO.ACCTDATA.ARRYPS) containing one record per account structured as the account identifier followed by five array occurrences, each containing a balance value and a cycle debit value.

REQ-F-062: [Ubiquitous] The system shall populate the array occurrences as follows: occurrence 1 with the account current balance and a cycle debit of 1005.00, occurrence 2 with the account current balance and a cycle debit of 1525.00, and occurrence 3 with a balance of -1025.00 and a cycle debit of -2500.00.

REQ-F-063: [Ubiquitous] The system shall produce the account variable block file (legacy: AWS.M2.CARDDEMO.ACCTDATA.VBPS) containing two records per account: the first record with the account identifier and account active status, and the second record with the account identifier, account current balance, account credit limit, and the reissue year extracted from the account reissue date.

REQ-F-064: [Event-driven] When producing the account variable block file, the system shall reformat the account reissue date into structured year, month, and day components and extract the year for inclusion in the balance and credit information record.

REQ-F-065: [Ubiquitous] The system shall process all account records from the account keyed dataset sequentially until the data source is exhausted.

REQ-F-066: [Event-driven] When the extract job begins, the system shall clear any previously existing versions of the account compressed file, the account array dataset, and the account variable block file before writing new output.

REQ-F-067: [Ubiquitous] The account compressed file records shall be ordered consistently with the key sequence of the account keyed dataset (by account identifier, ascending).

REQ-F-068: [Ubiquitous] The account array dataset records shall be ordered consistently with the key sequence of the account keyed dataset (by account identifier, ascending).

REQ-F-069: [Ubiquitous] The account variable block file records shall be ordered consistently with the key sequence of the account keyed dataset (by account identifier, ascending), with the two records per account written in sequence (identification/status record first, balance/credit record second).

### Non-Functional Requirements

REQ-N-006: [Unwanted] If the extract process is interrupted after partially writing output, the system shall ensure that writes to the account compressed file, the account array dataset, and the account variable block file are performed as a unit that either succeeds completely or fails completely, preventing inconsistent partial output across the three files.

### Open Questions

OQ-7: The array record structure specifies five occurrences but only three are explicitly populated (with defined balance and debit values). What values should occurrences 4 and 5 contain — are they zero-filled, or is there additional business logic not captured? — Owner: Account data domain expert

OQ-8: The default cycle debit value of 2525.00 applied when the source cycle debit is zero — is this a business policy (e.g., minimum assumed debit for reporting) or test data? Should this default apply in the modernized system? — Owner: Finance/reporting team

OQ-9: The magic numbers 1005.00, 1525.00, -1025.00, and -2500.00 used in the array format — do these represent specific business scenarios (e.g., multi-period projections, stress-test values) or are they test/placeholder data? — Owner: Account data domain expert

---

## 7. Job Dependencies

The following dependencies are inferred from shared data store access:

- **ACCTFILE.jcl** (Section 5: Account Master Data Store Initialization) → **READACCT.jcl** (Section 6: Account Data Multi-Format Extract) (via `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS`)
- **READACCT.jcl** (Section 6: Account Data Multi-Format Extract) → **ACCTFILE.jcl** (Section 5: Account Master Data Store Initialization) (via `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TRANCATG.PS`, `AWS.M2.CARDDEMO.TRANTYPE.PS`, `AWS.M2.CARDDEMO.USRSEC.PS`)

*These dependencies represent data flow ordering constraints. The implementation should ensure predecessor jobs complete before successors execute.*

---



## Shared Capability Dependencies

This capability depends on the following shared capabilities.
Do not reimplement their behavior — integrate with the shared service.

- **COSGN00C** (`_shared/COSGN00C/`) — As a user of the CardDemo application, I want to sign on with my credentials so that I am authenticated and directed to the appropriate application functions based on my role.


---

## Security Hardening Requirements

**Based on Security Scan Findings:** #4, #11, #21, #22

### Account Access Authorization

REQ-SEC-001: [Event-driven] When a user attempts to view or update account information, the system shall verify that the account belongs to the authenticated user or that the user has explicit authorization to access that account.

REQ-SEC-002: [Unwanted] If a user attempts to access an account they do not own or are not authorized to access, the system shall reject the request with a generic "account not found" error rather than revealing the account exists but is unauthorized.

REQ-SEC-003: [Ubiquitous] Regular users shall only access their own accounts; customer service representatives shall access only accounts for customers assigned to them; administrators may access all accounts with audit logging.

REQ-SEC-004: [Ubiquitous] The system shall log all account data access operations including: timestamp, user ID, account ID, fields accessed, and operation type (view, update, delete).

### SSN and PII Masking

REQ-SEC-005: [Ubiquitous] The system shall mask SSN values in all account lookup and display operations, showing only the last 4 digits (e.g., "***-**-1234") and never displaying the full SSN.

REQ-SEC-006: [Ubiquitous] The system shall mask government ID values in displays, showing only partial information unless full access is explicitly authorized and logged.

REQ-SEC-007: [Event-driven] When account information is returned from lookup operations, the system shall apply masking to all PII fields (SSN, government ID, date of birth) before returning the data to the caller.

REQ-SEC-008: [Ubiquitous] The system shall encrypt SSN and government ID fields at rest in the database using field-level encryption with AES-256 or equivalent.

### Thread Safety and Concurrency Control

REQ-SEC-009: [Ubiquitous] The system shall implement proper thread synchronization for all account update operations to prevent race conditions and data corruption in concurrent access scenarios.

REQ-SEC-010: [Event-driven] When an account update operation begins, the system shall acquire an exclusive lock on the account record to prevent concurrent modifications.

REQ-SEC-011: [Event-driven] When an account update operation completes or fails, the system shall release the account lock to allow other operations to proceed.

REQ-SEC-012: [Unwanted] If an account lock cannot be acquired within a defined timeout period (e.g., 5 seconds), the system shall reject the update request and display an error message indicating the account is currently being modified.

REQ-SEC-013: [Ubiquitous] The system shall use database-level locking mechanisms (pessimistic locking, optimistic locking with version numbers) rather than application-level locks to ensure consistency across multiple application instances.

REQ-SEC-014: [Ubiquitous] The system shall avoid using class-level or module-level mutable shared state (dictionaries, lists, caches) that can cause data leakage between concurrent requests in multi-threaded environments.

REQ-SEC-015: [Event-driven] When account data is cached for performance, the system shall use request-scoped or session-scoped caching rather than shared global caches to prevent cross-user data leakage.

### Field-Level Update Controls (Mass Assignment Prevention)

REQ-SEC-016: [Ubiquitous] The system shall implement an explicit allowlist of modifiable account fields for update operations, rejecting attempts to modify fields not in the allowlist.

REQ-SEC-017: [Ubiquitous] The allowed modifiable account fields shall be limited to: credit_limit (with authorization), account_status (with authorization), and contact preferences, explicitly excluding: account_id, customer_id, account_number, and creation_date.

REQ-SEC-018: [Unwanted] If an account update request includes fields not in the allowlist, the system shall reject the entire update request and display an error message indicating invalid fields were provided.

REQ-SEC-019: [Event-driven] When an account update is processed, the system shall iterate only over the allowlisted fields and shall ignore any other fields present in the update request.

REQ-SEC-020: [Ubiquitous] The system shall validate each field value against its specific validation rules (data type, range, format) before applying the update.

### Version Control and Optimistic Locking

REQ-SEC-021: [Ubiquitous] The system shall implement version numbering for account records, incrementing the version number with each update to detect concurrent modification conflicts.

REQ-SEC-022: [Event-driven] When an account update is submitted, the system shall verify that the version number in the update request matches the current version in the database.

REQ-SEC-023: [Unwanted] If the version number does not match (indicating another user modified the account), the system shall reject the update and display an error message indicating the account was modified by another user.

REQ-SEC-024: [Event-driven] When an account update succeeds, the system shall increment the version number and return the new version to the caller for subsequent updates.

### Account Balance and Credit Limit Validation

REQ-SEC-025: [Unwanted] If a credit limit update would result in a credit limit less than the current account balance, the system shall reject the update and display an error message.

REQ-SEC-026: [Unwanted] If an account balance update would result in a balance exceeding the credit limit, the system shall reject the update and display an error message.

REQ-SEC-027: [Ubiquitous] The system shall validate that account balance and available credit calculations are consistent (available_credit = credit_limit - current_balance) before and after every update.

REQ-SEC-028: [Event-driven] When account balance or credit limit is modified, the system shall recalculate available credit atomically within the same database transaction.

### Authorization for Sensitive Updates

REQ-SEC-029: [Event-driven] When a credit limit increase is requested, the system shall require additional authorization (supervisor approval, credit check, or elevated privileges) before applying the change.

REQ-SEC-030: [Event-driven] When an account status change is requested (e.g., closing an account, freezing an account), the system shall verify the user has appropriate authorization and shall log the change with justification.

REQ-SEC-031: [Ubiquitous] The system shall implement different authorization levels for different types of account updates: basic updates (contact preferences) require standard user access, financial updates (credit limit) require elevated privileges, and status changes (close account) require administrative access.

### Error Handling

REQ-SEC-032: [Unwanted] If an account update error occurs, the system shall display a generic error message to the user without exposing internal system details, database errors, or account details of other users.

REQ-SEC-033: [Ubiquitous] The system shall log detailed error information internally while displaying only generic messages to users.

### Non-Functional Security Requirements

REQ-SEC-N-001: [Ubiquitous] All account update operations shall be atomic, ensuring that either all changes succeed or all changes are rolled back with no partial updates.

REQ-SEC-N-002: [Ubiquitous] The system shall implement audit logging for all account modifications including: timestamp, user ID, account ID, fields modified, old values, new values, and operation result.

REQ-SEC-N-003: [Ubiquitous] The system shall implement rate limiting on account update operations to prevent automated abuse (e.g., maximum 20 account updates per user per hour).

REQ-SEC-N-004: [Ubiquitous] The system shall support horizontal scaling with multiple application instances without data corruption or race conditions through proper use of database-level locking and stateless application design.

### Open Questions

OQ-SEC-01: What is the approval workflow for credit limit increases above a certain threshold? — Owner: Credit risk team / Business analyst

OQ-SEC-02: What are the valid account status values and what authorization is required to change between each status? — Owner: Business analyst / Product owner

OQ-SEC-03: Should the system implement optimistic locking (version numbers) or pessimistic locking (database row locks) for account updates? — Owner: Application architecture team / Database team

OQ-SEC-04: What is the maximum allowed credit limit, and are there different limits for different customer segments? — Owner: Credit risk team / Business analyst
