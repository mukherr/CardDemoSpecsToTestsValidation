# Transaction Processing and Authorization — Requirements

## 1. Global Preconditions

- User must be authenticated via the sign-on process (COSGN00C) before accessing any online transaction processing screens.
- A valid session context must be established and passed between screens to maintain user state and navigation history.
- Online transaction functions require an active authenticated session; batch functions execute under scheduled operations authority without interactive sign-on.
- Date validation utility (CSUTLDTC) must be available for all transaction screens requiring date input or display.


> **Note:** The legacy system implements some of this processing as scheduled batch jobs. The modernized system may implement these requirements as batch, event-driven, or real-time processing provided the requirements below are met.


### Navigation Context

- From the main application menu, users select a transaction processing option which navigates to the transaction list screen (CT00).
- From the transaction list screen, users page through transactions and select an individual record, which transfers control to the transaction detail screen (CT01) with the selected transaction identifier passed via session context.
- From the transaction list or a menu option, users may navigate to the add-transaction screen (CT02) to create a new transaction record.
- The account/card authorization inquiry screen (CP00) is accessible from the menu for reviewing authorization-related account data.
- All online screens provide a return path to the main menu or previous screen via standard navigation keys.

- **OQ-NAV-01** (Owner: Application Team): The exact menu structure and whether CP00 is reachable from within the transaction list flow or only from the top-level menu is not explicitly documented. Clarify navigation entry points for the authorization inquiry screen.

---

## 2. Credit Card Authorization Request Processing


As a card authorization system, I want to receive authorization requests from a message queue, validate card and account information, evaluate available credit, and return approval or decline decisions so that merchants receive timely authorization responses and authorization records are maintained for tracking.

### Requirements

**Initialization**

REQ-F-001: [Event-driven] When the authorization processing service starts, the system shall retrieve the trigger message metadata to extract the request queue name and reply queue name, and set the message wait interval to 5000 milliseconds.

REQ-F-002: [Event-driven] When the request message queue open operation is initiated, the system shall open the queue for shared input access and mark the queue as open.

REQ-F-003: [Unwanted] If the request message queue fails to open, the system shall log a critical error with the completion code and reason code and transition to error handling.

REQ-F-004: [Event-driven] When the IMS database access scheduling operation is initiated, the system shall schedule the program specification block for database access and mark it as scheduled.

REQ-F-005: [Unwanted] If IMS database access scheduling fails, the system shall log a critical error with error location 'I001' and the IMS return code.

**Message Loop Processing**

REQ-F-006: [State-driven] While messages are available in the request queue and the message processing count is below 500, the system shall retrieve and process each authorization request message, incrementing the message count after each message.

REQ-F-007: [Event-driven] When the message processing count exceeds 500, the system shall terminate the message processing loop.

REQ-F-008: [Complex] While the request queue is open and ready for message retrieval, when a message retrieval operation is attempted, the system shall retrieve the next message from the queue and extract the correlation ID and reply-to queue name from the message metadata.

REQ-F-009: [Event-driven] When no message is available in the request queue (normal end-of-queue condition), the system shall terminate the message processing loop.

REQ-F-010: [Unwanted] If message retrieval fails for reasons other than an empty queue, the system shall log a critical error with the completion and reason codes.

**Card and Account Validation**

REQ-F-011: [Complex] While the card number from the authorization request is available, when a card cross-reference record retrieval is attempted, the system shall retrieve the cross-reference record by card number from the card cross-reference file (legacy: AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS).

REQ-F-012: [Unwanted] If the card cross-reference record is not found, the system shall mark the card as not found, mark the account as not found, and log a warning.

REQ-F-013: [Unwanted] If the card cross-reference record retrieval fails, the system shall log a critical error with error location 'C001' and the response codes.

REQ-F-014: [Complex] While the account ID from the cross-reference record is available, when an account master record retrieval is attempted, the system shall retrieve the account record by account ID from the account master file and load account details including credit limit and current balance.

REQ-F-015: [Unwanted] If the account master record is not found, the system shall mark the account as not found and log a warning.

REQ-F-016: [Unwanted] If the account master record retrieval fails, the system shall log a critical error with error location 'C002' and the response codes.

REQ-F-017: [Complex] While the customer ID from the cross-reference record is available, when a customer master record retrieval is attempted, the system shall retrieve the customer record by customer ID from the customer master file and load customer details.

REQ-F-018: [Unwanted] If the customer master record is not found, the system shall mark the customer as not found and log a warning.

REQ-F-019: [Unwanted] If the customer master record retrieval fails, the system shall log a critical error with error location 'C003' and the response codes.

**Authorization Summary Retrieval**

REQ-F-020: [Complex] While the account ID is available and the IMS database is accessible, when an authorization summary record retrieval is attempted, the system shall retrieve the authorization summary record by account ID from the IMS database.

REQ-F-021: [Event-driven] When the authorization summary record is found, the system shall mark the summary as found and use it for credit limit and balance calculations.

REQ-F-022: [Event-driven] When the authorization summary record is not found, the system shall mark the summary as not found so that a new summary will be created.

REQ-F-023: [Unwanted] If the authorization summary record retrieval fails, the system shall log a critical error with error location 'I002' and the IMS return code.

**Authorization Decision**

REQ-F-024: [Complex] While the authorization request has been parsed and account/customer data has been retrieved, when the authorization decision logic is evaluated, the system shall calculate available credit as the credit limit minus the current credit balance (from the authorization summary if it exists, or from the account master if no summary exists).

REQ-F-025: [Event-driven] When the transaction amount exceeds available credit, the system shall decline the authorization with reason code '4100' (insufficient funds).

REQ-F-026: [Event-driven] When the card, account, or customer is not found, the system shall decline the authorization with reason code '3100'.

REQ-F-027: [Event-driven] When the card is not active, the system shall decline the authorization with reason code '4200'.

REQ-F-028: [Event-driven] When the account is closed, the system shall decline the authorization with reason code '4300'.

REQ-F-029: [Event-driven] When card fraud is detected, the system shall decline the authorization with reason code '5100'.

REQ-F-030: [Event-driven] When merchant fraud is detected, the system shall decline the authorization with reason code '5200'.

REQ-F-031: [Event-driven] When a decline condition exists that does not match any specific reason, the system shall decline the authorization with reason code '9000'.

REQ-F-032: [Event-driven] When the authorization is approved, the system shall set the response code to '00' and the approved amount to the transaction amount.

REQ-F-033: [Event-driven] When the authorization is declined, the system shall set the response code to '05' and the approved amount to zero.

**Authorization Response**

REQ-F-034: [Complex] While the authorization decision has been made and the response fields are populated, when the response message is ready to be sent, the system shall format the response message with card number, transaction ID, authorization ID, response code, reason, and approved amount, and send it to the reply queue identified in the original request message using the correlation ID from the request, non-persistent delivery, and a 50-decisecond expiry.

REQ-F-035: [Unwanted] If the authorization response send operation fails, the system shall log a critical error with error location 'M004' and the completion and reason codes.

**Termination**

REQ-F-036: [Complex] While authorization processing is terminating and the request queue is open, when the close operation is initiated, the system shall close the request message queue and mark it as closed.

REQ-F-037: [Unwanted] If the request queue close operation fails, the system shall log a warning error with error location 'M005' and the completion and reason codes, but continue termination.

**Error Logging**

REQ-F-038: [Event-driven] When an error condition is detected during authorization processing, the system shall populate an error record with current date/time, application ID, program name, error location, error level (warning, info, or critical), subsystem identifier, error codes, error message, and event key (typically the card number), and write the error record to the audit trail.

REQ-F-039: [Event-driven] When the error level is critical, the system shall transition to end-of-routine to terminate processing.

### Non-Functional Requirements

REQ-N-001: [Event-driven] When a message has been fully processed (authorization decision made, response sent, and database updated), the system shall commit the transaction before processing the next message.

REQ-N-002: [Ubiquitous] The system shall process a maximum of 500 messages per invocation to prevent resource exhaustion.

### Open Questions

OQ-1: The 50-decisecond expiry on the authorization response message — is this a business SLA requirement (5 seconds for response validity) or an implementation artifact? — Owner: payments domain expert

OQ-2: The message wait interval of 5000 milliseconds — should this be configurable in the modernized system, or is it a fixed business requirement? — Owner: integration architecture team

OQ-3: The rules reference card fraud detection and merchant fraud detection as inputs to the decline decision, but the mechanism for setting these fraud flags is not described in this program. What upstream process or data source provides the fraud indicators? — Owner: fraud operations team

---

## 3. Transaction List Browsing and Selection


As a CardDemo application user, I want to view a paginated list of transactions and select individual transactions for detailed viewing so that I can review transaction history and navigate to specific transaction details.

### Requirements

REQ-F-040: [Event-driven] When the transaction list function is invoked without an active session context, the system shall redirect the user to the sign-on screen to establish a new session.

REQ-F-041: [Event-driven] When the transaction list function is invoked with a session context for the first time (not a re-entry), the system shall clear the display area and initiate the initial transaction list retrieval using forward page navigation starting from the beginning of the transaction data store.

REQ-F-042: [Event-driven] When the user re-enters the transaction list screen, the system shall evaluate the key pressed and route to the appropriate handler: Enter key for transaction selection processing, PF7 for previous page navigation, PF8 for next page navigation, or PF3 for exit to the menu program.

REQ-F-043: [Unwanted] If the user presses a key other than Enter, PF3, PF7, or PF8, the system shall display the error message 'Invalid key pressed. Please see below...' and retain the current screen content.

REQ-F-044: [Ubiquitous] The system shall display the transaction list screen header populated with the application title, transaction code, program name, current date in MM/DD/YY format, and current time in HH:MM:SS format.

REQ-F-045: [Event-driven] When the user presses PF8 to navigate to the next page, the system shall use the last transaction ID from the current page as the search key and initiate forward page navigation to display the next set of up to 10 transactions.

REQ-F-046: [Unwanted] If the user presses PF8 and no next page is available, the system shall display the message 'You are already at the bottom of the page...' and retain the current screen content.

REQ-F-047: [Event-driven] When the user presses PF7 to navigate to the previous page, the system shall use the first transaction ID from the current page as the search key and initiate backward page navigation to display the preceding set of up to 10 transactions, provided the current page number is greater than 1.

REQ-F-048: [Unwanted] If the user presses PF7 and the current page is already the first page, the system shall display the message 'You are already at the top of the page...' and retain the current screen content.

REQ-F-049: [Event-driven] When the user enters a transaction ID in the search field and presses Enter, the system shall reset the page number to 0 and initiate forward page navigation starting from the specified transaction ID, clearing the search field upon successful retrieval.

REQ-F-050: [Ubiquitous] The system shall validate that the transaction ID search input is numeric; if the input is non-numeric, the system shall display the error message 'Tran ID must be Numeric ...' and retain the current screen.

REQ-F-051: [Event-driven] When the user enters a selection indicator in any of the 10 transaction rows displayed on the screen, the system shall capture the selection indicator and the corresponding transaction identifier from that row.

REQ-F-052: [Event-driven] When the user presses Enter without marking any transaction row with a selection indicator, the system shall clear any previously stored selection information to prevent processing of stale selections.

REQ-F-053: [Event-driven] When a transaction selection has been captured, the system shall validate that the selection indicator is 'S' (case-insensitive); if the indicator is any other value, the system shall display the error message 'Invalid selection. Valid value is S'.

REQ-F-054: [Event-driven] When the user selects a transaction with a valid selection indicator ('S' or 's'), the system shall transfer control to the transaction detail program, passing the session context including the originating program name, originating transaction identifier, the selected transaction identifier, the authenticated user ID, user type, active account identifier, card number, and a context flag indicating fresh entry.

REQ-F-055: [Event-driven] When the user presses PF3, the system shall transfer control to the menu program, passing the session context including the originating program name, originating transaction identifier, the authenticated user ID, user type, and a context flag indicating fresh entry.

REQ-F-056: [Unwanted] If the target program for navigation is empty or contains only spaces, the system shall default the target to the sign-on screen to ensure a valid destination is always invoked.

REQ-F-057: [Ubiquitous] The system shall initialize the next-page availability flag to 'No' at the start of each transaction list session.

### Open Questions

OQ-4: The transaction list displays up to 10 rows per page. Is this page size a fixed business requirement or should it be configurable? — Owner: product owner

OQ-5: The selection logic is case-insensitive for 'S'/'s'. Should additional selection indicators (e.g., 'U' for update, 'D' for delete) be supported in the modernized system? — Owner: business analyst

---

## 4. Transaction Detail Inquiry


As a card operations user, I want to view detailed information about a specific credit card transaction so that I can review transaction attributes including card number, merchant information, amounts, and timestamps.

### Requirements

REQ-F-058: [Event-driven] When the transaction is invoked with no session context, the system shall navigate to the signon screen to require authentication before accessing transaction inquiry functionality.

REQ-F-059: [Event-driven] When a session context is received from a calling program, the system shall load the session context including user ID, user type, program context, customer information, account information, card number, and transaction selection state.

REQ-F-060: [Event-driven] When the program is entered for the first time without a pre-selected transaction, the system shall display the transaction view screen with an empty transaction ID field, allowing the user to enter a transaction ID.

REQ-F-061: [Complex] While the program is being entered for the first time, when a transaction has been pre-selected from a prior screen, the system shall populate the transaction ID input field with the pre-selected transaction ID, validate and retrieve the transaction from the transaction data store (legacy: CARDDEMO.TRANSACT.VSAM.KSDS), and display the transaction details.

REQ-F-062: [Event-driven] When the user enters a non-empty transaction ID and presses Enter, the system shall clear all transaction detail fields and retrieve the transaction record from the transaction data store using the entered transaction ID.

REQ-F-063: [Event-driven] When a transaction record is successfully retrieved from the transaction data store, the system shall display all transaction detail fields including transaction ID, card number, transaction type, category, source, amount, description, original and processing timestamps, and merchant information (merchant ID, name, city, and ZIP code).

REQ-F-064: [Event-driven] When the user presses Enter with an empty transaction ID field, the system shall display the error message 'Tran ID can NOT be empty...' and prompt the user to provide a transaction ID.

REQ-F-065: [Unwanted] If the transaction ID is not found in the transaction data store, the system shall display the error message 'Transaction ID NOT found...' and prompt the user to correct the input.

REQ-F-066: [Unwanted] If a system error occurs while retrieving the transaction record, the system shall display the error message 'Unable to lookup Transaction...' and prompt the user to retry.

REQ-F-067: [Event-driven] When the user presses PF4 to clear the screen, the system shall reset all transaction detail fields to empty and redisplay the transaction view screen with all fields cleared.

REQ-F-068: [Unwanted] If the user presses any key other than Enter, PF3, PF4, or PF5, the system shall display an invalid key error message.

REQ-F-069: [Ubiquitous] The system shall display the screen header with title lines, program name, current date formatted as MM/DD/YY, and current time formatted as HH:MM:SS.

REQ-F-070: [Ubiquitous] The system shall display any error message in the error message area of the transaction view screen.

REQ-F-071: [Event-driven] When the user presses PF3 (back) and a calling program is recorded in the session context, the system shall navigate to that calling program, passing the session context including the current program and transaction identifiers as the originating program and transaction, and resetting the program context flag to indicate a fresh entry.

REQ-F-072: [Event-driven] When the user presses PF3 (back) and no calling program is recorded in the session context, the system shall navigate to the menu screen.

REQ-F-073: [Event-driven] When the user presses PF5 (menu), the system shall navigate to the transaction menu screen, passing the session context including the current program and transaction identifiers as the originating program and transaction, and resetting the program context flag to indicate a fresh entry.

REQ-F-074: [Ubiquitous] The system shall record the current program and transaction identifiers as the calling program and transaction in the session context before transferring control to any target program.

REQ-F-075: [Unwanted] If the target program for navigation is empty or contains only spaces, the system shall default the target program to the signon screen.

REQ-F-076: [Ubiquitous] The system shall transfer control to the target program passing the full session context including user ID (alphanumeric, up to 8 characters), user type (1 character), program context flag, customer identifier (numeric, 9 digits), customer name fields, account identifier (numeric, 11 digits), account status, card number (numeric, 16 digits), originating program (alphanumeric, up to 8 characters), and originating transaction (alphanumeric, up to 4 characters).

---

## 5. Add New Card Transaction


As a card system user, I want to add new transaction records by entering transaction details so that card transactions are recorded in the system with proper validation and unique identification.

### Requirements

REQ-F-077: [Event-driven] When the transaction entry function is invoked with no session context, the system shall navigate to the sign-on function.

REQ-F-078: [Event-driven] When the transaction entry function is invoked with a session context, the system shall restore the session data including user identifier, user type, customer identifier, account identifier, and card number.

REQ-F-079: [Unwanted] If the target program for navigation is blank or contains null values, the system shall default navigation to the sign-on function.

REQ-F-080: [Event-driven] When the user presses PF3 during transaction entry and a previous program is recorded in the session context, the system shall navigate to that previous program, passing the current program identifier, transaction identifier, user identifier, user type, customer identifier, account identifier, and card number in the session context.

REQ-F-081: [Event-driven] When the user presses PF3 during transaction entry and no previous program is recorded, the system shall navigate to the main menu function, passing the session context.

REQ-F-082: [Ubiquitous] The system shall record the current program and transaction identifier as the navigation source and reset the re-entry flag before transferring control to any target function.

REQ-F-083: [Event-driven] When the user presses Enter on the transaction entry screen, the system shall validate the account identifier or card number, validate all transaction data fields, and if all validations pass, prompt the user for confirmation.

REQ-F-084: [Event-driven] When the user presses PF4 on the transaction entry screen, the system shall clear all input fields and present the cleared screen for new entry.

REQ-F-085: [Event-driven] When the user presses PF5 on the transaction entry screen and the account identifier or card number passes validation, the system shall retrieve the most recent transaction record from the transaction keyed dataset (legacy: CARDDEMO.TRANSACT.VSAM.KSDS) by browsing backward from the highest transaction ID and populate all transaction data fields with the retrieved values.

REQ-F-086: [Unwanted] If the user presses PF5 and no previous transaction is found or a retrieval error occurs, the system shall display an appropriate error message.

REQ-F-087: [Event-driven] When the user presses any key other than Enter, PF3, PF4, or PF5, the system shall display an invalid key message.

REQ-F-088: [Ubiquitous] The system shall validate that the account identifier or card number field is numeric and that the referenced account or card exists in the card cross-reference file (legacy: CARDDEMO.CARDXREF.VSAM.KSDS); if account identifier is provided, the system shall retrieve the associated card number, and if card number is provided, the system shall retrieve the associated account identifier.

REQ-F-089: [Unwanted] If neither account identifier nor card number is provided, the system shall reject the entry with an error message.

REQ-F-090: [Ubiquitous] The system shall validate that transaction type code and category code are numeric, that the transaction amount conforms to the format ±99999999.99, that the original date and processing date conform to the format YYYY-MM-DD, and that the merchant identifier is numeric.

REQ-F-091: [Ubiquitous] The system shall validate that all required transaction data fields are non-empty: type code, category code, source, description, amount, original date, processing date, merchant identifier, merchant name, merchant city, and merchant zip code.

REQ-F-092: [Event-driven] When the original date and processing date pass format validation, the system shall invoke the external date validation service for each date using the YYYY-MM-DD format and reject the entry with a date-specific error message if the service returns a failure code other than 2513.

REQ-F-093: [Ubiquitous] The system shall convert the transaction amount string to a numeric value and reformat it to the standardized display format ±99999999.99 for consistent presentation.

REQ-F-094: [Event-driven] When all transaction data validations pass and the user submits the screen, the system shall display a confirmation prompt requiring the user to enter 'Y' or 'y' to proceed, or 'N', 'n', space, or low-value to return to the data entry screen.

REQ-F-095: [Unwanted] If the user enters any value other than 'Y', 'y', 'N', 'n', space, or low-value at the confirmation prompt, the system shall display an invalid value error message and re-prompt for confirmation.

REQ-F-096: [Event-driven] When the user confirms the transaction entry by entering 'Y' or 'y', the system shall generate the next transaction identifier by browsing the transaction keyed dataset backward from the highest existing identifier and incrementing by 1.

REQ-F-097: [Event-driven] When the user confirms the transaction entry by entering 'Y' or 'y', the system shall assemble the complete transaction record with the generated transaction identifier, type code, category code, source, description, amount, card number, merchant identifier, merchant name, merchant city, merchant zip code, and timestamps, and write it to the transaction keyed dataset using the transaction identifier as the key.

REQ-F-098: [Event-driven] When the transaction record is successfully written, the system shall display a success message including the newly assigned transaction identifier and clear all input fields.

REQ-F-099: [Unwanted] If the transaction record write fails due to a duplicate key, the system shall display a duplicate transaction identifier error message.

REQ-F-100: [Unwanted] If the transaction record write fails for any reason other than a duplicate key, the system shall display a generic unable-to-add error message.

REQ-F-101: [Unwanted] If the browse or read of transaction records fails to find a starting point, the system shall display a transaction-not-found error message.

REQ-F-102: [Unwanted] If the cross-reference lookup for account or card data fails because the record is not found, the system shall display a not-found error message; for any other lookup error, the system shall display a generic unable-to-lookup error message.

REQ-F-103: [Ubiquitous] The system shall display the transaction entry screen with the application title, product name, current date in MM/DD/YY format, current time in HH:MM:SS format, and any applicable error or status message.

REQ-F-104: [Event-driven] *When arriving from a prior function with a pre-selected card number in the session context,* the system shall pre-populate the card number field on the transaction entry screen.

### Non-Functional Requirements

REQ-N-003: [Unwanted] If the transaction identifier generation encounters an end-of-data condition during backward browse (indicating no existing transactions), the system shall initialize the transaction identifier to zero and increment from that baseline.

### Open Questions

OQ-6: The date validation service returns code 2513 which is explicitly excluded from triggering an error. What does code 2513 represent, and should it be treated as a valid-date indicator or a specific warning condition? — Owner: domain expert / date validation service team

OQ-7: The transaction identifier is generated by incrementing the highest existing ID by 1. In a concurrent multi-user environment, what mechanism ensures uniqueness if two users attempt to add transactions simultaneously? — Owner: architecture team

---

## 6. Transaction Consolidation and Repository Loading


As a batch operations team, I want transaction records from backup and system sources consolidated into a single ordered repository so that all transactions are available for efficient indexed lookup by downstream online and batch processes.

### Requirements

REQ-F-105: [Event-driven] When the transaction consolidation job executes, the system shall read all transaction records from the transaction backup file (legacy: AWS.M2.CARDDEMO.TRANSACT.BKUP) and the system transaction file (legacy: AWS.M2.CARDDEMO.SYSTRAN), merge them, sort the combined set in ascending order by transaction ID, and produce a new version of the transaction combined file.

REQ-F-106: [Ubiquitous] The system shall ensure the consolidated output contains every transaction record from both the transaction backup file and the system transaction file without omission or duplication introduced by the consolidation process itself.

REQ-F-107: [Event-driven] When the consolidated transaction combined file is ready, the system shall load all consolidated transaction records into the transaction keyed dataset (legacy: AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS), indexed by transaction ID, enabling efficient retrieval of individual transactions by their identifier.

REQ-F-108: [Ubiquitous] The transaction keyed dataset shall preserve the ascending transaction ID order established during consolidation and support both sequential and keyed access patterns.

### Non-Functional Requirements

REQ-N-004: [Unwanted] If the consolidation or repository load is interrupted before completion, the system shall not leave the transaction keyed dataset in a partially updated state that could be mistaken for a successful load.

### Open Questions

OQ-8: If duplicate transaction IDs exist across the backup and system sources, should both records be retained, or should one source take precedence? — Owner: transaction operations domain expert

---

## 7. Daily Rejection File Version Management


As a batch operations team, I want the system to maintain a rolling history of daily rejected transactions so that operations staff can review recent rejection patterns without manual file cleanup.

### Requirements

REQ-F-109: [Ubiquitous] The system shall maintain a versioned history of the daily rejections file (legacy: AWS.M2.CARDDEMO.DALYREJS), retaining a maximum of 5 versions.

REQ-F-110: [Event-driven] When a new version of the daily rejections file is created and the number of retained versions exceeds 5, the system shall automatically delete the oldest version.

### Open Questions

OQ-9: What downstream processes consume the daily rejections file, and do they require access to all 5 retained versions or only the most recent? — Owner: batch operations team

OQ-10: Is the 5-version retention limit a business requirement (e.g., aligned with a 5-business-day review cycle) or an arbitrary storage constraint that could be adjusted in the modernized system? — Owner: domain expert / operations

---

## 8. Transaction and Balance Data Version Retention


As a batch operations team, I want critical transaction and balance data stores to maintain a rolling history of up to 5 versions so that recent data snapshots are available for recovery and audit purposes while preventing unbounded storage growth.

**Category:** setup
**Purpose:** Establishes versioned data store definitions with automatic retention management for critical transaction and balance files used by downstream batch processing.
**Migration relevance:** Defines the retention policy and versioning structure that downstream jobs depend on for backup, reporting, and history preservation.

### Requirements

REQ-F-111: [Ubiquitous] The system shall maintain a versioned data store for the transaction backup file (legacy: AWS.M2.CARDDEMO.TRANSACT.BKUP) that retains up to 5 versions, automatically removing the oldest version when the limit is exceeded.

REQ-F-112: [Ubiquitous] The system shall maintain a versioned data store for the transaction catalog backup (legacy: AWS.M2.CARDDEMO.TCATBALF.BKUP) that retains up to 5 versions, automatically removing the oldest version when the limit is exceeded.

REQ-F-113: [Ubiquitous] The system shall maintain a versioned data store for the transaction combined file (legacy: AWS.M2.CARDDEMO.TRANSACT.COMBINED) that retains up to 5 versions, automatically removing the oldest version when the limit is exceeded.

REQ-F-114: [Ubiquitous] The system shall maintain a versioned data store for the transaction report file (legacy: AWS.M2.CARDDEMO.TRANREPT) that retains up to 5 versions, automatically removing the oldest version when the limit is exceeded.

REQ-F-115: [Ubiquitous] The system shall maintain a versioned data store for the transaction daily file (legacy: AWS.M2.CARDDEMO.TRANSACT.DALY) that retains up to 5 versions, automatically removing the oldest version when the limit is exceeded.

REQ-F-116: [Ubiquitous] The system shall maintain a versioned data store for the system transaction file (legacy: AWS.M2.CARDDEMO.SYSTRAN) that retains up to 5 versions, automatically removing the oldest version when the limit is exceeded.

---

## 9. Daily Transaction Posting


As a batch operations team, I want daily credit card transactions validated against card and account information and posted to transaction history with updated category balances, so that account balances reflect the day's activity and invalid transactions are captured for review.

**Restart/Recovery:** The posting process updates account records and transaction category balances in place; if interrupted, partial updates may exist with no automatic rollback.

### Requirements

**Transaction Processing Loop**

REQ-F-117: [State-driven] While unprocessed records remain in the daily transaction file (legacy: AWS.M2.CARDDEMO.DALYTRAN.PS), the system shall read each transaction record sequentially, validate it, and if validation passes, post it; processing shall terminate when all records have been read.

**Card Validation**

REQ-F-118: [Event-driven] When a transaction record is read for validation, the system shall reset the validation failure state to indicate no errors before performing any checks.

REQ-F-119: [Event-driven] When a transaction is submitted for validation, the system shall look up the transaction's card number in the card cross-reference data store (legacy: AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS) to retrieve the associated account identifier; if the card number is not found, the system shall record validation failure reason 100 with description 'INVALID CARD NUMBER FOUND'.

**Account Validation**

REQ-F-120: [Event-driven] When the card number is resolved to an account identifier, the system shall retrieve the account record from the account keyed dataset (legacy: AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS); if the account record is not found, the system shall record validation failure reason 101 with description 'ACCOUNT RECORD NOT FOUND'.

**Credit Limit Validation**

REQ-F-121: [Complex] While the account record is found, when the transaction amount is evaluated, the system shall calculate the projected balance as (account current cycle credit − account current cycle debit + transaction amount); if the projected balance exceeds the account credit limit, the system shall record validation failure reason 102 with description 'OVERLIMIT TRANSACTION'.

**Expiration Date Validation**

REQ-F-122: [Complex] While the account record is found and the credit limit check passes, when the transaction date (first 10 characters of the transaction origination timestamp) is compared to the account expiration date, the system shall reject the transaction with validation failure reason 103 and description 'TRANSACTION RECEIVED AFTER ACCT EXPIRATION' if the transaction date is after the account expiration date.

**Validation Short-Circuit**

REQ-F-123: [Ubiquitous] The system shall stop validation at the first failure encountered in the sequence: card lookup → account lookup → credit limit check → expiration date check; no further checks shall be performed after a failure is detected.

**Rejection Handling**

REQ-F-124: [Event-driven] When transaction validation completes with a non-zero failure reason, the system shall write the original transaction data together with a validation trailer containing the failure reason code and failure description to the daily rejections file (legacy: AWS.M2.CARDDEMO.DALYREJS).

**Posting — Transaction History**

REQ-F-125: [Event-driven] When a transaction passes all validation checks, the system shall copy the transaction ID, type code, category code, source, description, amount, merchant ID, merchant name, merchant city, merchant ZIP code, card number, and original timestamp from the daily transaction record to the transaction keyed dataset (legacy: AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS), and stamp the record with a processing timestamp formatted as YYYY-MM-DD HH.MM.SS.MIL0000 using the current system date and time.

**Posting — Account Balance Update**

REQ-F-126: [Event-driven] When a transaction passes all validation checks, the system shall add the transaction amount to the account's current balance; if the transaction amount is non-negative, add it to the account's current cycle credits; if negative, add it to the account's current cycle debits; and persist the updated account record to the account keyed dataset.

**Posting — Transaction Category Balance Update**

REQ-F-127: [Event-driven] When a transaction passes all validation checks, the system shall look up the transaction category balance record in the transaction catalog keyed data store (legacy: AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS) using the composite key of account identifier, transaction type code, and transaction category code.

REQ-F-128: [Event-driven] When the transaction category balance record exists, the system shall add the transaction amount to the existing category balance and persist the updated record.

REQ-F-129: [Event-driven] When the transaction category balance record does not exist for the given key, the system shall create a new record with the account identifier, transaction type code, transaction category code, and an initial balance equal to the transaction amount, and write it to the transaction catalog keyed data store.

**Conditional Posting Gate**

REQ-F-130: [Event-driven] When transaction validation completes with a failure reason of zero, the system shall proceed to post the transaction; when the failure reason is non-zero, the system shall skip posting and proceed to the next transaction.

### Non-Functional Requirements

REQ-N-005: [Unwanted] If the posting process writes to the account keyed dataset, the daily rejections file, the transaction catalog keyed data store, and the transaction keyed dataset for a single transaction, the system shall ensure that all writes for that transaction either succeed completely or fail completely, preventing partial updates across data stores.

REQ-N-006: [Unwanted] If an unexpected read error occurs on the daily transaction file (other than end-of-file), the system shall terminate processing with an error condition.

### Open Questions

OQ-11: The validation rules check credit limit using projected balance (current cycle credit − current cycle debit + transaction amount). It is unclear whether the "current balance" field is also considered in this calculation or whether cycle credits/debits alone determine the limit. — Owner: Credit Risk / Product team

OQ-12: Rule group 5 describes posting the transaction amount to the account's "current balance" in addition to cycle credits/debits, while the credit limit check only uses cycle credits/debits. Confirm whether the current balance update is an independent running total or whether it should factor into the overlimit check. — Owner: Finance / Accounting team

OQ-13: When a new transaction category balance record is created (REQ-F-129), confirm whether any additional initialization fields (e.g., opening date, status) are required beyond the key fields and initial balance. — Owner: Product team

---

## 10. Daily Transaction Extraction by Processing Date


As a batch operations team, I want credit card transactions extracted from the backup transaction file for a specific processing date and sorted by card number so that downstream reporting and reconciliation processes receive a consistently ordered daily transaction dataset.

### Requirements

REQ-F-131: [Event-driven] When the daily transaction extraction job executes, the system shall read the transaction backup file (legacy: AWS.M2.CARDDEMO.TRANSACT.BKUP) and select only those transaction records whose processing date matches the configured extraction date (June 2, 2022).

REQ-F-132: [Ubiquitous] The system shall sort the selected transaction records in ascending order by card number to produce the daily transaction dataset (legacy: AWS.M2.CARDDEMO.TRANSACT.DALY).

REQ-F-133: [Ubiquitous] The system shall produce a new version of the daily transaction dataset containing only the filtered and sorted records for the configured processing date.

### Open Questions

OQ-14: The extraction date is hard-coded to June 2, 2022. Should the modernized system accept the processing date as a runtime parameter (e.g., from the date parameter file or scheduling system) rather than a fixed value? — Owner: batch operations team

OQ-15: No explicit rules were provided for this job. Are there additional filtering criteria beyond processing date (e.g., transaction status, transaction type) that should be applied during extraction? — Owner: transaction processing domain expert

---

## 11. Transaction Category Balance Data Store Initialization


As a batch operations team, I want the transaction category balance data store rebuilt from a sequential source so that the system maintains an indexed, keyed data store of transaction category balances available for efficient retrieval during transaction processing.

**Category:** setup
**Purpose:** Establishes the transaction category balance data store by replacing any existing version with a freshly loaded copy from the sequential source file, ensuring a clean and consistent indexed data store for keyed access.
**Migration relevance:** Defines the structure and initial/refreshed state of the transaction category balance data store. The data exchange pattern (source format, key structure, record layout) must be preserved; the storage mechanism is implementation-specific.

### Requirements

REQ-F-134: [Event-driven] When the transaction category balance initialization process executes, the system shall replace the existing transaction catalog keyed data store (legacy: AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS) with a new version loaded from the transaction catalog file (legacy: AWS.M2.CARDDEMO.TCATBALF.PS), making all transaction category balance records accessible by key.

REQ-F-135: [Ubiquitous] The transaction category balance data store shall use a 17-byte key starting at position 0 of each record, with a fixed record size of 50 bytes, to support keyed retrieval of transaction category balance records.

REQ-F-136: [Ubiquitous] The system shall load all transaction category balance records from the sequential source into the indexed data store, preserving the keyed organization so that records are retrievable by their 17-byte key.

### Non-Functional Requirements

REQ-N-007: [Unwanted] If the existing transaction category balance data store cannot be removed prior to recreation, the system shall halt the initialization process and report the failure rather than risk data corruption from a partial rebuild.

REQ-N-008: [Ubiquitous] The transaction category balance data store shall support concurrent access by multiple readers and writers.

### Open Questions

OQ-16: Is this job intended as a one-time setup/migration activity, or is it executed periodically (e.g., daily) to refresh the transaction category balance data store from an updated sequential source? — Owner: batch operations / business analyst

OQ-17: What is the business meaning of the 17-byte key (e.g., does it represent a composite of transaction type code and category code, or another combination)? — Owner: domain expert / data architect

---

## 12. Transaction Data Store Backup and Rebuild


As a batch operations team, I want the transaction data store backed up and rebuilt on a scheduled basis so that a point-in-time recovery snapshot exists and the data store structure is optimized for ongoing transaction processing.

### Requirements

REQ-F-137: [Event-driven] When the transaction backup process is initiated, the system shall copy all records from the transaction keyed dataset (legacy: AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS) to a new version of the transaction backup file (legacy: AWS.M2.CARDDEMO.TRANSACT.BKUP), creating a point-in-time snapshot that is retained for recovery purposes.

REQ-F-138: [Event-driven] When the backup copy completes successfully, the system shall remove the existing transaction keyed dataset and its alternate index structure, clearing all prior data and index paths.

REQ-F-139: [Event-driven] When the existing transaction data store and alternate index have been removed, the system shall recreate the transaction keyed dataset as an empty indexed data store with a 16-byte key and 350-byte fixed record size, configured for concurrent read access and exclusive write access.

### Non-Functional Requirements

REQ-N-009: [Unwanted] If the removal of the existing transaction data store fails, the system shall not attempt to recreate the data store, preventing a partial rebuild that could result in data loss.

REQ-N-010: [Ubiquitous] The backup process shall preserve the source transaction data unchanged and available for concurrent access during the copy operation.

### Open Questions

OQ-18: After the rebuild, the transaction keyed dataset is empty. Is there a subsequent step or job that reloads the backed-up data into the fresh data store, or does the empty state represent the intended post-backup condition (e.g., start of a new processing cycle)? — Owner: batch operations / business domain expert

---

## 13. Transaction Data Store Initialization and Alternate Index Configuration


As a batch operations team, I want the transaction data store initialized with proper structure, populated with source data, and configured with alternate index access paths so that applications can efficiently retrieve transaction records by both primary key and alternate key (e.g., card number or date).

**Restart/Recovery:** This job recreates the transaction data store from scratch on each execution; it is inherently idempotent.

### Requirements

REQ-F-140: [Event-driven] When the transaction data store initialization process begins, the system shall close the transaction file and the transaction alternate index file in the online processing region to prevent concurrent access during batch processing.

REQ-F-141: [Event-driven] When the online transaction files are successfully closed, the system shall remove any existing transaction data store instance (including all associated index components), proceeding without error if no prior instance exists.

REQ-F-142: [Event-driven] When the prior transaction data store is removed, the system shall create a new indexed transaction data store (legacy: AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS) with a 16-byte primary key, 350-byte fixed-length records, and concurrent read/write access enabled.

REQ-F-143: [Event-driven] When the indexed transaction data store is successfully created, the system shall populate it with all transaction records from the transaction sequential file (legacy: AWS.M2.CARDDEMO.TRANSACT.DALY), maintaining primary key order.

REQ-F-144: [Event-driven] When the transaction data store is populated, the system shall remove any existing alternate index on the transaction data store to ensure a clean state before rebuild.

REQ-F-145: [Event-driven] When the previous alternate index deletion completes, the system shall create a new alternate index on the transaction data store (legacy: AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX) with a composite key spanning bytes 26–304 of each transaction record, configured as non-unique and upgradeable.

REQ-F-146: [Event-driven] When the alternate index is defined, the system shall build the alternate index from the populated transaction data store, enabling efficient lookups by the alternate key.

REQ-F-147: [Event-driven] When the alternate index is built, the system shall register the alternate index path (legacy: AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX.PATH), enabling applications to retrieve transaction records by the alternate key without requiring a full sequential scan of the primary transaction data store.

REQ-F-148: [Event-driven] When the alternate index path is registered, the system shall reopen the transaction file and the transaction alternate index file in the online processing region to restore normal application access.

### Non-Functional Requirements

REQ-N-011: [Unwanted] If the online transaction files cannot be closed before batch processing begins, the system shall halt the initialization process to prevent data corruption from concurrent access.

REQ-N-012: [Ubiquitous] The transaction data store initialization process shall be idempotent — re-execution shall produce the same result regardless of prior state.

### Open Questions

OQ-19: The alternate index composite key spans bytes 26–304 (279 bytes). What business fields does this key represent (e.g., card number + transaction date)? Confirmation is needed to document the alternate lookup semantics. — Owner: data architecture team

OQ-20: The non-unique alternate index implies multiple transactions may share the same alternate key value. Is there a business constraint on the maximum number of records per alternate key value? — Owner: transaction processing domain expert

---

## 14. Transaction Secondary Index Access


As a transaction processing system, I want transaction records to be accessible by a secondary key (transaction identifier and posting date) so that batch and online processes can efficiently retrieve and query transactions without requiring full sequential scans of the transaction data store.

**Category:** setup
**Purpose:** Establishes a secondary access path on the transaction keyed dataset to support efficient lookups by transaction identifier and posting date.
**Migration relevance:** Defines a secondary access pattern that downstream batch and online processes depend on for transaction retrieval and reporting.

### Requirements

REQ-F-149: [Ubiquitous] The system shall maintain a secondary index on the transaction keyed dataset (legacy: AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS) keyed by transaction identifier and posting date, allowing non-unique key values so that multiple transaction records may share the same key combination.

REQ-F-150: [Ubiquitous] The system shall automatically synchronize the secondary index whenever the underlying transaction keyed dataset is modified, ensuring that queries via the secondary key always reflect the current state of the transaction data.

REQ-F-151: [Ubiquitous] The system shall provide a named access path (transaction alternate path) that enables batch and online processes to retrieve transaction records using the secondary key (transaction identifier and posting date) rather than the primary key.

### Open Questions

OQ-21: The legacy alternate index key is described as a 26-byte field at position 304 (posting date) and also references transaction identifier at byte 26. What is the exact composite key structure (field order, lengths) for the secondary index? — Owner: data architecture team

OQ-22: Are there downstream processes that depend on the non-unique key behavior (multiple records per key value), and should the modernized system enforce any ordering among records sharing the same secondary key? — Owner: transaction processing domain expert

---

## 15. Job Dependencies

The following dependencies are inferred from shared data store access:

- **COMBTRAN.jcl** (Section 6: Transaction Consolidation and Repository Loading) → **DALYREJS.jcl** (Section 7: Daily Rejection File Version Management) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.COMBINED`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **COMBTRAN.jcl** (Section 6: Transaction Consolidation and Repository Loading) → **DEFGDGB.jcl** (Section 8: Transaction and Balance Data Version Retention) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.COMBINED`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **COMBTRAN.jcl** (Section 6: Transaction Consolidation and Repository Loading) → **POSTTRAN.jcl** (Section 9: Daily Transaction Posting) (via `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`)
- **COMBTRAN.jcl** (Section 6: Transaction Consolidation and Repository Loading) → **SORTTEST.jcl** (Section 10: Daily Transaction Extraction by Processing Date) (via `AWS.M2.CARDDEMO.TCATBALF.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`)
- **COMBTRAN.jcl** (Section 6: Transaction Consolidation and Repository Loading) → **TCATBALF.jcl** (Section 11: Transaction Category Balance Data Store Initialization) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.COMBINED`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **COMBTRAN.jcl** (Section 6: Transaction Consolidation and Repository Loading) → **TRANBKP.jcl** (Section 12: Transaction Data Store Backup and Rebuild) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.COMBINED`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **COMBTRAN.jcl** (Section 6: Transaction Consolidation and Repository Loading) → **TRANFILE.jcl** (Section 13: Transaction Data Store Initialization and Alternate Index Configuration) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.COMBINED`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **COMBTRAN.jcl** (Section 6: Transaction Consolidation and Repository Loading) → **TRANIDX.jcl** (Section 14: Transaction Secondary Index Access) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.COMBINED`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **DALYREJS.jcl** (Section 7: Daily Rejection File Version Management) → **COMBTRAN.jcl** (Section 6: Transaction Consolidation and Repository Loading) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.BKUP`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **DALYREJS.jcl** (Section 7: Daily Rejection File Version Management) → **DEFGDGB.jcl** (Section 8: Transaction and Balance Data Version Retention) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **DALYREJS.jcl** (Section 7: Daily Rejection File Version Management) → **POSTTRAN.jcl** (Section 9: Daily Transaction Posting) (via `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`)
- **DALYREJS.jcl** (Section 7: Daily Rejection File Version Management) → **SORTTEST.jcl** (Section 10: Daily Transaction Extraction by Processing Date) (via `AWS.M2.CARDDEMO.TCATBALF.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`)
- **DALYREJS.jcl** (Section 7: Daily Rejection File Version Management) → **TCATBALF.jcl** (Section 11: Transaction Category Balance Data Store Initialization) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **DALYREJS.jcl** (Section 7: Daily Rejection File Version Management) → **TRANBKP.jcl** (Section 12: Transaction Data Store Backup and Rebuild) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **DALYREJS.jcl** (Section 7: Daily Rejection File Version Management) → **TRANFILE.jcl** (Section 13: Transaction Data Store Initialization and Alternate Index Configuration) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **DALYREJS.jcl** (Section 7: Daily Rejection File Version Management) → **TRANIDX.jcl** (Section 14: Transaction Secondary Index Access) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **DEFGDGB.jcl** (Section 8: Transaction and Balance Data Version Retention) → **COMBTRAN.jcl** (Section 6: Transaction Consolidation and Repository Loading) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.BKUP`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **DEFGDGB.jcl** (Section 8: Transaction and Balance Data Version Retention) → **DALYREJS.jcl** (Section 7: Daily Rejection File Version Management) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **DEFGDGB.jcl** (Section 8: Transaction and Balance Data Version Retention) → **POSTTRAN.jcl** (Section 9: Daily Transaction Posting) (via `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`)
- **DEFGDGB.jcl** (Section 8: Transaction and Balance Data Version Retention) → **SORTTEST.jcl** (Section 10: Daily Transaction Extraction by Processing Date) (via `AWS.M2.CARDDEMO.TCATBALF.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`)
- **DEFGDGB.jcl** (Section 8: Transaction and Balance Data Version Retention) → **TCATBALF.jcl** (Section 11: Transaction Category Balance Data Store Initialization) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **DEFGDGB.jcl** (Section 8: Transaction and Balance Data Version Retention) → **TRANBKP.jcl** (Section 12: Transaction Data Store Backup and Rebuild) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **DEFGDGB.jcl** (Section 8: Transaction and Balance Data Version Retention) → **TRANFILE.jcl** (Section 13: Transaction Data Store Initialization and Alternate Index Configuration) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **DEFGDGB.jcl** (Section 8: Transaction and Balance Data Version Retention) → **TRANIDX.jcl** (Section 14: Transaction Secondary Index Access) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **POSTTRAN.jcl** (Section 9: Daily Transaction Posting) → **COMBTRAN.jcl** (Section 6: Transaction Consolidation and Repository Loading) (via `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`)
- **POSTTRAN.jcl** (Section 9: Daily Transaction Posting) → **DALYREJS.jcl** (Section 7: Daily Rejection File Version Management) (via `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`)
- **POSTTRAN.jcl** (Section 9: Daily Transaction Posting) → **DEFGDGB.jcl** (Section 8: Transaction and Balance Data Version Retention) (via `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`)
- **POSTTRAN.jcl** (Section 9: Daily Transaction Posting) → **SORTTEST.jcl** (Section 10: Daily Transaction Extraction by Processing Date) (via `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`)
- **POSTTRAN.jcl** (Section 9: Daily Transaction Posting) → **TCATBALF.jcl** (Section 11: Transaction Category Balance Data Store Initialization) (via `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`)
- **POSTTRAN.jcl** (Section 9: Daily Transaction Posting) → **TRANBKP.jcl** (Section 12: Transaction Data Store Backup and Rebuild) (via `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`)
- **POSTTRAN.jcl** (Section 9: Daily Transaction Posting) → **TRANFILE.jcl** (Section 13: Transaction Data Store Initialization and Alternate Index Configuration) (via `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`)
- **POSTTRAN.jcl** (Section 9: Daily Transaction Posting) → **TRANIDX.jcl** (Section 14: Transaction Secondary Index Access) (via `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`)
- **SORTTEST.jcl** (Section 10: Daily Transaction Extraction by Processing Date) → **COMBTRAN.jcl** (Section 6: Transaction Consolidation and Repository Loading) (via `AWS.M2.CARDDEMO.TRANSACT.COMBINED`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **SORTTEST.jcl** (Section 10: Daily Transaction Extraction by Processing Date) → **DALYREJS.jcl** (Section 7: Daily Rejection File Version Management) (via `AWS.M2.CARDDEMO.TRANSACT.COMBINED`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **SORTTEST.jcl** (Section 10: Daily Transaction Extraction by Processing Date) → **DEFGDGB.jcl** (Section 8: Transaction and Balance Data Version Retention) (via `AWS.M2.CARDDEMO.TRANSACT.COMBINED`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **SORTTEST.jcl** (Section 10: Daily Transaction Extraction by Processing Date) → **TCATBALF.jcl** (Section 11: Transaction Category Balance Data Store Initialization) (via `AWS.M2.CARDDEMO.TRANSACT.COMBINED`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **SORTTEST.jcl** (Section 10: Daily Transaction Extraction by Processing Date) → **TRANBKP.jcl** (Section 12: Transaction Data Store Backup and Rebuild) (via `AWS.M2.CARDDEMO.TRANSACT.COMBINED`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **SORTTEST.jcl** (Section 10: Daily Transaction Extraction by Processing Date) → **TRANFILE.jcl** (Section 13: Transaction Data Store Initialization and Alternate Index Configuration) (via `AWS.M2.CARDDEMO.TRANSACT.COMBINED`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **SORTTEST.jcl** (Section 10: Daily Transaction Extraction by Processing Date) → **TRANIDX.jcl** (Section 14: Transaction Secondary Index Access) (via `AWS.M2.CARDDEMO.TRANSACT.COMBINED`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TCATBALF.jcl** (Section 11: Transaction Category Balance Data Store Initialization) → **COMBTRAN.jcl** (Section 6: Transaction Consolidation and Repository Loading) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.BKUP`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TCATBALF.jcl** (Section 11: Transaction Category Balance Data Store Initialization) → **DALYREJS.jcl** (Section 7: Daily Rejection File Version Management) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TCATBALF.jcl** (Section 11: Transaction Category Balance Data Store Initialization) → **DEFGDGB.jcl** (Section 8: Transaction and Balance Data Version Retention) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TCATBALF.jcl** (Section 11: Transaction Category Balance Data Store Initialization) → **POSTTRAN.jcl** (Section 9: Daily Transaction Posting) (via `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`)
- **TCATBALF.jcl** (Section 11: Transaction Category Balance Data Store Initialization) → **SORTTEST.jcl** (Section 10: Daily Transaction Extraction by Processing Date) (via `AWS.M2.CARDDEMO.TCATBALF.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`)
- **TCATBALF.jcl** (Section 11: Transaction Category Balance Data Store Initialization) → **TRANBKP.jcl** (Section 12: Transaction Data Store Backup and Rebuild) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TCATBALF.jcl** (Section 11: Transaction Category Balance Data Store Initialization) → **TRANFILE.jcl** (Section 13: Transaction Data Store Initialization and Alternate Index Configuration) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TCATBALF.jcl** (Section 11: Transaction Category Balance Data Store Initialization) → **TRANIDX.jcl** (Section 14: Transaction Secondary Index Access) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANBKP.jcl** (Section 12: Transaction Data Store Backup and Rebuild) → **COMBTRAN.jcl** (Section 6: Transaction Consolidation and Repository Loading) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.BKUP`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANBKP.jcl** (Section 12: Transaction Data Store Backup and Rebuild) → **DALYREJS.jcl** (Section 7: Daily Rejection File Version Management) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANBKP.jcl** (Section 12: Transaction Data Store Backup and Rebuild) → **DEFGDGB.jcl** (Section 8: Transaction and Balance Data Version Retention) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANBKP.jcl** (Section 12: Transaction Data Store Backup and Rebuild) → **POSTTRAN.jcl** (Section 9: Daily Transaction Posting) (via `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`)
- **TRANBKP.jcl** (Section 12: Transaction Data Store Backup and Rebuild) → **SORTTEST.jcl** (Section 10: Daily Transaction Extraction by Processing Date) (via `AWS.M2.CARDDEMO.TCATBALF.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`)
- **TRANBKP.jcl** (Section 12: Transaction Data Store Backup and Rebuild) → **TCATBALF.jcl** (Section 11: Transaction Category Balance Data Store Initialization) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANBKP.jcl** (Section 12: Transaction Data Store Backup and Rebuild) → **TRANFILE.jcl** (Section 13: Transaction Data Store Initialization and Alternate Index Configuration) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANBKP.jcl** (Section 12: Transaction Data Store Backup and Rebuild) → **TRANIDX.jcl** (Section 14: Transaction Secondary Index Access) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANFILE.jcl** (Section 13: Transaction Data Store Initialization and Alternate Index Configuration) → **COMBTRAN.jcl** (Section 6: Transaction Consolidation and Repository Loading) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.BKUP`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANFILE.jcl** (Section 13: Transaction Data Store Initialization and Alternate Index Configuration) → **DALYREJS.jcl** (Section 7: Daily Rejection File Version Management) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANFILE.jcl** (Section 13: Transaction Data Store Initialization and Alternate Index Configuration) → **DEFGDGB.jcl** (Section 8: Transaction and Balance Data Version Retention) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANFILE.jcl** (Section 13: Transaction Data Store Initialization and Alternate Index Configuration) → **POSTTRAN.jcl** (Section 9: Daily Transaction Posting) (via `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`)
- **TRANFILE.jcl** (Section 13: Transaction Data Store Initialization and Alternate Index Configuration) → **SORTTEST.jcl** (Section 10: Daily Transaction Extraction by Processing Date) (via `AWS.M2.CARDDEMO.TCATBALF.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`)
- **TRANFILE.jcl** (Section 13: Transaction Data Store Initialization and Alternate Index Configuration) → **TCATBALF.jcl** (Section 11: Transaction Category Balance Data Store Initialization) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANFILE.jcl** (Section 13: Transaction Data Store Initialization and Alternate Index Configuration) → **TRANBKP.jcl** (Section 12: Transaction Data Store Backup and Rebuild) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANFILE.jcl** (Section 13: Transaction Data Store Initialization and Alternate Index Configuration) → **TRANIDX.jcl** (Section 14: Transaction Secondary Index Access) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANIDX.jcl** (Section 14: Transaction Secondary Index Access) → **COMBTRAN.jcl** (Section 6: Transaction Consolidation and Repository Loading) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.BKUP`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANIDX.jcl** (Section 14: Transaction Secondary Index Access) → **DALYREJS.jcl** (Section 7: Daily Rejection File Version Management) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANIDX.jcl** (Section 14: Transaction Secondary Index Access) → **DEFGDGB.jcl** (Section 8: Transaction and Balance Data Version Retention) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANIDX.jcl** (Section 14: Transaction Secondary Index Access) → **POSTTRAN.jcl** (Section 9: Daily Transaction Posting) (via `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`)
- **TRANIDX.jcl** (Section 14: Transaction Secondary Index Access) → **SORTTEST.jcl** (Section 10: Daily Transaction Extraction by Processing Date) (via `AWS.M2.CARDDEMO.TCATBALF.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`)
- **TRANIDX.jcl** (Section 14: Transaction Secondary Index Access) → **TCATBALF.jcl** (Section 11: Transaction Category Balance Data Store Initialization) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANIDX.jcl** (Section 14: Transaction Secondary Index Access) → **TRANBKP.jcl** (Section 12: Transaction Data Store Backup and Rebuild) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANIDX.jcl** (Section 14: Transaction Secondary Index Access) → **TRANFILE.jcl** (Section 13: Transaction Data Store Initialization and Alternate Index Configuration) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)

*These dependencies represent data flow ordering constraints. The implementation should ensure predecessor jobs complete before successors execute.*

---



## Shared Capability Dependencies

This capability depends on the following shared capabilities.
Do not reimplement their behavior — integrate with the shared service.

- **COSGN00C** (`_shared/COSGN00C/`) — As a user of the CardDemo application, I want to sign on with my credentials so that I am authenticated and directed to the appropriate application functions based on my role.
- **CSUTLDTC** (`_shared/CSUTLDTC/`) — As any system component requiring date validation, I want to submit a date string with its format specification and receive a structured validation result so that I can determine whether the date is valid and, if not, understand the specific reason for failure.


---

## Security Hardening Requirements

**Based on Security Scan Findings:** #2, #13, #14, #23

### Transaction Creation Authorization

REQ-SEC-001: [Event-driven] When a user attempts to create a new transaction, the system shall verify that the specified card number belongs to an account associated with the authenticated user before allowing transaction creation.

REQ-SEC-002: [Event-driven] When verifying card ownership for transaction creation, the system shall query the card-to-account-to-customer-to-user relationship and reject the transaction if the card does not belong to the authenticated user.

REQ-SEC-003: [Unwanted] If a user attempts to create a transaction on a card they do not own, the system shall reject the request with a generic "card not found" error message rather than revealing the card exists but is unauthorized.

REQ-SEC-004: [Ubiquitous] Administrator users with explicit transaction management privileges may create transactions on behalf of customers, but such operations shall be logged with the administrator ID and a flag indicating administrative override.

### Transaction Viewing Authorization

REQ-SEC-005: [Event-driven] When a user requests a list of transactions, the system shall filter the results to include only transactions associated with cards belonging to accounts owned by the authenticated user.

REQ-SEC-006: [Event-driven] When a user requests transaction detail for a specific transaction ID, the system shall verify the transaction belongs to a card owned by the authenticated user before displaying transaction details.

REQ-SEC-007: [Unwanted] If a user attempts to view a transaction that does not belong to them, the system shall return a generic "transaction not found" error rather than revealing the transaction exists but is unauthorized.

REQ-SEC-008: [Ubiquitous] The system shall never return transactions from other users' accounts in list views, search results, or pagination, regardless of filter parameters or query manipulation attempts.

### Transaction Amount Validation

REQ-SEC-009: [Unwanted] If a transaction amount is less than or equal to zero, the system shall reject the transaction and display an error message indicating the amount must be positive.

REQ-SEC-010: [Unwanted] If a transaction amount is not a valid numeric value (including rejection of NaN, Infinity, -Infinity, or non-numeric strings), the system shall reject the transaction and display an error message.

REQ-SEC-011: [Unwanted] If a transaction amount exceeds a system-defined maximum transaction limit (e.g., $25,000 for a single transaction), the system shall reject the transaction and display an error message.

REQ-SEC-012: [Ubiquitous] The system shall validate transaction amounts server-side using strict numeric type checking and range validation, treating client-side validation as user experience enhancements only.

REQ-SEC-013: [Event-driven] When a transaction amount is received from user input, the system shall parse and validate it as a decimal number with exactly two decimal places for cents, rejecting values with more than two decimal places or invalid formatting.

### Financial Integrity and Coordination

REQ-SEC-014: [Event-driven] When a transaction is created, the system shall update the associated account's current balance and available credit in the same atomic database transaction to maintain financial integrity.

REQ-SEC-015: [Unwanted] If the account balance update fails after a transaction record is created, the system shall roll back the entire operation including the transaction record creation.

REQ-SEC-016: [Event-driven] When a bill payment is processed, the system shall create a corresponding transaction record in the transaction history to maintain a complete audit trail of all balance-affecting operations.

REQ-SEC-017: [Ubiquitous] The system shall ensure that the sum of all transaction amounts for an account equals the difference between the account's original balance and current balance, enabling transaction-to-balance reconciliation.

REQ-SEC-018: [Event-driven] When a transaction affects account balance, the system shall update both the Account.current_balance and Account.available_credit fields atomically within the same database transaction.

REQ-SEC-019: [Ubiquitous] The system shall implement database-level constraints or application-level checks to prevent account balances from exceeding credit limits or becoming invalid through transaction processing.

### CSRF Protection

REQ-SEC-020: [Ubiquitous] All transaction creation requests (POST operations) shall include and validate a CSRF token that is unique per user session.

REQ-SEC-021: [Unwanted] If a transaction creation request does not include a valid CSRF token matching the user's current session, the system shall reject the request and display an error message without creating the transaction.

REQ-SEC-022: [Event-driven] When the transaction creation screen is displayed, the system shall generate and embed a unique CSRF token in the form that must be submitted with the transaction request.

### Transaction Data Validation

REQ-SEC-023: [Unwanted] If a card number provided for transaction creation is not a valid 16-digit numeric value, the system shall reject the transaction and display an error message.

REQ-SEC-024: [Event-driven] When a transaction is created, the system shall verify that the specified card exists and is in active status before allowing the transaction.

REQ-SEC-025: [Unwanted] If a transaction is submitted with a card that is inactive, expired, or closed, the system shall reject the transaction with an appropriate error message.

REQ-SEC-026: [Ubiquitous] The system shall validate all transaction fields server-side including: transaction type, category, merchant information, and description, rejecting transactions with invalid or missing required fields.

### Search and Filtering Security

REQ-SEC-027: [Event-driven] When a user performs a transaction search with a card number filter, the system shall verify the user owns the specified card before executing the search.

REQ-SEC-028: [Unwanted] If a search parameter (card number, account number) contains SQL wildcard characters (%, _) or other special characters, the system shall escape or sanitize these characters before constructing database queries to prevent wildcard injection attacks.

REQ-SEC-029: [Ubiquitous] The system shall implement pagination limits (maximum records per page) to prevent bulk data extraction through repeated page requests.

REQ-SEC-030: [Ubiquitous] The system shall log transaction search operations that return large result sets or use wildcard patterns to detect potential data harvesting attempts.

### Error Handling

REQ-SEC-031: [Unwanted] If a transaction processing error occurs, the system shall display a generic error message to the user without exposing internal system details, database errors, or file paths.

REQ-SEC-032: [Ubiquitous] The system shall log detailed error information (including exception messages, stack traces, and system state) to internal logs for debugging, while displaying only generic messages to users.

### Rate Limiting and Fraud Prevention

REQ-SEC-033: [Ubiquitous] The system shall implement rate limiting on transaction creation to prevent automated abuse (e.g., maximum 50 transaction creations per user per hour).

REQ-SEC-034: [Event-driven] When a user creates multiple transactions within a short time period, the system shall flag the activity for fraud review if the pattern is unusual for that user.

REQ-SEC-035: [Ubiquitous] The system shall monitor for suspicious transaction patterns (e.g., rapid repeated transactions, transactions with unusual amounts or merchants) and alert fraud prevention teams.

### Non-Functional Security Requirements

REQ-SEC-N-001: [Ubiquitous] All transaction operations (create, view, update) shall be logged to a tamper-evident audit trail including: timestamp, user ID, transaction ID, card number (masked), amount, and operation result.

REQ-SEC-N-002: [Ubiquitous] The system shall implement idempotency controls to prevent duplicate transaction creation if a user submits the same transaction multiple times.

REQ-SEC-N-003: [Ubiquitous] Transaction processing operations shall complete within a defined timeout period (e.g., 30 seconds), after which the operation shall be rolled back and the user notified of the timeout.

REQ-SEC-N-004: [Ubiquitous] The system shall maintain referential integrity between transactions, cards, accounts, and customers, preventing orphaned transaction records.

### Open Questions

OQ-SEC-01: What is the appropriate maximum single transaction limit for different transaction types (purchase, cash advance, balance transfer)? — Owner: Business analyst / Risk management team

OQ-SEC-02: Should the system support transaction reversal or void operations, and if so, what authorization and audit requirements apply? — Owner: Product owner / Compliance team

OQ-SEC-03: What is the required retention period for transaction records and audit logs? — Owner: Compliance team / Legal team

OQ-SEC-04: Should transaction creation trigger real-time fraud detection checks, and if so, what fraud detection service should be integrated? — Owner: Fraud prevention team / Security architecture team
