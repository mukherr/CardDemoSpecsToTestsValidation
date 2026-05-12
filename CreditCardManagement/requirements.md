# Credit Card Management — Requirements

## 1. Global Preconditions

- User must be authenticated via the sign-on process (COSGN00C) before accessing any online credit card management function.
- A valid session context must be established and passed between screens; unauthenticated or expired sessions must redirect to sign-on.
- Online functions require an active connection to the card master data store (first mention: CARDDAT) and the card cross-reference data store (first mention: CARDXREF).
- Batch processes (data store refresh, sequential retrieval, cross-reference initialization) require successful completion of prior scheduling prerequisites and availability of the authoritative sequential source.


> **Note:** The legacy system implements some of this processing as scheduled batch jobs. The modernized system may implement these requirements as batch, event-driven, or real-time processing provided the requirements below are met.


### Navigation Context

- Users access credit card functions from a main menu by selecting the appropriate option, which transfers control to the card list screen (txn CC00).
- From the card list screen, users may filter by account identifier or card number, paginate through results, and select a card to navigate to either the detail inquiry screen (txn CCDL) or the detail update screen (txn CCUP).
- The detail inquiry and detail update screens return the user to the card list screen upon completion or cancellation.
- Subroutines for account validation (CBACT02C, CBACT03C) are invoked internally by the online programs and are not directly navigable by users.
- Batch functions (data store refresh, sequential retrieval, cross-reference initialization/retrieval) operate independently of the online navigation flow and are initiated through batch scheduling.

- **OQ-NAV1** *(Owner: Business Analyst)*: Is navigation from the card list to detail inquiry vs. detail update determined by a user-selected action code on the list screen, or by separate menu options? The available program descriptions do not clarify the selection mechanism.

---

## 2. Credit Card List Display and Navigation


As a card operations user, I want to view a paginated list of credit cards with optional filtering by account identifier or card number so that I can locate specific cards for viewing or updating.

### Requirements

**Input Reception and Validation**

REQ-F-001: [Event-driven] When the user submits the card list screen, the system shall receive and extract the account identifier, card number, and row selection codes (one per displayed row, up to 7 rows) into working variables for validation.

REQ-F-002: [Event-driven] When the account identifier input is validated, the system shall verify that it contains only numeric characters and mark the input as invalid if it is non-numeric.

REQ-F-003: [Event-driven] When the card number input is validated, the system shall verify that it contains only numeric characters and mark the input as invalid if it is non-numeric.

REQ-F-004: [State-driven] While the system is validating row selection codes, the system shall verify that each row's selection code is blank, 'S' (view detail), or 'U' (update) and mark the input as invalid if any row contains a value other than these three.

REQ-F-005: [Unwanted] If more than one row is marked for selection (either 'S' or 'U'), the system shall mark the input as invalid.

REQ-F-006: [Event-driven] When user input fails validation and no filter validation errors exist, the system shall read the first page of card records and redisplay the screen with the error message and the user's original input preserved for correction.

REQ-F-007: [Event-driven] When user input fails validation and filter validation errors exist, the system shall redisplay the screen with the error message and the user's original input preserved for correction without reading card records.

**Function Key Handling**

REQ-F-008: [Ubiquitous] The system shall translate the user's physical key input to a standardized function key identifier, mapping PF13–PF24 to their PF1–PF12 equivalents.

REQ-F-009: [Ubiquitous] The system shall recognize only Enter, PF3 (exit), PF7 (page up), and PF8 (page down) as valid function keys for the card list screen and normalize any other key press to Enter.

**Card Record Retrieval and Filtering**

REQ-F-010: [Event-driven] When a card record is read from the card master file (legacy: AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS), the system shall apply the active filters: if the account identifier filter is active and the card's account identifier does not match, the record shall be excluded; if the card number filter is active and the card number does not match, the record shall be excluded; otherwise the record shall be included for display.

REQ-F-011: [Event-driven] When a card record passes the filter criteria, the system shall add the card number, account identifier, and card status to the next available row in the display buffer and increment the row counter.

REQ-F-012: [Event-driven] When the display buffer is full (7 rows), the system shall read one additional record to determine whether more pages exist and set the next-page indicator accordingly.

REQ-F-013: [Event-driven] When the end of the data store is reached during card record retrieval, the system shall stop reading, set the next-page indicator to indicate no more pages, and display an informational message.

REQ-F-014: [Event-driven] When the end of the data store is reached on the first page with no records retrieved, the system shall indicate that no records were found for the given search criteria.

REQ-F-015: [Unwanted] If a read error occurs during card record retrieval, the system shall stop reading and display an error message containing the operation name, file name, response code, and reason code.

**Pagination**

REQ-F-016: [Complex] While more pages are available, when the user presses PF8 (page down), the system shall increment the page counter, read the next page of card records starting from the last card of the current page, and display the new page.

REQ-F-017: [Complex] While the user is on a page other than the first page, when the user presses PF7 (page up), the system shall decrement the page counter, read the previous page of card records in backward order, and display the previous page.

REQ-F-018: [Event-driven] When the user presses PF7 (page up) while on the first page, the system shall redisplay the first page with an error message 'NO PREVIOUS PAGES TO DISPLAY'.

REQ-F-019: [Event-driven] When the user presses PF8 (page down) and no more pages exist and the last page has already been shown, the system shall display an error message 'NO MORE PAGES TO DISPLAY'.

REQ-F-020: [Event-driven] When the user navigates using a key other than PF8, the system shall reset the last-page indicator to indicate that the last page has not been displayed.

**Screen Display Messages**

REQ-F-021: [Ubiquitous] The system shall display the informational message 'TYPE S FOR DETAIL, U TO UPDATE ANY RECORD' when no error condition exists and more pages are available or the last page has not yet been shown.

REQ-F-022: [Ubiquitous] The system shall protect (make read-only) any card row that contains no card data, and make rows containing card data selectable.

**Default and Unrecognized Key Handling**

REQ-F-023: [Event-driven] When the user presses an unrecognized function key or no other navigation condition matches, the system shall read the first page of card records and redisplay the screen.

---

## 3. Credit Card List Navigation and Selection


As a card operations user, I want to select a card from the list for viewing or updating so that I can access card details or modify card information.

### Requirements

**View Card Detail**

REQ-F-024: [Complex] While the user has selected exactly one card row marked 'S' (view), when the user presses Enter, the system shall populate the session context with the selected card's account identifier (numeric, 11 digits) and card number (numeric, 16 digits), set the user type to standard user, mark the program state as re-entry, record the current screen location for return navigation, and transfer control to the card detail program.

**Update Card**

REQ-F-025: [Complex] While the user has selected exactly one card row marked 'U' (update), when the user presses Enter, the system shall populate the session context with the originating transaction identifier (alphanumeric, 4 characters), originating program name (alphanumeric, 8 characters), user type, program context entry flag, selected card number (numeric, 16 digits), and selected account identifier (numeric, 11 digits), and transfer control to the card update program.

**Exit to Main Menu**

REQ-F-026: [Event-driven] When the user presses PF3 (exit) and the program is in its initial entry state, the system shall record the current transaction identifier and program name as the originating source, set the user type to standard user, capture the current screen map information, designate the main menu program as the destination, mark the context as a fresh entry, and transfer control to the main menu program with the updated session context.

REQ-F-027: [Complex] While the program is in a re-entry context from a different program, when the user presses PF3 (exit), the system shall reset all session state, reinitialize the program context as entry, reset pagination to the first page, read the first page of card records, and redisplay the screen.

**Context Reset on External Entry**

REQ-F-028: [Event-driven] When the program is entered from a different program, the system shall discard any prior pagination and filter state and reset to the first page to ensure a fresh view.

### Open Questions

OQ-1: The exit behavior differs depending on whether the program is in "initial entry state" versus "re-entry context from a different program." The rules describe PF3 in initial entry as navigating to the main menu, but PF3 in re-entry context as resetting state and staying on the card list. Is this the intended UX, or should PF3 always return to the calling program? — Owner: business analyst / UX team

OQ-2: The filter matching logic for account identifier and card number is described as "does not match" but does not specify whether this is an exact match or a prefix/partial match. Clarify the matching semantics for card list filtering. — Owner: business analyst

---

## 4. Credit Card Detail Inquiry


As a credit card system user, I want to search for and view detailed information about a specific credit card by entering an account number or card number so that I can review cardholder details, expiration date, and card status.

### Requirements

REQ-F-029: [Event-driven] When the user enters the credit card detail inquiry function from any source other than the card list program, the system shall display the search screen prompting the user to enter an account number and/or card number as search criteria.

REQ-F-030: [Event-driven] When the user enters the credit card detail inquiry function from the card list program, the system shall retrieve the card number from the session context (specifically the card number passed by the calling program), fetch the full card details from the card master file (legacy: CARDFILE-FILE), and display the card details on the screen.

REQ-F-031: [Event-driven] When the user submits the search screen, the system shall validate the account number and card number individually, then perform cross-field validation across both fields.

REQ-F-032: [Unwanted] If validation errors are found on the submitted search criteria, the system shall redisplay the search screen with error messages identifying the invalid fields.

REQ-F-033: [Event-driven] When validation of the search criteria succeeds, the system shall retrieve matching card records from the card master file using the card number as the search key and display the card details.

REQ-F-034: [Unwanted] If no matching card record is found in the card master file for the provided card number, the system shall indicate that both the account number and card number are invalid and redisplay the search screen with an appropriate error message.

REQ-F-035: [Unwanted] If an error occurs during retrieval from the card master file, the system shall indicate a retrieval failure and redisplay the search screen with an error message.

REQ-F-036: [Ubiquitous] The system shall accept only Enter (to submit search criteria) and PF3 (to exit) as valid user actions on the search screen; any other function key shall be treated as Enter.

REQ-F-037: [Ubiquitous] The system shall map extended function keys PF13–PF24 to their base equivalents PF1–PF12 for consistent processing.

REQ-F-038: [Event-driven] When the user presses PF3 (exit), the system shall navigate to the calling program if one is recorded in the session context, passing the originating transaction identifier, this program's identifier, the user ID, user type, and the current screen reference; if no calling program is recorded, the system shall navigate to the main menu.

REQ-F-039: [Ubiquitous] The system shall display an informational prompt message instructing the user to enter search criteria when the search screen is shown for the first time with no pre-existing information message.

REQ-F-040: [Unwanted] If the program reaches an unexpected processing state that does not match initial entry, entry from card list, or re-entry with user input, the system shall check for input errors and, if present, redisplay the search screen with error messages.

### Open Questions

OQ-3: The cross-field validation between account number and card number is referenced but the specific cross-field rules (e.g., must both be provided, must be consistent with each other) are not explicitly defined. What are the exact cross-field validation rules? — Owner: Business analyst / Credit card operations team

---

## 5. Credit Card Detail Update


As a credit card operations user, I want to search for a credit card record by account and card number, view its details, modify card attributes (cardholder name, expiration date, active status), and save validated changes so that card master data remains accurate and current.

### Requirements

**Session Restoration**

REQ-F-041: [Event-driven] When the program is re-entered with an existing session context containing previously fetched card details, the system shall restore the prior card details (account identifier, card number, cardholder name, expiration date, active status) from the session context, mark search criteria as valid, and indicate that card details are available for editing.

REQ-F-042: [Event-driven] When the program is re-entered with an existing session context but card details have not yet been fetched, the system shall restore the session data (user ID, user type, account identifier, card number) and resume processing from the search state.

**Function Key Handling**

REQ-F-043: [Ubiquitous] The system shall recognize Enter, F3 (exit), F5 (confirm changes when changes are pending), and F12 (return to prior screen when card details have been fetched) as valid function keys for the card update screen.

REQ-F-044: [Unwanted] If the user presses a function key that is not valid for the current screen state, the system shall treat the input as Enter and redisplay the current screen.

**Input Reception**

REQ-F-045: [Event-driven] When the user submits the card detail screen, the system shall receive all card detail fields (account ID, card number, cardholder name, card active status, expiry month, expiry year, expiry day) and treat any field containing an asterisk or left blank as a null value.

**Validation — Account ID**

REQ-F-046: [Complex] While the user has submitted card detail input, when the account ID field is evaluated, the system shall reject the input with an error message prompting for an account number if the account ID is blank, null, or zero.

REQ-F-047: [Complex] While the user has submitted card detail input, when the account ID field is evaluated, the system shall reject the input with an error message stating the account number must be 11 digits if the value is non-numeric.

**Validation — Card Number**

REQ-F-048: [Complex] While the user has submitted card detail input, when the card number field is evaluated, the system shall reject the input with an error message prompting for a card number if the card number is blank, null, or zero.

REQ-F-049: [Complex] While the user has submitted card detail input, when the card number field is evaluated, the system shall reject the input with an error message stating the card number must be a 16-digit number if the value is non-numeric.

**Validation — Cardholder Name**

REQ-F-050: [Complex] While the user has submitted card detail input, when the cardholder name field is evaluated, the system shall reject the input with an error message prompting for a card name if the value is blank, null, or zero.

REQ-F-051: [Complex] While the user has submitted card detail input, when the cardholder name field is evaluated, the system shall reject the input with an error message stating that only alphabets and spaces are allowed if the value contains non-alphabetic characters (excluding spaces).

**Validation — Expiry Month**

REQ-F-052: [Complex] While the user has submitted card detail input, when the expiry month field is evaluated, the system shall reject the input with an error message stating the month must be between 1 and 12 if the value is blank, null, zero, non-numeric, or outside the range 1–12.

**Validation — Expiry Year**

REQ-F-053: [Complex] While the user has submitted card detail input, when the expiry year field is evaluated, the system shall reject the input with an error message stating the year is invalid if the value is blank, null, zero, non-numeric, or outside the range 1950–2099.

**Validation — Active Status**

REQ-F-054: [Complex] While the user has submitted card detail input, when the card active status field is evaluated, the system shall reject the input with an error message stating the status must be Y or N if the value is blank, null, zero, or any character other than 'Y' or 'N'.

**Validation Completion**

REQ-F-055: [State-driven] While all field validations have been completed, the system shall mark input validation as failed if any individual field validation produced an error, preventing progression to the confirmation step.

**Card Retrieval**

REQ-F-056: [Complex] While the user has provided a valid account ID and card number, when the system retrieves the card record from the card master file (legacy: CARDFILE-FILE) using the card number as key, the system shall mark the card as found and make its details available for display and editing if the record exists.

REQ-F-057: [Unwanted] If the card record cannot be found in the card master file or a retrieval error occurs, the system shall display an appropriate error message and not proceed to the detail editing state.

**Change Detection**

REQ-F-058: [Complex] While the user has submitted card detail input that passes validation, when the new card details are compared with the previously fetched details using case-insensitive comparison, the system shall flag that no changes were detected if the values are identical and shall not proceed to the confirmation step.

**Confirmation Prompt**

REQ-F-059: [Complex] While the user has submitted card details that pass all validation and differ from the previously fetched details, when validation completes with no errors, the system shall set a changes-pending-confirmation state and prompt the user to press F5 to confirm the changes before saving.

**Card Record Assembly**

REQ-F-060: [Ubiquitous] The system shall assemble the updated card record by populating card number, account ID, CVV code, cardholder embossed name, expiry date (formatted as YYYY-MM-DD), and active status from the validated user input.

**Update Lock and Persistence**

REQ-F-061: [Event-driven] When the user confirms changes (F5), the system shall read the card record from the card master file using the card number as key and request an exclusive update lock before writing changes.

REQ-F-062: [Unwanted] If the system cannot obtain an update lock on the card record, the system shall display an error message indicating the record is currently locked by another user and shall not persist the changes.

REQ-F-063: [Unwanted] If the system detects that the card record was modified by another user since it was originally fetched, the system shall display a message indicating the record has been modified by another user and return to the detail display for the user to review current data before reattempting the update.

REQ-F-064: [Event-driven] When the update lock is obtained and no concurrent modification is detected, the system shall write the assembled updated card record to the card master file.

REQ-F-065: [Event-driven] When the write operation completes successfully, the system shall display a success message confirming the card record has been updated.

REQ-F-066: [Unwanted] If the write operation fails after the update lock was successfully obtained, the system shall display an error message indicating the update could not be completed.

**Information Messages**

REQ-F-067: [State-driven] While the program is preparing to display the screen, the system shall display a contextual information message: prompt for search keys on initial entry, prompt for changes when details are shown, prompt for confirmation when changes are pending, confirm success when update is complete, or inform of failure when an error occurs.

**Navigation**

REQ-F-068: [Event-driven] When the user presses F3, the system shall transfer control to the calling program (defaulting to the menu program if no caller is recorded), passing the session context including user ID, user type, account identifier, and card number.

REQ-F-069: [Event-driven] When the user presses F12 after card details have been fetched, the system shall transfer control to the calling program, passing the session context including user ID, user type, account identifier, and card number.

REQ-F-070: [Event-driven] When the user returns from the card list screen, the system shall clear the account identifier and card number to force a fresh search.

**Unexpected State**

REQ-F-071: [Unwanted] If the program reaches a state that does not match any expected processing condition, the system shall halt processing with the error message 'UNEXPECTED DATA SCENARIO'.

### Non-Functional Requirements

REQ-N-001: [Unwanted] If concurrent updates target the same card record, the system shall detect the conflict by comparing the current record state against the originally fetched state and reject the stale write with a user notification.

REQ-N-002: [Unwanted] If the update lock acquisition or write operation fails, the system shall not leave the card record in a partially updated state; the record shall remain unchanged from its pre-update values.

### Open Questions

OQ-4: The expiry day field is received from the user but no explicit validation rule for the day value (e.g., 1–31, or valid for the given month/year) is described. Should the system validate the expiry day against the month and year for calendar correctness? — Owner: Credit Card Operations domain expert

OQ-5: The CVV code is included in the assembled update record but no user input or validation rule for CVV is described. Is the CVV carried forward from the original fetched record unchanged, or can the user modify it? — Owner: Credit Card Operations domain expert

---

## 6. Card Data Store Refresh and Reindexing


As a batch operations team, I want the card data store fully refreshed from the authoritative sequential source and its alternate index rebuilt so that online and batch processes can access card records by both primary key and account-based alternate key with consistent, up-to-date data.

### Requirements

REQ-F-072: [Event-driven] When the card data refresh job executes, the system shall close the card data file and its alternate index in the online transaction processing environment to prevent concurrent access during the refresh process.

REQ-F-073: [Event-driven] When the card data refresh job executes, the system shall replace the contents of the card keyed dataset (legacy: AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS) with all card records from the card sequential file (legacy: AWS.M2.CARDDEMO.CARDDATA.PS), organized by a 16-byte primary key.

REQ-F-074: [Event-driven] When the card data store has been successfully loaded, the system shall rebuild the card alternate index (legacy: AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX) using a secondary key at record positions 11–16, configured for non-unique key values and automatic synchronization with the primary card data store.

REQ-F-075: [Event-driven] When the alternate index has been rebuilt, the system shall define the card alternate path (legacy: AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX.PATH) that routes queries through the alternate index to retrieve card records by the secondary key.

REQ-F-076: [Event-driven] When the card data store refresh and reindexing completes successfully, the system shall reopen the card data file and its alternate index in the online transaction processing environment to restore normal business operations.

REQ-F-077: [Unwanted] If the card data store refresh fails at any step, the system shall not reopen the card data file and alternate index in the online transaction processing environment, and shall report the failure status for operator review.

### Non-Functional Requirements

REQ-N-003: [Ubiquitous] The system shall ensure that the card data file and its alternate index are unavailable to online users for the duration of the refresh process to guarantee data integrity.

REQ-N-004: [Ubiquitous] The card data store refresh shall be idempotent — re-executing the job shall produce the same result as a single execution, fully replacing the prior card data and alternate index.

### Open Questions

OQ-6: The secondary key at positions 11–16 of the card record is used for the alternate index. Based on the field glossary, this appears to correspond to the card account identifier, but the exact field mapping should be confirmed. — Owner: data architecture team

OQ-7: The rule specifies "5 cylinders of storage" for the alternate index. Is there a business constraint on the maximum number of card records or a sizing requirement that should be preserved, or is this purely an infrastructure detail? — Owner: capacity planning team

---

## 7. Card Master Data Sequential Retrieval


As a batch operations team, I want all card records extracted sequentially from the card master data store and output for review so that card portfolio data is available for downstream reporting, inquiry, and audit purposes.

### Requirements

REQ-F-078: [Event-driven] When the card data retrieval job executes, the system shall read all card records from the card keyed dataset (legacy: AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS) sequentially and produce an output containing each card record's card number, card account identifier, card CVV code, embossed cardholder name, expiration date, and active status.

REQ-F-079: [Ubiquitous] The system shall process all card records in the card keyed dataset until no further records remain, producing one output entry per card record.

REQ-F-080: [Unwanted] If a card record cannot be retrieved due to a data access error (other than reaching the end of available records), the system shall terminate processing.

---

## 8. Card Cross-Reference Data Retrieval


As a business analyst, I want to retrieve and display the relationships between card numbers and their associated customer and account identifiers from the card cross-reference data store so that I can review card-to-account mappings for analysis and reporting purposes.

### Requirements

REQ-F-081: [Event-driven] When the card cross-reference retrieval process is initiated, the system shall read all records sequentially from the card cross-reference data store (legacy: AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS) and output each record's card number, customer identifier, and account identifier.

REQ-F-082: [State-driven] While unprocessed records remain in the card cross-reference data store, the system shall continue retrieving and outputting the next record containing the card number (alphanumeric, 16 characters), customer identifier (numeric, 9 digits), and account identifier (numeric, 11 digits).

REQ-F-083: [Event-driven] When no more records are available in the card cross-reference data store, the system shall terminate processing normally.

REQ-F-084: [Unwanted] If a read error occurs (other than end-of-data) while retrieving a cross-reference record, the system shall cease further record retrieval.

### Open Questions

OQ-8: The legacy implementation outputs records to the job log. In the modernized system, should the output be directed to a structured report file, a downstream integration endpoint, or a user-facing display? — Owner: business operations team

OQ-9: Is there a business requirement for the output to be ordered by card number (the primary key of the data store), or is any order acceptable? — Owner: business analyst team

---

## 9. Card Cross-Reference Data Store Initialization


As a credit card operations team, I want the card cross-reference data store fully provisioned with both primary and alternate key access paths so that online and batch applications can efficiently look up card relationships by either the primary card identifier or by alternate attributes such as account number.

**Restart/Recovery:** This process is idempotent; re-execution rebuilds the data store and indexes from the source file, producing the same end state regardless of prior failures.

### Requirements

REQ-F-085: [Event-driven] When the card cross-reference initialization process executes, the system shall load all records from the card cross-reference file (legacy: AWS.M2.CARDDEMO.CARDXREF.PS) into the card xref keyed dataset (legacy: AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS), preserving record content and order, with a 16-byte primary key and 50-byte fixed-length records.

REQ-F-086: [Event-driven] When the card xref keyed dataset has been populated, the system shall build an alternate index with a 25-character secondary key starting at byte position 11 of each record, supporting non-unique key values, and storing the index in the card xref alternate index (legacy: AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX).

REQ-F-087: [Ubiquitous] The system shall define an access path linking the card xref alternate index to the card xref keyed dataset so that applications can retrieve cross-reference records using the alternate key without requiring a full scan of the primary data store.

REQ-F-088: [Ubiquitous] The alternate index on the card xref keyed dataset shall be automatically synchronized whenever the primary data store is modified, ensuring alternate-key lookups always reflect current data.

REQ-F-089: [Event-driven] When a prior version of the card xref alternate index exists at the start of the initialization process, the system shall remove it before rebuilding; if no prior version exists, the process shall proceed without error.

### Non-Functional Requirements

REQ-N-005: [Unwanted] If the initialization process is interrupted after partial data load, re-execution shall completely replace the partially loaded data store and rebuild all indexes from the source file, ensuring a consistent final state.

### Open Questions

OQ-10: The alternate index key occupies positions 11–35 (25 bytes). What business attribute does this key represent — account identifier, customer identifier, or a composite? Clarification will improve field-level documentation. — Owner: card data domain expert

---

## 10. Job Dependencies

The following dependencies are inferred from shared data store access:

- **CARDFILE.jcl** (Section 6: Card Data Store Refresh and Reindexing) → **READCARD.jcl** (Section 7: Card Master Data Sequential Retrieval) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`)
- **CARDFILE.jcl** (Section 6: Card Data Store Refresh and Reindexing) → **READXREF.jcl** (Section 8: Card Cross-Reference Data Retrieval) (via `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`)
- **CARDFILE.jcl** (Section 6: Card Data Store Refresh and Reindexing) → **XREFFILE.jcl** (Section 9: Card Cross-Reference Data Store Initialization) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **XREFFILE.jcl** (Section 9: Card Cross-Reference Data Store Initialization) → **CARDFILE.jcl** (Section 6: Card Data Store Refresh and Reindexing) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **XREFFILE.jcl** (Section 9: Card Cross-Reference Data Store Initialization) → **READCARD.jcl** (Section 7: Card Master Data Sequential Retrieval) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`)
- **XREFFILE.jcl** (Section 9: Card Cross-Reference Data Store Initialization) → **READXREF.jcl** (Section 8: Card Cross-Reference Data Retrieval) (via `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`)

*These dependencies represent data flow ordering constraints. The implementation should ensure predecessor jobs complete before successors execute.*

---



## Shared Capability Dependencies

This capability depends on the following shared capabilities.
Do not reimplement their behavior — integrate with the shared service.

- **COSGN00C** (`_shared/COSGN00C/`) — As a user of the CardDemo application, I want to sign on with my credentials so that I am authenticated and directed to the appropriate application functions based on my role.


---

## 10. Security Hardening Requirements

**Based on Security Scan Findings:** #3, #5, #15, #18, #24

### Card Access Authorization

REQ-SEC-001: [Ubiquitous] The system shall verify that the authenticated user has ownership or authorized access to a card before allowing any view, update, or detail operation on that card record.

REQ-SEC-002: [Event-driven] When a user attempts to access a card detail or update screen, the system shall query the card-to-customer-to-user relationship and reject the request if the card does not belong to an account associated with the authenticated user.

REQ-SEC-003: [Event-driven] When a user attempts to view or update a card via direct URL or API call with a card number parameter, the system shall validate ownership before retrieving or displaying any card data.

REQ-SEC-004: [Unwanted] If a user attempts to access a card that does not belong to them, the system shall return a generic "card not found" error rather than revealing the card exists but is unauthorized.

REQ-SEC-005: [Ubiquitous] Administrator users with explicit card management privileges may access cards across all customers, but such access shall be logged to an audit trail with administrator ID, timestamp, and accessed card number.

### CVV Code Protection (PCI-DSS Compliance)

REQ-SEC-006: [Ubiquitous] The system shall mask CVV codes in all user interface displays, showing only asterisks (e.g., "***") or no value, and shall never display the full CVV code in any screen or report.

REQ-SEC-007: [Ubiquitous] The system shall mask CVV codes in all API responses, log files, and error messages, ensuring CVV values are never exposed outside the secure processing layer.

REQ-SEC-008: [Ubiquitous] The system shall encrypt CVV codes at rest in the database using field-level encryption with a secure encryption algorithm (AES-256 or equivalent) and proper key management.

REQ-SEC-009: [Event-driven] When a card record is retrieved for display, the system shall decrypt the CVV code only if required for authorized processing (e.g., payment authorization) and shall immediately mask it for display purposes.

REQ-SEC-010: [Ubiquitous] The system shall restrict CVV code decryption to only those system components that have a legitimate business need, implementing least-privilege access controls.

REQ-SEC-011: [Ubiquitous] The system shall log all CVV code access events (decryption operations) to a secure audit trail including timestamp, user ID, card number (masked), and operation type for PCI-DSS compliance monitoring.

### Sensitive Data Storage

REQ-SEC-012: [Ubiquitous] The system shall encrypt all sensitive cardholder data at rest, including: CVV codes, full card numbers (PAN), cardholder names, and expiration dates using field-level encryption with AES-256 or equivalent.

REQ-SEC-013: [Ubiquitous] The system shall mask Primary Account Numbers (PAN) in displays and logs, showing only the first 6 and last 4 digits (e.g., "424242******4242") in accordance with PCI-DSS requirements.

REQ-SEC-014: [Ubiquitous] The system shall store encryption keys separately from encrypted data, using a secure key management service or hardware security module (HSM), and shall rotate encryption keys according to organizational policy.

### Input Validation and Sanitization

REQ-SEC-015: [Unwanted] If a search parameter (account number or card number) contains SQL wildcard characters (%, _) or other special characters, the system shall escape or sanitize these characters before constructing database queries to prevent wildcard injection attacks.

REQ-SEC-016: [Ubiquitous] The system shall validate all card field inputs server-side before processing, including: card number format (16 digits), expiration date range (current date or future, year between 1950-2099), cardholder name (alphabetic characters and spaces only), and active status (Y or N only).

REQ-SEC-017: [Unwanted] If the expiration year is not a 4-digit numeric value between 1950 and 2099, the system shall reject the input and display an error message.

REQ-SEC-018: [Unwanted] If the expiration month is not a numeric value between 1 and 12, the system shall reject the input and display an error message.

REQ-SEC-019: [Unwanted] If the cardholder name contains non-alphabetic characters (excluding spaces and hyphens), the system shall reject the input and display an error message.

REQ-SEC-020: [Unwanted] If the active status is not exactly 'Y' or 'N' (case-sensitive), the system shall reject the input and display an error message.

REQ-SEC-021: [Ubiquitous] The system shall perform all input validation on the server side, treating client-side validation (HTML5 attributes, JavaScript) as user experience enhancements only, not security controls.

### Card Update Authorization

REQ-SEC-022: [Event-driven] When a card update is submitted, the system shall validate that all modified fields conform to their respective validation rules before persisting any changes to the database.

REQ-SEC-023: [Event-driven] When a card update is submitted, the system shall verify the user has authorization to modify the card and that the card belongs to an account the user is authorized to manage.

REQ-SEC-024: [Ubiquitous] The system shall implement an allowlist of modifiable card fields (embossed name, expiration date, active status) and shall reject attempts to modify other fields (card number, account ID, CVV) through update operations.

REQ-SEC-025: [Event-driven] When a card update operation is performed, the system shall log the change to an audit trail including: timestamp, user ID, card number (masked), fields modified, old values, and new values.

### Search and Filtering Security

REQ-SEC-026: [Event-driven] When a user performs a card search or filter operation, the system shall restrict results to only cards associated with accounts the authenticated user is authorized to access.

REQ-SEC-027: [Ubiquitous] The system shall implement pagination limits (maximum records per page) to prevent bulk data extraction through repeated page requests.

REQ-SEC-028: [Ubiquitous] The system shall log card search operations that return large result sets or use wildcard patterns to detect potential data harvesting attempts.

### CSRF Protection

REQ-SEC-029: [Ubiquitous] All card update operations (POST, PUT, DELETE requests) shall validate a CSRF token that is unique per user session and included in the request.

REQ-SEC-030: [Unwanted] If a card update request does not include a valid CSRF token matching the user's current session, the system shall reject the request with an error message and shall not process the update.

REQ-SEC-031: [Ubiquitous] The system shall generate a new CSRF token for each user session and include it in all forms that perform state-changing operations.

### Non-Functional Security Requirements

REQ-SEC-N-001: [Ubiquitous] The system shall comply with PCI-DSS requirements for cardholder data protection, including but not limited to: Requirements 3.2 (do not store sensitive authentication data after authorization), 3.4 (render PAN unreadable), and 3.5 (protect cryptographic keys).

REQ-SEC-N-002: [Ubiquitous] The system shall implement role-based access control (RBAC) for card management operations, distinguishing between: regular users (access own cards only), customer service representatives (access cards for assigned customers), and administrators (access all cards with audit logging).

REQ-SEC-N-003: [Ubiquitous] The system shall implement rate limiting on card search and retrieval operations to prevent automated bulk data extraction (e.g., maximum 100 card retrievals per user per hour).

### Open Questions

OQ-SEC-01: What is the organization's PCI-DSS compliance level and what specific cardholder data environment (CDE) requirements apply to this system? — Owner: Security compliance team

OQ-SEC-02: Should CVV codes be stored at all, or should the system follow PCI-DSS Requirement 3.2 strictly and never store CVV after authorization? — Owner: Payment operations team / Security team

OQ-SEC-03: What key management system (KMS) or hardware security module (HSM) should be used for encryption key storage and management? — Owner: Security architecture team

OQ-SEC-04: What is the required encryption key rotation schedule for cardholder data encryption keys? — Owner: Security team
