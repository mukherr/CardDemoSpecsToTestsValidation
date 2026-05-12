# Bill Payment Processing — Requirements

## 1. Global Preconditions

- User must be authenticated via a valid session context before accessing bill payment functions.
- An active session context must carry the authenticated user identifier and account reference for downstream processing.
- The user must hold a customer role with an associated credit card account to initiate a payment.
- OQ-BP01: Are there additional role-based guards (e.g., account status checks such as frozen or closed accounts) that restrict access to bill payment? Owner: Business Analyst.


### Navigation Context

- The user selects the bill payment option from the application main menu, which transfers control to the bill payment screen.
- The bill payment screen receives the session context containing user and account identifiers passed from the menu.
- Upon completing or cancelling a payment, control returns to the application main menu.
- The sign-on subroutine is invoked internally to validate session credentials during navigation; it is not directly accessible by the user.
- OQ-BP02: Is there an intermediate confirmation or summary screen between payment submission and return to the menu, or does control return directly? Owner: Business Analyst.

---

## 2. Bill Payment Processing


As a credit card customer, I want to pay my account balance in full through an online interface so that my outstanding balance is settled and a payment transaction is recorded.

### Requirements

**Screen Display**

REQ-F-001: [Ubiquitous] The system shall display the bill payment screen with the current date (formatted MM-DD-YY), current time (formatted HH:MM:SS), program name, transaction identifier, and application titles ("AWS Mainframe Modernization" and "CardDemo"), along with any pending error or informational message.

**User Input Routing**

REQ-F-002: [Event-driven] When the user presses Enter on the bill payment screen, the system shall process the payment entry.

REQ-F-003: [Event-driven] When the user presses PF4 on the bill payment screen, the system shall clear all input fields (account ID, current balance, confirmation, and messages) and redisplay the bill payment screen.

REQ-F-004: [Event-driven] When the user presses any key other than Enter, PF3, or PF4 on the bill payment screen, the system shall display an error message indicating that an invalid key was pressed.

**Account Validation**

REQ-F-005: [Event-driven] When the user submits a payment with an empty account ID, the system shall display an error message, set focus to the account ID field, and redisplay the screen without processing the payment.

REQ-F-006: [Unwanted] If the account is not found or a retrieval error occurs, the system shall display an appropriate error message (account not found or lookup failed), set focus to the account ID field, and redisplay the screen.

**Balance Validation**

REQ-F-007: [Unwanted] If the account balance is zero or negative, the system shall display an error message indicating no payment is due, set focus to the account ID field, and redisplay the screen.

**Payment Confirmation Evaluation**

REQ-F-008: [Complex] While the account ID is valid and the account has a positive balance, when the confirmation field is empty, the system shall retrieve the account data and display the current account balance on the screen with a prompt requesting payment confirmation.

REQ-F-009: [Complex] While the account ID is valid and the account has a positive balance, when the user enters 'Y' or 'y' in the confirmation field, the system shall set the confirmation flag and proceed with payment processing.

REQ-F-010: [Complex] While the account ID is valid and the account has a positive balance, when the user enters 'N' or 'n' in the confirmation field, the system shall clear the screen and redisplay the bill payment screen without processing the payment.

REQ-F-011: [Complex] While the account ID is valid and the account has a positive balance, when the user enters any value other than 'Y', 'y', 'N', 'n', or empty in the confirmation field, the system shall display an error message indicating that valid values are Y or N, set focus to the confirmation field, and redisplay the screen.

**Payment Transaction Creation**

REQ-F-012: [Complex] While the payment is confirmed and no error has occurred, when the system processes the payment, it shall retrieve the card cross-reference data for the account, determine the next transaction ID by reading the most recent transaction record from the transaction keyed dataset (legacy: AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS) and incrementing its transaction ID by one.

REQ-F-013: [Event-driven] When the system creates the payment transaction record, it shall populate the record with: transaction type '02', category '2', source 'POS TERM', description 'BILL PAYMENT - ONLINE', amount equal to the full account current balance, merchant ID '999999999', merchant name 'BILL PAYMENT', merchant city 'N/A', merchant zip 'N/A', and the current system timestamp.

REQ-F-014: [Event-driven] When the payment transaction record is populated, the system shall write it to the transaction keyed dataset (legacy: AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS).

**Account Balance Update**

REQ-F-015: [Event-driven] When the transaction record is successfully written, the system shall compute the new account balance by subtracting the payment amount from the current balance and update the account record in the account keyed dataset (legacy: AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS).

**Success and Error Feedback**

REQ-F-016: [Event-driven] When the transaction write and account update succeed, the system shall clear all input fields, display a success message containing the transaction ID, and redisplay the bill payment screen.

REQ-F-017: [Unwanted] If a duplicate key is detected when writing the transaction record, the system shall display an error message, set focus to the account ID field, and redisplay the screen without completing the payment.

REQ-F-018: [Unwanted] If a write error (other than duplicate key) occurs when writing the transaction record, the system shall display an error message, set focus to the account ID field, and redisplay the screen.

REQ-F-019: [Unwanted] If the account update fails (account not found or update error), the system shall display an appropriate error message, set focus to the account ID field, and redisplay the screen.

REQ-F-020: [Unwanted] If the card cross-reference data cannot be retrieved (account not found or lookup failed), the system shall display an appropriate error message, set focus to the account ID field, and redisplay the screen.

REQ-F-021: [Unwanted] If the end of the transaction file is reached when determining the next transaction ID (no prior transactions exist), the system shall set the transaction ID to zeros and proceed with payment processing.

REQ-F-022: [Unwanted] If a read error (other than end-of-file) occurs when reading the previous transaction, the system shall display an error message, set focus to the account ID field, and redisplay the screen.

REQ-F-023: [Unwanted] If a browse start error occurs when positioning to the end of the transaction file, the system shall display an appropriate error message, set focus to the account ID field, and redisplay the screen.

### Non-Functional Requirements

REQ-N-001: [Unwanted] If the system writes to both the transaction keyed dataset and the account keyed dataset during payment processing, the writes shall be performed as a transaction that either succeeds completely or fails completely.

---

## 3. Bill Payment Navigation


As a credit card customer, I want to navigate to and from the bill payment screen so that I can access the payment function from the application menu and return when finished.

### Requirements

REQ-F-024: [Event-driven] When a request is received with no session context, the system shall set the target program to the sign-on screen and transfer control to that program.

REQ-F-025: [Complex] While session context is present and this is a re-entry into the program, when the user presses PF3 (exit), the system shall transfer control to the calling program recorded in the session context, or to the main menu if no calling program is recorded.

REQ-F-026: [Ubiquitous] The system shall record the current program name and transaction identifier as the origin point in the session context, reset the re-entry flag to indicate fresh entry, and transfer control to the target program with the updated session context.

REQ-F-027: [Unwanted] If the target program in the session context is empty or blank, the system shall default the target to the sign-on screen before transferring control.

### Open Questions

OQ-1: The transaction ID for a new payment is determined by reading the last transaction record and incrementing by one. In a concurrent multi-user environment, this approach may produce duplicate IDs. Should the modernized system use a different mechanism (e.g., database sequence) to guarantee uniqueness? — Owner: application architecture team

OQ-2: The payment always pays the full account balance (no partial payments). Is this a deliberate business constraint to be preserved, or a limitation of the legacy implementation that should be relaxed? — Owner: product owner / business analyst

---



## Shared Capability Dependencies

This capability depends on the following shared capabilities.
Do not reimplement their behavior — integrate with the shared service.

- **COSGN00C** (`_shared/COSGN00C/`) — As a user of the CardDemo application, I want to sign on with my credentials so that I am authenticated and directed to the appropriate application functions based on my role.


---

## 4. Security Hardening Requirements

**Based on Security Scan Findings:** #2, #6, #9

### Payment Amount Validation

REQ-SEC-001: [Unwanted] If the payment amount is less than or equal to zero, the system shall reject the payment and display an error message indicating the payment amount must be positive.

REQ-SEC-002: [Unwanted] If the payment amount is not a valid numeric value (including rejection of NaN, Infinity, -Infinity, or non-numeric strings), the system shall reject the payment and display an error message.

REQ-SEC-003: [Unwanted] If the payment amount exceeds the current account balance, the system shall reject the payment and display an error message indicating the payment cannot exceed the outstanding balance.

REQ-SEC-004: [Unwanted] If the payment amount exceeds a system-defined maximum single payment limit (e.g., $50,000), the system shall reject the payment and display an error message requiring the payment to be split or processed through an alternative channel.

REQ-SEC-005: [Ubiquitous] The system shall validate payment amounts server-side using strict numeric type checking and range validation, treating client-side HTML5 validation (min/max attributes) as user experience enhancements only.

REQ-SEC-006: [Event-driven] When a payment amount is received from user input, the system shall parse and validate it as a decimal number with exactly two decimal places for cents, rejecting values with more than two decimal places or invalid formatting.

### Account Ownership Verification

REQ-SEC-007: [Event-driven] When a user submits a bill payment, the system shall verify that the specified account ID belongs to the authenticated user before processing the payment.

REQ-SEC-008: [Event-driven] When verifying account ownership, the system shall query the account-to-customer-to-user relationship and reject the payment if the account does not belong to the authenticated user.

REQ-SEC-009: [Unwanted] If a user attempts to make a payment on an account they do not own, the system shall reject the payment with a generic "account not found" error message rather than revealing the account exists but is unauthorized.

REQ-SEC-010: [Ubiquitous] The system shall log all payment attempts including: timestamp, user ID, account ID, payment amount, and success/failure status to an audit trail for fraud detection and investigation.

REQ-SEC-011: [Ubiquitous] Administrator users with explicit payment processing privileges may process payments on behalf of customers, but such operations shall be logged with the administrator ID and a flag indicating administrative override.

### CSRF Protection

REQ-SEC-012: [Ubiquitous] All bill payment submission requests (POST operations) shall include and validate a CSRF token that is unique per user session.

REQ-SEC-013: [Unwanted] If a bill payment request does not include a valid CSRF token matching the user's current session, the system shall reject the request and display an error message without processing the payment.

REQ-SEC-014: [Event-driven] When the bill payment screen is displayed, the system shall generate and embed a unique CSRF token in the payment form that must be submitted with the payment request.

REQ-SEC-015: [Ubiquitous] The system shall invalidate CSRF tokens after successful use or after session expiration to prevent token reuse attacks.

### Payment Confirmation

REQ-SEC-016: [Event-driven] When a payment amount exceeds a defined threshold (e.g., $1,000), the system shall require explicit user confirmation before processing the payment, displaying the account ID and payment amount for user verification.

REQ-SEC-017: [Event-driven] When a user confirms a payment, the system shall re-validate all payment parameters (account ownership, amount validity, account balance) immediately before processing to prevent time-of-check-time-of-use (TOCTOU) vulnerabilities.

### Financial Integrity

REQ-SEC-018: [Ubiquitous] The system shall process payment transactions atomically, ensuring that the transaction record creation and account balance update either both succeed or both fail, with no partial updates.

REQ-SEC-019: [Event-driven] When a payment transaction is created, the system shall update the corresponding account balance in the same database transaction to maintain financial integrity between transaction history and account balances.

REQ-SEC-020: [Unwanted] If the account balance update fails after the transaction record is created, the system shall roll back the entire payment operation and display an error message to the user.

REQ-SEC-021: [Ubiquitous] The system shall implement database-level constraints or application-level checks to prevent account balances from becoming negative through payment processing.

### Error Handling

REQ-SEC-022: [Unwanted] If a payment processing error occurs, the system shall display a generic error message to the user (e.g., "Payment processing failed. Please try again.") without exposing internal system details, database errors, or file paths.

REQ-SEC-023: [Ubiquitous] The system shall log detailed error information (including exception messages, stack traces, and system state) to internal logs for debugging, while displaying only generic messages to users.

REQ-SEC-024: [Event-driven] When a payment fails due to system error, the system shall ensure no partial payment is recorded and the account balance remains unchanged from its pre-payment state.

### Rate Limiting and Fraud Prevention

REQ-SEC-025: [Ubiquitous] The system shall implement rate limiting on payment submissions to prevent automated abuse (e.g., maximum 10 payment attempts per user per hour).

REQ-SEC-026: [Event-driven] When a user submits multiple failed payment attempts within a short time period, the system shall flag the activity for fraud review and may temporarily restrict payment functionality for that user.

REQ-SEC-027: [Ubiquitous] The system shall monitor for suspicious payment patterns (e.g., rapid repeated payments, payments immediately followed by refund requests) and alert fraud prevention teams.

### Non-Functional Security Requirements

REQ-SEC-N-001: [Ubiquitous] All payment operations shall be logged to a tamper-evident audit trail including: timestamp, user ID, account ID, payment amount, transaction ID, IP address, and operation result.

REQ-SEC-N-002: [Ubiquitous] The system shall implement idempotency controls to prevent duplicate payment processing if a user submits the same payment multiple times (e.g., through double-clicking or browser refresh).

REQ-SEC-N-003: [Ubiquitous] Payment processing operations shall complete within a defined timeout period (e.g., 30 seconds), after which the operation shall be rolled back and the user notified of the timeout.

### Open Questions

OQ-SEC-01: What is the appropriate maximum single payment limit for the system? — Owner: Business analyst / Risk management team

OQ-SEC-02: What payment amount threshold should trigger additional confirmation or verification steps? — Owner: Business analyst / Fraud prevention team

OQ-SEC-03: Should the system support partial payments (paying less than the full balance), and if so, what are the minimum partial payment amounts and rules? — Owner: Product owner / Business analyst

OQ-SEC-04: What is the required retention period for payment audit logs and transaction records? — Owner: Compliance team / Legal team
