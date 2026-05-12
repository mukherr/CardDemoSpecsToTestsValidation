# Payment Authorization Management — Requirements

## 1. Global Preconditions

- User must be authenticated via the sign-on service (COSGN00C) before accessing any online authorization management screens.
- User must hold a card operations role to view authorization summaries, select transactions, or mark fraud status.
- A valid session context must be established and passed between online screens; session timeout terminates access.
- Batch processes (expired record purge, database load, database extract) require batch scheduling authorization and run under service credentials.


> **Note:** The legacy system implements some of this processing as scheduled batch jobs. The modernized system may implement these requirements as batch, event-driven, or real-time processing provided the requirements below are met.


### Navigation Context

- From the main menu, the card operations user selects the authorization summary function (transaction CPVS), which displays pending authorizations for a specified account.
- From the authorization summary screen, the user selects a specific transaction to navigate to the authorization detail screen (transaction CPVD), which displays full transaction details and provides fraud marking capability.
- The detail screen invokes shared subroutines (CBPAUP0C, COPAUS2C) for fraud status persistence and record updates; control returns to the detail screen upon completion.
- From either the summary or detail screen, the user may exit to return to the main menu.
- Batch extract and load processes operate independently of the online navigation flow and are initiated through batch scheduling.

- **OQ-NAV-01** *(Owner: Business Analyst)*: The exact menu hierarchy and whether CPVS is reachable from multiple menu paths is not documented—confirm the entry point(s) for the authorization summary screen.

---

## 2. Authorization Summary Display


As a card operations user, I want to view a summary of pending payment authorizations for a given account so that I can review authorization counts, balances, and individual transaction details for fraud investigation and tracking purposes.

### Requirements

REQ-F-001: [Event-driven] When the user enters an account ID on the authorization summary screen, the system shall validate that the account ID is not empty and contains only numeric characters, rejecting with the message "Please enter Acct Id..." if empty or blank, or "Acct Id must be Numeric ..." if non-numeric.

REQ-F-002: [Event-driven] When a valid account ID is provided, the system shall retrieve the card cross-reference record by account ID, extracting the card number and customer ID; if not found, the system shall display "Account: [ID] not found in XREF file"; if a system error occurs, the system shall display "Account: [ID] System error while reading XREF file".

REQ-F-003: [Event-driven] When the card cross-reference is successfully retrieved, the system shall retrieve the account record by account ID, extracting account status, current balance, credit limit, and cash credit limit; if not found, the system shall display "Account: [ID] not found in ACCT file"; if a system error occurs, the system shall display "Account: [ID] System error while reading ACCT file".

REQ-F-004: [Event-driven] When the card cross-reference is successfully retrieved, the system shall retrieve the customer record by customer ID, extracting customer name, address, and phone number; if not found, the system shall display "Customer: [ID] not found in CUST file"; if a system error occurs, the system shall display "Customer: [ID] System error while reading CUST file".

REQ-F-005: [Event-driven] When the account ID is provided for authorization summary lookup, the system shall retrieve the pending authorization summary from the authorization database by account ID; if a system error occurs, the system shall display "System error while reading AUTH Summary: Code: [code]".

REQ-F-006: [Event-driven] When an authorization summary is found, the system shall display the approved authorization count, declined authorization count, credit balance, cash balance, approved authorization amount, and declined authorization amount; if no summary is found, the system shall display zeros for all these fields.

REQ-F-007: [Ubiquitous] The system shall clear all five authorization transaction display fields to spaces before populating them with new authorization data.

REQ-F-008: [Event-driven] When authorization transactions are displayed, the system shall retrieve up to five authorization transactions per page from the authorization database; if a system error occurs during retrieval, the system shall display "System error while reading AUTH Details: Code: [code]" and stop processing.

REQ-F-009: [Ubiquitous] The system shall populate the screen header with the current date in MM/DD/YY format, current time in HH:MM:SS format, transaction identifier, program name, and title text.

REQ-F-010: [Event-driven] When the user presses PF8 (next page) and more authorization transactions are available beyond the current page, the system shall retrieve and display the next page of up to five authorization transactions.

REQ-F-011: [Event-driven] When the user presses PF7 (previous page) and the current page number is greater than 1, the system shall reposition to the previous page of authorization transactions and display them; if already at the first page, the system shall display "You are already at the top of the page...".

REQ-F-012: [Event-driven] When the user presses PF8 (next page) and no more authorization transactions are available, the system shall display "You are already at the bottom of the page...".

REQ-F-013: [Event-driven] When the user presses an unsupported key, the system shall display "Invalid key pressed. Please see below... " and remain on the authorization summary screen.

REQ-F-014: [Event-driven] When the program is re-entered with session context containing a numeric account ID, the system shall restore the account ID to the working area and display buffer; if the account ID in the session context is not numeric, the system shall clear the account ID.

REQ-F-015: [Event-driven] When the authorization database session cannot be established, the system shall display "System error while scheduling PSB: Code: [code]" and present the error on the authorization summary screen.

---

## 3. Authorization Transaction Selection and Detail Navigation


As a card operations user, I want to select a specific authorization transaction from the summary list so that I can view its full details for investigation or fraud marking.

### Requirements

REQ-F-016: [Event-driven] When the user presses Enter with a valid account ID, the system shall evaluate selection fields 1 through 5 in order; when a non-blank, non-null selection value is found, the system shall map it to the corresponding authorization key from the displayed list.

REQ-F-017: [Event-driven] When the user enters a selection code other than 'S' or 's' in a selection field, the system shall display "Invalid selection. Valid value is S".

REQ-F-018: [Event-driven] When the user selects an authorization transaction by entering 'S' or 's' in a selection field and both the selection flag and selected authorization key are populated, the system shall transfer control to the authorization detail program, passing the session context including the account ID, selected authorization key, originating transaction identifier, and originating program name, with the context marked as a new entry.

REQ-F-019: [Event-driven] When the user submits the authorization summary screen and no selection field contains a non-blank value, the system shall clear the selection flag and selected key and proceed without navigation.

---

## 4. Authorization Summary Exit and Navigation


As a card operations user, I want to exit the authorization summary screen so that I can return to the main menu or another appropriate entry point.

### Requirements

REQ-F-020: [Event-driven] When the user presses PF3 on the authorization summary screen, the system shall set the navigation destination to the menu program and initiate the transfer.

REQ-F-021: [Ubiquitous] The system shall record the current transaction identifier and program name as the navigation source in the session context and reset the program context to initial entry state before transferring control to the destination program.

REQ-F-022: [Unwanted] If the destination program is empty or contains only spaces at the time of navigation, the system shall default the destination to the sign-on program.

REQ-F-023: [Ubiquitous] The system shall transfer control to the destination program passing the complete session context including user identity, account information, and navigation history.

REQ-F-024: [Event-driven] When the program is invoked with session context data, the system shall extract and load the session context into working memory for use throughout the session.

---

## 5. Fraud Marking of Authorization Transactions


As a card operations user, I want to mark or unmark an authorization transaction as fraudulent so that the fraud status is recorded for risk management and investigation purposes.

### Requirements

REQ-F-025: [Event-driven] When the fraud marking process is initiated, the system shall retrieve the current system date and time, format the date as MM/DD/YY, and store it in the fraud report record as the fraud report date.

REQ-F-026: [Event-driven] When a fraud record is successfully added to the authorization fraud data store (legacy: CARDDEMO.AUTHFRDS), the system shall set the update status to success and record the message "ADD SUCCESS".

REQ-F-027: [Event-driven] When an attempt to add a fraud record fails due to a duplicate key (the authorization already exists in the fraud data store), the system shall invoke the fraud record update procedure to modify the existing record instead of creating a new one.

REQ-F-028: [Event-driven] When an existing fraud record is successfully updated in the authorization fraud data store, the system shall set the update status to success and record the message "UPDT SUCCESS".

REQ-F-029: [Unwanted] If a fraud record addition fails with a database error other than duplicate key, the system shall set the update status to failed and record an error message containing the database error code and state.

REQ-F-030: [Unwanted] If an existing fraud record update fails with a database error, the system shall set the update status to failed and record an error message containing the database error code and state.

---

## 6. Authorization Record Persistence


As a card operations system, I want authorization transaction details persisted to the fraud tracking data store so that a complete audit trail of authorization transactions and their fraud determinations is maintained.

### Requirements

REQ-F-031: [Event-driven] When an authorization transaction is received for persistence, the system shall extract the year, month, and day from the authorization date and store them as separate components.

REQ-F-032: [Ubiquitous] The system shall convert the numeric authorization time to hour, minute, second, and millisecond components by subtracting the authorization time value from 999999999 and extracting the individual time components from the resulting string.

REQ-F-033: [Ubiquitous] The system shall combine the date and time components into a formatted timestamp string in the format 'YY-MM-DD HH:MI:SS.SSS'.

REQ-F-034: [Ubiquitous] The system shall map all authorization transaction fields from the session context to the database record structure, including card number, card expiry date, authorization type, response code, reason code, ID code, transaction amount, approved amount, processing code, transaction ID, merchant ID, merchant name, merchant city, merchant state, merchant zip, merchant category code, acquirer country code, point-of-sale entry mode, match status, fraud indicator, and account/customer identifiers.

REQ-F-035: [Ubiquitous] The system shall insert the complete authorization transaction record into the authorization fraud data store with the timestamp formatted as 'YY-MM-DD HH24.MI.SSNNNNNN' and the fraud report date set to the current date.

---

## 7. Fraud Status Update on Existing Authorization Records


As a card operations system, I want to update the fraud status on an existing authorization record so that fraud determinations can be revised without creating duplicate records.

### Requirements

REQ-F-036: [Unwanted] If a duplicate key error is detected when inserting an authorization record into the fraud tracking data store, the system shall invoke the fraud update operation to modify the existing record.

REQ-F-037: [Ubiquitous] The system shall reconstruct the authorization timestamp by extracting year, month, and day from the original authorization date, computing the time by subtracting the authorization time value from 999999999, and extracting hour, minute, second, and millisecond components from the computed value.

REQ-F-038: [Ubiquitous] The system shall prepare the fraud record update by copying the card number, reconstructed authorization timestamp, and fraud action status into the database record buffer.

REQ-F-039: [Event-driven] When a duplicate authorization record is detected in the fraud tracking data store, the system shall update the authorization record by setting the fraud status to the fraud action value ('F' to mark as fraudulent or 'R' to remove the fraud flag) and recording the current date as the fraud report date, matching on card number and authorization timestamp.

### Open Questions

OQ-1: The authorization time conversion uses complement arithmetic (subtracting from 999999999). Is this a legacy IMS storage convention, and should the modernized system preserve this encoding or store time in a standard format? — Owner: IMS database team

OQ-2: The rules reference up to five authorization transactions displayed per page. Is this a business constraint or a legacy screen size limitation that could be relaxed in the modernized system? — Owner: business analyst / UX team

---

## 8. Authorization Transaction Detail View and Fraud Management


As a card operations user, I want to view detailed information about pending authorization transactions and toggle their fraud status so that I can investigate suspicious activity and manage fraud indicators on authorization records.

### Requirements

REQ-F-040: [Event-driven] When the user navigates to the authorization detail screen for the first time in a session, the system shall initialize the display state by clearing any prior error messages and preparing the screen for a fresh render.

REQ-F-041: [Event-driven] When the program receives a session context indicating first entry, the system shall process the current authorization selection and display the authorization detail screen.

REQ-F-042: [Event-driven] When the program receives a session context indicating re-entry, the system shall evaluate the function key pressed and route to the appropriate handler: Enter for validation and retrieval, PF5 for fraud toggle, PF8 for next-record pagination, or PF3 for return to the authorization summary screen.

REQ-F-043: [Event-driven] When the user presses an unrecognized function key, the system shall display an invalid key message and re-display the current screen without altering data.

REQ-F-044: [Event-driven] When the user presses Enter, the system shall validate that the account identifier is numeric and that an authorization transaction has been selected; only when both conditions are met shall the system proceed to retrieve the authorization record.

REQ-F-045: [Event-driven] When the user presses Enter and either the account identifier is not numeric or no authorization transaction is selected, the system shall re-display the screen without performing a retrieval.

REQ-F-046: [Event-driven] When the system needs to retrieve an authorization record by key, the system shall query the authorization data store using the provided key; if the record is found, the system shall load the summary record and then retrieve the corresponding detailed authorization record.

REQ-F-047: [Event-driven] When the authorization record retrieval returns not-found or end-of-collection, the system shall set an end-of-data indicator and not load a record.

REQ-F-048: [Unwanted] If the authorization record retrieval encounters a system error, the system shall construct an error message containing the error code and display it on the screen.

REQ-F-049: [Event-driven] When the user presses PF8 to navigate to the next authorization record, the system shall retrieve the current authorization record and attempt to read the next record in sequence; if a next record exists, the system shall display it.

REQ-F-050: [Event-driven] When the user presses PF8 and no next authorization record exists, the system shall display an end-of-collection message without clearing the current screen content.

REQ-F-051: [Event-driven] When the user presses PF5 to toggle fraud status, the system shall retrieve the selected authorization record and evaluate its current fraud status: if the record is already marked as fraud-confirmed, the system shall remove the fraud flag; if the record is not marked as fraud, the system shall mark it as fraud-confirmed.

REQ-F-052: [Event-driven] When the fraud status has been toggled, the system shall assemble the authorization record data along with the account identifier and customer identifier, then invoke the external fraud processing service.

REQ-F-053: [Complex] While the external fraud processing service has completed execution, when the service reports successful completion and the fraud update succeeded, the system shall persist the updated authorization record with the toggled fraud status back to the authorization data store.

REQ-F-054: [Unwanted] If the external fraud processing service fails or reports that the fraud update did not succeed, the system shall display the error message returned by the external service and shall not persist changes to the authorization record.

REQ-F-055: [Event-driven] When the authorization record is successfully updated with the fraud flag removed, the system shall display a fraud removal confirmation message.

REQ-F-056: [Event-driven] When the authorization record is successfully updated with the fraud flag set, the system shall display a fraud marking confirmation message.

REQ-F-057: [Unwanted] If the authorization record update encounters a system error, the system shall construct an error message containing the error code indicating a fraud tagging error and display it on the screen.

REQ-F-058: [Event-driven] When the user presses PF3, the system shall transfer control to the authorization summary screen, passing the session context including the current program as the originating program, the current transaction identifier, and a re-entry indicator.

REQ-F-059: [Event-driven] When the authorization detail program is re-invoked with a pre-existing session context, the system shall restore the session context from the incoming data and clear any fraud-related data from the previous interaction.

REQ-F-060: [Event-driven] When the authorization detail program is invoked with no pre-existing session context, the system shall initialize the session context and transfer control to the authorization summary screen.

### Non-Functional Requirements

REQ-N-001: [Unwanted] If the database connection scheduling fails, the system shall construct an error message containing the scheduling error code and display it on the screen without proceeding to data retrieval or update operations.

### Open Questions

OQ-3: The external fraud processing service is invoked during the fraud toggle operation. What are the specific business rules and validations performed by this external service, and what constitutes a "successful" vs. "failed" fraud update from its perspective? — Owner: Fraud operations team

OQ-4: The fraud toggle uses a binary flag (fraud-confirmed or not). Are there additional fraud status states (e.g., under investigation, confirmed by multiple reviewers) that should be supported in the modernized system? — Owner: Fraud operations team

---

## 9. Expired Authorization Record Purge


As a batch operations team, I want expired pending authorization records automatically removed from the authorization database so that stale data does not accumulate, storage is optimized, and only current authorization information is retained for fraud detection and tracking.

**Restart/Recovery:** The process iterates through all authorization summary and detail records; if interrupted, partial deletions may exist with no automatic rollback of already-deleted records.

### Requirements

REQ-F-061: [Event-driven] When the expired authorization purge process starts, the system shall retrieve the current system date and accept the expiry threshold from input configuration; if the expiry threshold is not a valid numeric value, the system shall default to 5 days.

REQ-F-062: [Ubiquitous] The system shall calculate the age of each authorization detail record by converting the stored authorization date to a day-of-year value and subtracting it from the current day-of-year.

REQ-F-063: [Event-driven] When the calculated age of an authorization detail record is greater than or equal to the configured expiry threshold, the system shall mark the record as eligible for deletion.

REQ-F-064: [Event-driven] When the calculated age of an authorization detail record is less than the configured expiry threshold, the system shall retain the record without modification.

REQ-F-065: [Event-driven] When an authorization detail record is marked as eligible for deletion and the authorization was approved, the system shall decrement the parent summary's approved authorization count by 1 and subtract the approved amount from the parent summary's approved total.

REQ-F-066: [Event-driven] When an authorization detail record is marked as eligible for deletion and the authorization was declined, the system shall decrement the parent summary's declined authorization count by 1 and subtract the transaction amount from the parent summary's declined total.

REQ-F-067: [Event-driven] When an authorization detail record is determined eligible for deletion, the system shall delete that detail record from the authorization database.

REQ-F-068: [Unwanted] If both the approved authorization count and the declined authorization count on a summary record are zero or less after all associated detail records have been evaluated, the system shall delete the authorization summary record from the authorization database.

REQ-F-069: [State-driven] While no error has occurred and the authorization database contains more summary records, the system shall continue retrieving the next authorization summary and processing all its associated detail records for expiry evaluation and deletion.

REQ-F-070: [Event-driven] When the end of the authorization database is reached with no more summary records available, the system shall terminate processing and commit all changes.

### Non-Functional Requirements

REQ-N-002: [Ubiquitous] The system shall commit all deletion changes upon successful completion of the purge process as a single unit of work.

### Open Questions

OQ-5: The age calculation uses day-of-year subtraction, which does not account for year boundaries (e.g., an authorization from December 30 evaluated on January 2 would yield a negative age). Should the modernized system use a calendar-aware date difference calculation? — Owner: domain expert / business analyst

OQ-6: The expiry threshold default of 5 days is applied when the input parameter is non-numeric. Should the modernized system reject invalid configuration and fail rather than silently defaulting? — Owner: operations team

---

## 10. Payment Authorization Database Loading


As a batch operations team, I want pending payment authorization data loaded from input files into the authorization database so that authorization summary and detail records are persistently stored in a hierarchical structure for downstream retrieval and processing.

### Requirements

REQ-F-071: [Event-driven] When the authorization database load job executes, the system shall open the authorization summary input file (legacy: INFILE1) and the authorization detail input file (legacy: INFILE2) for sequential reading.

REQ-F-072: [State-driven] While root authorization summary records remain available in the authorization summary input file, the system shall read each record and store it as a root-level segment in the authorization database (legacy: OEM.IMS.IMSP.PAUTHDB).

REQ-F-073: [Unwanted] If end-of-file is reached on the authorization summary input file, the system shall stop reading root records and proceed to child record processing.

REQ-F-074: [Unwanted] If a read error occurs on the authorization summary input file, the system shall skip the errored record and continue processing subsequent records.

REQ-F-075: [State-driven] While child authorization detail records remain available in the authorization detail input file, the system shall read each record, validate that the root segment key is numeric, and associate the detail record with its parent summary record in the authorization database.

REQ-F-076: [Event-driven] When a child authorization detail record is successfully read and the root segment key is numeric, the system shall retrieve the corresponding root authorization summary segment from the authorization database using the root segment key, then insert the detail record as a child segment under that root segment.

REQ-F-077: [Unwanted] If the root segment key in a child authorization detail record is not numeric, the system shall skip that record and continue processing.

REQ-F-078: [Unwanted] If end-of-file is reached on the authorization detail input file, the system shall stop reading child records and complete the load operation.

REQ-F-079: [Unwanted] If a read error occurs on the authorization detail input file, the system shall skip the errored record and continue processing subsequent records.

REQ-F-080: [Ubiquitous] The system shall maintain the parent-child hierarchical relationship between authorization summary records (root segments) and authorization detail records (child segments) such that each detail record is stored under its corresponding summary record identified by the root segment key.

### Non-Functional Requirements

REQ-N-003: [Ubiquitous] The system shall write load operation results, processing diagnostics, and any database errors to an operational log for post-execution review.

### Open Questions

OQ-7: What constitutes the root segment key used to associate child records with parent records, and what is its format/length beyond the numeric validation? — Owner: IMS database team

OQ-8: What is the expected behavior when a child record references a root segment key that was not successfully inserted (e.g., due to a prior skip)? Should the child record be rejected or should the system attempt insertion regardless? — Owner: Payment authorization domain expert

---

## 11. Authorization Data Extraction


As a batch operations team, I want pending authorization summaries and their associated authorization details extracted from the IMS authorization database and written to separate sequential output files so that the data is available for backup, archival, data migration, or downstream reporting purposes.

**Restart/Recovery:** The extraction reads the source authorization database without modifying it (read-only access). Output files are produced fresh on each execution; re-running the job regenerates the output completely.

### Requirements

REQ-F-081: [Event-driven] When the authorization data extraction job executes, the system shall read all pending authorization summary records from the IMS authorization database (legacy: OEM.IMS.IMSP.PAUTHDB) and write each valid summary to the paut root file (legacy: AWS.M2.CARDDEMO.PAUTDB.ROOT.GSAM).

REQ-F-082: [Event-driven] When an authorization summary record is successfully retrieved, the system shall validate that the account identifier is numeric before writing the record to the paut root file.

REQ-F-083: [State-driven] While parent authorization summary records remain available, the system shall continue retrieving and processing each summary and its associated child detail records.

REQ-F-084: [State-driven] While child authorization detail records remain available for the current authorization summary, the system shall retrieve each detail record and write it to the paut child file (legacy: AWS.M2.CARDDEMO.PAUTDB.CHILD.GSAM).

REQ-F-085: [Event-driven] When an authorization detail record is successfully retrieved, the system shall write the authorization detail record to the paut child file.

REQ-F-086: [Unwanted] If no more parent authorization summary records are available in the source database, the system shall terminate the main processing loop.

REQ-F-087: [Unwanted] If no more child authorization detail records are available for the current authorization summary, the system shall terminate child record iteration and proceed to the next parent authorization summary.

REQ-F-088: [Ubiquitous] The system shall produce two distinct output files: one containing authorization summary records (root segments with account identifier, customer identifier, authorization status, credit limit, cash limit, balances, and transaction counts) and one containing authorization detail records (child segments with authorization date, time, card number, merchant details, and authorization response codes).

REQ-F-089: [Ubiquitous] The system shall preserve the hierarchical relationship between authorization summaries and their associated details by processing all child detail records for a given summary before advancing to the next summary.

### Non-Functional Requirements

REQ-N-004: [Ubiquitous] The system shall access the source IMS authorization database in read-only mode, preserving the original data after extraction completion.

REQ-N-005: [Ubiquitous] The extraction process shall be idempotent — re-executing the job shall produce identical output files given the same source database state.

### Open Questions

OQ-9: What should happen when an authorization summary record has a non-numeric account identifier? The current behavior appears to skip the record silently. Should these records be logged or written to a rejection report? — Owner: Payment Authorization domain expert

OQ-10: The source database is identified as "DLIGSAMP" in the job parameters. Is this a test/sample database name, or does it represent the production authorization database? — Owner: IMS database administration team

---

## 12. Payment Authorization Database Extract


As a batch operations team, I want payment authorization data extracted from the authorization database into sequential files so that authorization summary and detail records are available for downstream analysis, backup, migration, and fraud detection outside the hierarchical database environment.

### Requirements

REQ-F-090: [Event-driven] When the authorization database extract job executes, the system shall read all authorization summary records from the payment authorization database (legacy: OEM.IMS.IMSP.PAUTHDB) and write each valid summary record to the paut root file (legacy: AWS.M2.CARDDEMO.PAUTDB.ROOT.FILEO).

REQ-F-091: [Ubiquitous] The system shall validate that the account identifier in each authorization summary record is numeric before writing the summary record to the output; records with non-numeric account identifiers shall be skipped.

REQ-F-092: [Event-driven] When a valid authorization summary record is written, the system shall retrieve all associated authorization detail records for that summary and write each detail record to the paut child file (legacy: AWS.M2.CARDDEMO.PAUTDB.CHILD.FILEO).

REQ-F-093: [State-driven] While authorization summary records remain in the database, the system shall continue retrieving and processing the next summary record and its associated detail records until no more summary records exist.

REQ-F-094: [State-driven] While authorization detail records remain for the current summary, the system shall continue retrieving and writing detail records until no more detail records exist for that summary, then proceed to the next summary.

REQ-F-095: [Ubiquitous] The system shall produce the authorization summary output as fixed-length records of 100 bytes per record.

REQ-F-096: [Ubiquitous] The system shall produce the authorization detail output as fixed-length records of 206 bytes per record.

REQ-F-097: [Ubiquitous] The system shall access the payment authorization database in shared read-only mode to ensure the database remains available to other processes during extraction.

REQ-F-098: [Event-driven] When the database signals that no more authorization summary records exist, the system shall terminate the extraction process.

REQ-F-099: [Event-driven] When the database signals that no more authorization detail records exist for the current summary, the system shall stop detail retrieval for that summary and proceed to the next summary record.

### Open Questions

OQ-11: The not-applicable rules reference validation of the account identifier and initialization of detail processing state. Should records with non-numeric account identifiers be logged or reported for investigation, or is silent skipping sufficient? — Owner: Payment Authorization domain expert

OQ-12: Are there any ordering requirements for the output records (e.g., must summaries appear in account identifier order, must details appear in chronological order)? — Owner: Downstream consumers of the extract files

---



## Shared Capability Dependencies

This capability depends on the following shared capabilities.
Do not reimplement their behavior — integrate with the shared service.

- **COSGN00C** (`_shared/COSGN00C/`) — As a user of the CardDemo application, I want to sign on with my credentials so that I am authenticated and directed to the appropriate application functions based on my role.
