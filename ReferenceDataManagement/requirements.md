# Reference Data Management — Requirements

## 1. Global Preconditions

- User must be authenticated via the sign-on process before accessing any online reference data management screens.
- Only users with a system administrator role may view, search, create, update, or delete transaction type reference data.
- A valid session context must be established and passed between screens during online navigation.
- Batch reference data operations (initialization, maintenance, extraction, backup) require appropriate batch scheduling authorization.


> **Note:** The legacy system implements some of this processing as scheduled batch jobs. The modernized system may implement these requirements as batch, event-driven, or real-time processing provided the requirements below are met.


### Navigation Context

- From the main application menu, the administrator selects the reference data management option, which transfers control to the transaction type list screen.
- The transaction type list screen supports paginated browsing, searching, and selection of individual records for update or deletion.
- Selecting a record or choosing to add a new record transfers control to the transaction type detail screen for single-record maintenance (create, update, delete).
- Upon completing or cancelling a detail operation, control returns to the transaction type list screen.
- Batch processes (database initialization, data store refresh, extraction, backup) operate independently of the online navigation flow and are triggered via batch scheduling.

- **OQ-1**: Is there an online screen or menu path for managing transaction category or disclosure group reference data, or is maintenance of those data stores exclusively batch-driven? *Owner: Business/Product team*

---

## 2. Transaction Type List Management


As a system administrator, I want to view, search, update, and delete transaction type records from a paginated list so that I can maintain the reference data that classifies card transactions throughout the application.

### Requirements

**Session and Navigation**

REQ-F-001: [Event-driven] When the user enters the transaction type list function from the main menu, from a different program, or returns from the add transaction type function via PF3, the system shall clear all prior search criteria, row selections, and pagination state, and reset to the initial entry state displaying the first page of results.

REQ-F-002: [Event-driven] When the program is re-entered with existing session context from a prior invocation, the system shall restore the previously saved session state including search filters, pagination position, and any pending actions.

REQ-F-003: [Event-driven] When the user presses PF3 to exit and the originating program is recorded in the navigation history, the system shall transfer control to the originating program, passing the session context including the current transaction identifier, current program identifier, user type, program state, and last-visited screen identifiers.

REQ-F-004: [Event-driven] When the user presses PF3 to exit and no originating program is recorded (or the origin is the current program itself), the system shall transfer control to the administration menu.

REQ-F-005: [Event-driven] When the user presses PF2 (Add), the system shall transfer control to the add transaction type function, passing the session context including the originating transaction identifier, originating program identifier, user type, and current screen identifiers for return navigation.

**Input Validation**

REQ-F-006: [Ubiquitous] The system shall validate the transaction type code filter: if blank or zero, treat as no filter (wildcard); if non-numeric, reject as invalid; if numeric, accept as a valid filter criterion.

REQ-F-007: [Ubiquitous] The system shall validate the transaction type description filter: if blank or spaces, treat as no filter (wildcard); otherwise, accept as a valid filter criterion for pattern matching.

REQ-F-008: [Ubiquitous] The system shall validate each row's selection flag, accepting only 'D' (delete), 'U' (update), or blank; if any row contains an invalid action code, the system shall reject the input as invalid.

REQ-F-009: [Ubiquitous] The system shall enforce that at most one action (one 'D' or one 'U' selection across all displayed rows) is selected per submission; if more than one action is selected, the system shall reject the input as invalid.

REQ-F-010: [Ubiquitous] The system shall validate that the function key pressed is one of Enter, PF2, PF3, PF7, PF8, or PF10 (only when a delete or update action is pending); if an invalid key is pressed, the system shall treat it as Enter.

**Search and Data Retrieval**

REQ-F-011: [Event-driven] When the user submits search criteria, the system shall query the transaction type reference table (legacy: CARDDEMO.TRANSACTION_TYPE) to count records matching the supplied transaction type code and/or description filters.

REQ-F-012: [Event-driven] When the count query returns zero matching records, the system shall reject the search as invalid and display a no-records-found error message.

REQ-F-013: [Event-driven] When the user presses Enter with valid filters and no pending action, the system shall retrieve the first page of transaction type records (up to 7 rows) matching the search criteria and display them on screen.

**Pagination**

REQ-F-014: [Event-driven] When the user presses PF8 (page down) and more pages are available, the system shall increment the page number, retrieve the next page of matching records, clear all row selections, and display the results.

REQ-F-015: [Event-driven] When the user presses PF8 (page down) and no more pages are available, the system shall redisplay the current page with a message indicating the last page has been reached.

REQ-F-016: [Event-driven] When the user presses PF7 (page up) while on the first page, the system shall redisplay the first page with a message indicating no previous pages exist.

REQ-F-017: [Event-driven] When the user presses PF7 (page up) while on a page other than the first, the system shall decrement the page number, retrieve the previous page of matching records, clear all row selections, and display the results.

REQ-F-018: [Ubiquitous] The system shall reset the last-page indicator when the user performs any action other than PF8 (page down).

**Update Processing**

REQ-F-019: [Event-driven] When the user presses Enter with a 'U' selection on one row, the system shall redisplay the screen with the update selection preserved and the description field enabled for editing, prompting the user to confirm with PF10.

REQ-F-020: [Event-driven] When the user presses PF10 to confirm an update and the search criteria and row selections have not changed since the action was selected, the system shall execute an update against the transaction type reference table to persist the new description for the selected transaction type code.

REQ-F-021: [Event-driven] When the update succeeds, the system shall commit the change, mark the update as succeeded, retrieve the first page of data, and redisplay the screen with a success message.

REQ-F-022: [Unwanted] If the update fails because the record is not found, the system shall display an error message indicating the record was deleted by another user.

REQ-F-023: [Unwanted] If the update fails due to a deadlock, the system shall display an error message indicating another user is updating the same record and mark the input as invalid.

REQ-F-024: [Unwanted] If the update fails for any other database error, the system shall format and display the database error details.

**Delete Processing**

REQ-F-025: [Event-driven] When the user presses Enter with a 'D' selection on one row, the system shall redisplay the screen with the delete selection preserved, prompting the user to confirm with PF10.

REQ-F-026: [Event-driven] When the user presses PF10 to confirm a delete and the search criteria and row selections have not changed since the action was selected, the system shall execute a delete against the transaction type reference table for the selected transaction type code.

REQ-F-027: [Event-driven] When the delete succeeds, the system shall commit the change, mark the delete as succeeded, retrieve the first page of data, and redisplay the screen with a success message.

REQ-F-028: [Unwanted] If the delete fails due to a referential integrity constraint violation, the system shall display an error message indicating the record cannot be deleted because it has dependent records.

REQ-F-029: [Unwanted] If the delete fails because the record is not found, the system shall display an error message indicating the record was already deleted by another user.

REQ-F-030: [Unwanted] If the delete fails for any other database error, the system shall format and display the database error details.

**Confirmation Guard**

REQ-F-031: [Unwanted] If the user presses PF10 to confirm a delete or update but has changed the search criteria or row selections since the action was marked, the system shall treat the PF10 press as an Enter key press to force re-execution of the search with the current criteria, preventing confirmation of a stale action.

**Screen Population**

REQ-F-032: [Ubiquitous] The system shall populate each displayed row with the transaction type code, selection flag, and description; if the user has modified the description for a pending update, the system shall display the modified description with an asterisk indicator.

**Database Connectivity**

REQ-F-033: [Unwanted] If the database is not accessible during the initial connectivity check, the system shall display an error message indicating database access failure and prevent further processing.

**Error Message Formatting**

REQ-F-034: [Event-driven] When a database operation fails, the system shall format the error details (operation context, SQL code, and error description) into a human-readable message and display it to the user.

**Default Action**

REQ-F-035: [Event-driven] When the user performs an action that does not match any specific business logic condition, the system shall retrieve the first page of data and redisplay the screen.

### Non-Functional Requirements

REQ-N-001: [Unwanted] If a delete or update operation succeeds, the system shall commit the database transaction before redisplaying the screen to ensure data persistence.

REQ-N-002: [Unwanted] If a deadlock is detected during an update operation, the system shall not commit and shall allow the user to retry the operation.

### Open Questions

OQ-1: The screen displays up to 7 rows per page. Is this page size a fixed business requirement or should it be configurable in the modernized system? — Owner: business analyst

OQ-2: The system maps PF13–PF24 to their PF1–PF12 equivalents. Should the modernized system preserve this extended key mapping behavior? — Owner: UX team

---

## 3. Transaction Type Maintenance


As a system administrator, I want to search for, view, create, update, and delete transaction type reference data so that the card processing system has accurate and current transaction classifications.

### Requirements

**Navigation and Function Key Handling**

REQ-F-036: [Event-driven] When the user presses a key on the terminal, the system shall map the key code to a standardized function key identifier (Enter, Clear, PA1, PA2, PF1 through PF12), remapping extended function keys (PF13–PF24) to their base equivalents (PF1–PF12).

REQ-F-037: [Complex] While the function key has been identified, when the system evaluates whether the key press is valid for the current application state, the system shall reject the key press with an error message if it is not valid for the current state and no prior error message exists.

REQ-F-038: [Complex] When the system evaluates the combination of the key pressed and the current state, the system shall route to the appropriate action: PF3 exits the application; PF4 with details shown initiates delete confirmation; PF4 with delete confirmation pending proceeds with delete; PF5 with record not found initiates new record creation; PF5 with changes pending confirmation saves changes; PF12 with changes or delete pending cancels the operation; all other valid combinations process user input from the screen.

REQ-F-039: [Event-driven] When the user presses PF3 (exit), the system shall transfer control to the calling program (or to the administration program if no calling program is recorded), passing the updated session context including the current program as the origin point and user type set to admin.

**Function Key Availability**

REQ-F-040: [State-driven] While the application is in a given state, the system shall configure function key availability as follows: Enter is disabled when delete confirmation is pending; F4 (Delete) is enabled when details are shown or delete confirmation is pending; F5 (Save/Create) is enabled when changes are pending confirmation or a new record is being created; F12 (Cancel) is enabled when changes are pending confirmation, details are shown, a new record is being created, or delete confirmation is pending; all other function keys are disabled.

**Input Handling**

REQ-F-041: [Event-driven] When the user submits the screen, the system shall receive the transaction type code and description fields, trim whitespace, and convert placeholder values (asterisk or spaces) to empty indicators to mark fields as not entered.

**Validation**

REQ-F-042: [Event-driven] When the system validates user input, the system shall require the transaction type code to be numeric, non-zero, and exactly 2 digits, and shall require the description to contain only alphabetic characters and spaces, be non-empty, and be up to 50 characters in length.

REQ-F-043: [Event-driven] When the user is searching with the same transaction type code that was previously not found, the system shall skip re-validation of the code and proceed with the search.

REQ-F-044: [Event-driven] When the user is creating a new record or confirming changes, the system shall skip search key validation but still validate the description field.

REQ-F-045: [Unwanted] If validation fails for the transaction type code or description, the system shall set error flags, generate a field-specific error message, and highlight the invalid field.

**Search and Retrieval**

REQ-F-046: [Event-driven] When the user enters a valid transaction type code and requests a search, the system shall query the transaction type table (legacy: CARDDEMO.TRANSACTION_TYPE) to retrieve the matching record.

REQ-F-047: [Event-driven] When a transaction type record is successfully retrieved, the system shall store the retrieved transaction type code and description in an original-values area for later change comparison, and mark the search as successful.

REQ-F-048: [Event-driven] When no matching transaction type record is found, the system shall mark the search as unsuccessful and inform the user that the record does not exist.

REQ-F-049: [Unwanted] If a database error occurs during retrieval, the system shall mark the search as failed and display an error message.

**Change Detection**

REQ-F-050: [Event-driven] When the user has retrieved an existing transaction type record and is editing it, the system shall compare the original transaction type code and description with the user-entered values using case-insensitive comparison accounting for whitespace differences, and mark whether changes have been detected.

**Update**

REQ-F-051: [Event-driven] When the user confirms changes to a transaction type record, the system shall trim the description of trailing spaces and attempt to update the existing record in the transaction type table using the transaction type code as the key.

REQ-F-052: [Event-driven] When the update succeeds, the system shall commit the transaction and mark the operation as successful.

REQ-F-053: [Unwanted] If the update finds no matching record, the system shall attempt to insert a new transaction type record with the provided code and description.

REQ-F-054: [Unwanted] If a lock conflict occurs during the update, the system shall mark the operation as failed due to lock contention and display an appropriate error message.

REQ-F-055: [Unwanted] If any other database error occurs during the update, the system shall mark the operation as failed and generate an error message.

**Create**

REQ-F-056: [Event-driven] When the user creates a new transaction type record and confirms the creation, the system shall insert the record into the transaction type table with the provided transaction type code and description.

REQ-F-057: [Event-driven] When the insert succeeds, the system shall commit the transaction.

REQ-F-058: [Unwanted] If any database error occurs during the insert, the system shall mark the insert as failed and generate an error message.

**Delete**

REQ-F-059: [Event-driven] When the user confirms deletion of a transaction type record, the system shall delete the record from the transaction type table where the transaction type code matches the selected value.

REQ-F-060: [Event-driven] When the deletion succeeds, the system shall commit the transaction and mark the deletion as successful.

REQ-F-061: [Unwanted] If a referential integrity constraint is violated during deletion (indicating child records exist), the system shall display an error message instructing the user to delete dependent records first.

REQ-F-062: [Unwanted] If any other database error occurs during deletion, the system shall mark the deletion as failed and generate an error message.

### Non-Functional Requirements

REQ-N-003: [Unwanted] If a lock conflict occurs when updating a transaction type record, the system shall reject the write and inform the user of the contention rather than waiting indefinitely.

REQ-N-004: [Ubiquitous] The system shall commit each successful create, update, or delete operation as an atomic transaction so that partial changes are not persisted.

### Open Questions

OQ-3: The validation rule states the description must be "alphabetic and spaces only." Should this constraint be relaxed to allow digits, hyphens, or other characters in transaction type descriptions for the modernized system? — Owner: Business operations team

OQ-4: The referential integrity constraint on deletion references "child records" — which specific data stores hold these dependent records (e.g., transaction records, transaction category records)? — Owner: Data architecture team

---

## 4. Reference Data Database Initialization


As a reference data management team, I want transaction type and transaction category reference data loaded into the application database so that card processing operations can classify and label transactions using standardized codes.

### Requirements

REQ-F-063: [Event-driven] When the reference data initialization process executes, the system shall load transaction type definitions into the transaction type table (legacy: CARDDEMO.TRANSACTION_TYPE), making all defined transaction type codes and their descriptions available for use by the application.

REQ-F-064: [Event-driven] When transaction type definitions have been successfully loaded, the system shall catalog the transaction type table so that it is accessible for subsequent query and lookup operations throughout the application.

REQ-F-065: [Event-driven] When the reference data initialization process executes, the system shall load transaction type category definitions into the transaction type category store (legacy: CARDDEMO.TRANSACTION_TYPE_CATEGORY:CARDSTTC:CARDDEMO), establishing the higher-level business categories used to group transaction types.

REQ-F-066: [Unwanted] If the transaction type load fails, the system shall not proceed with cataloging the transaction type table.

### Non-Functional Requirements

REQ-N-005: [Ubiquitous] The reference data initialization process shall be idempotent — re-execution shall produce the same final state in the transaction type table and transaction type category store without creating duplicates or leaving partial data.

### Open Questions

OQ-5: Are there specific transaction type codes and category codes that must be present after initialization (i.e., a defined seed data set), or is the content entirely driven by the input control definitions? — Owner: Reference Data domain expert

OQ-6: The job includes a "free plan" operation that releases database resources. Is this a prerequisite cleanup step that must always precede data loading, or is it an independent maintenance activity? — Owner: Database administration team

---

## 5. Reference Data Versioned Backup


As a batch operations team, I want versioned backups of transaction type, transaction category, and disclosure group reference data maintained automatically so that recent versions are available for recovery while storage remains bounded.

### Requirements

REQ-F-067: [Event-driven] When the reference data backup job executes, the system shall validate the transaction type file (legacy: AWS.M2.CARDDEMO.TRANTYPE.PS) structure and content before creating a backup, and shall skip the backup if validation fails.

REQ-F-068: [Event-driven] When transaction type validation succeeds, the system shall create a new versioned backup of the transaction type file in the transaction type backup store (legacy: AWS.M2.CARDDEMO.TRANTYPE.BKUP), retaining a maximum of 5 versions and automatically deleting the oldest version when the limit is exceeded.

REQ-F-069: [Event-driven] When the transaction type backup completes successfully, the system shall create a new versioned backup of the transaction category file (legacy: AWS.M2.CARDDEMO.TRANCATG.PS) in the transaction category backup store (legacy: AWS.M2.CARDDEMO.TRANCATG.PS.BKUP), retaining a maximum of 5 versions and automatically deleting the oldest version when the limit is exceeded.

REQ-F-070: [Event-driven] When the disclosure group data preparation step succeeds, the system shall create a new versioned backup of the discount group file (legacy: AWS.M2.CARDDEMO.DISCGRP.PS) in the discount group backup store (legacy: AWS.M2.CARDDEMO.DISCGRP.BKUP), retaining a maximum of 5 versions and automatically deleting the oldest version when the limit is exceeded.

REQ-F-071: [Unwanted] If the disclosure group data preparation step fails, the system shall skip the disclosure group backup to prevent archiving potentially corrupt data.

REQ-F-072: [Ubiquitous] The system shall maintain versioned backup infrastructure for each of the three reference data stores (transaction type backup store, transaction category backup store, and discount group backup store), each configured with a 5-version retention limit.

REQ-F-073: [Ubiquitous] The system shall execute the backup sequence in dependency order: transaction type backup must succeed before transaction category backup proceeds; disclosure group backup operates independently of the transaction type/category sequence.

### Non-Functional Requirements

REQ-N-006: [Unwanted] If any validation or preparation step fails, the system shall prevent all dependent downstream backup steps from executing to ensure only validated data is archived.

### Open Questions

OQ-7: The validation step for transaction type data is described generically ("validate structure and content"). What specific validation criteria must be met before backup proceeds? — Owner: Reference Data domain expert

OQ-8: The disclosure group backup has a separate "preparation" step before backup. What preparation or validation logic is performed, and does it differ from the transaction type validation? — Owner: Reference Data domain expert

---

## 6. Disclosure Group Data Store Initialization


As a reference data operations team, I want the disclosure group master data store initialized from a sequential source file so that card processing applications can perform keyed lookups against current disclosure group reference data.

**Category:** setup
**Purpose:** Loads disclosure group reference data from a sequential source into an indexed data store, replacing any prior version to ensure a clean, consistent state.
**Migration relevance:** Defines the initial/refreshed state of the disclosure group keyed data store (legacy: AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS). The data exchange pattern (source format, key structure, record layout) must be preserved; the storage mechanism is implementation-specific.

### Requirements

REQ-F-074: [Event-driven] When the disclosure group initialization process executes, the system shall replace the contents of the discount group keyed data store (legacy: AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS) with all disclosure group records from the discount group file (legacy: AWS.M2.CARDDEMO.DISCGRP.PS), making the data available for keyed lookups.

REQ-F-075: [Ubiquitous] The system shall store disclosure group records as fixed 50-byte records indexed by a 16-byte key located at the start of each record.

### Open Questions

OQ-9: Is the disclosure group initialization intended to run on a recurring schedule (e.g., daily refresh) or only as a one-time setup/migration activity? — Owner: reference data operations team

OQ-10: Are there downstream systems or processes that depend on the disclosure group data store being available without interruption during refresh, requiring a swap-in strategy rather than delete-and-reload? — Owner: reference data operations team

---

## 7. Transaction Type Reference Data Maintenance


As a reference data administrator, I want transaction type records added, updated, or deleted in the transaction type master table based on maintenance requests so that the transaction type reference data remains current and accurate for card processing operations.

### Requirements

REQ-F-076: [Event-driven] When the transaction type maintenance process executes, the system shall read all maintenance request records sequentially from the input file and apply each request to the transaction type table (legacy: CARDDEMO.TRANSACTION_TYPE) based on the operation type indicator in each record.

REQ-F-077: [Event-driven] When a maintenance request record with operation type 'A' (add) is encountered, the system shall insert a new row into the transaction type table with the transaction type code and transaction type description extracted from the input record.

REQ-F-078: [Event-driven] When a maintenance request record with operation type 'U' (update) is encountered, the system shall update the transaction type description in the transaction type table for the row matching the transaction type identifier from the input record.

REQ-F-079: [Unwanted] If an update operation targets a transaction type identifier that does not exist in the transaction type table, the system shall treat the operation as failed.

REQ-F-080: [Event-driven] When a maintenance request record with operation type 'D' (delete) is encountered, the system shall delete the row from the transaction type table where the transaction type code matches the identifier in the input record.

REQ-F-081: [State-driven] While records remain in the input file, the system shall continue reading and processing the next maintenance request record until all records have been processed.

### Open Questions

OQ-11: What should happen when an 'A' (add) operation targets a transaction type code that already exists in the table — should it be rejected, or should it overwrite the existing record? — Owner: Reference Data domain expert

OQ-12: What should happen when a 'D' (delete) operation targets a transaction type code that does not exist in the table — should it be silently ignored or reported as an error? — Owner: Reference Data domain expert

OQ-13: Are there any operation type values other than 'A', 'U', and 'D' that may appear in the input file, and if so, how should they be handled? — Owner: Reference Data domain expert

---

## 8. Transaction Category Data Refresh


As a reference data operations team, I want the transaction category keyed data store refreshed with current transaction category reference data so that card processing operations can correctly categorize transactions using up-to-date category codes.

### Requirements

REQ-F-082: [Event-driven] When the transaction category data refresh executes, the system shall replace the entire contents of the transaction category keyed data store (legacy: AWS.M2.CARDDEMO.TRANCATG.VSAM.KSDS) with the current transaction category records from the transaction category file (legacy: AWS.M2.CARDDEMO.TRANCATG.PS), producing a fully populated indexed data store keyed on a 6-character transaction category code.

REQ-F-083: [Ubiquitous] The system shall ensure that the transaction category keyed data store is in a consistent, complete state after the refresh — containing exactly the set of records present in the source transaction category file, with no residual data from any prior version.

### Non-Functional Requirements

REQ-N-007: [Unwanted] If the refresh process is interrupted before completion, the system shall not leave a partially populated transaction category keyed data store available for use by other processes.

---

## 9. Transaction Reference Data Backup and Extract


As a batch operations team, I want transaction type and transaction category reference data extracted from the database and backed up on a scheduled basis so that downstream card processing applications have access to current reference data and point-in-time snapshots are preserved for recovery purposes.

### Requirements

REQ-F-084: [Ubiquitous] The system shall create a new versioned backup of the transaction type file (legacy: AWS.M2.CARDDEMO.TRANTYPE.PS) and retain it for future reference.

REQ-F-085: [Event-driven] When the transaction type backup completes successfully, the system shall create a new versioned backup of the transaction category file (legacy: AWS.M2.CARDDEMO.TRANCATG.PS) and retain it for future reference.

REQ-F-086: [Event-driven] When the transaction category backup completes successfully, the system shall delete the prior versions of both the transaction type file and the transaction category file to maintain a clean state.

REQ-F-087: [Event-driven] When dataset cleanup completes successfully, the system shall extract all transaction type records from the transaction type table (legacy: CARDDEMO.TRANSACTION_TYPE), ordered by type code, and write them to the transaction type file.

REQ-F-088: [Event-driven] When the transaction type extraction completes successfully, the system shall extract all transaction type category records from the transaction type category store (legacy: CARDDEMO.TRANSACTION_TYPE_CATEGORY:CARDSTTC:CARDDEMO), ordered by type code and category identifier, and write them to the transaction category file.

REQ-F-089: [Ubiquitous] The system shall order transaction type extract records by type code in ascending sequence.

REQ-F-090: [Ubiquitous] The system shall order transaction category extract records by type code and then by category identifier, both in ascending sequence.

### Non-Functional Requirements

REQ-N-008: [Unwanted] If the transaction type backup fails, the system shall not proceed with the transaction category backup or any subsequent steps.

REQ-N-009: [Unwanted] If the transaction category backup fails, the system shall not proceed with dataset cleanup or extraction steps.

REQ-N-010: [Unwanted] If the transaction type extraction fails, the system shall not proceed with the transaction category extraction.

### Open Questions

OQ-14: The job purpose states extraction from DB2 tables, but the backup steps operate on sequential files. Are the sequential files populated by a prior run of this same job, or by another upstream process? — Owner: batch operations / data architecture team

OQ-15: What is the retention policy for the versioned backups of the transaction type and transaction category files? — Owner: data governance team

---

## 10. Transaction Type Reference Data Initialization


As a reference data administrator, I want the transaction type master data store to be initialized with a complete set of valid transaction type codes and descriptions so that downstream card processing operations can validate and categorize transactions against authoritative reference data.

### Requirements

REQ-F-091: [Event-driven] When the transaction type data initialization process executes, the system shall replace the contents of the transaction type keyed data store (legacy: AWS.M2.CARDDEMO.TRANTYPE.VSAM.KSDS) with the full set of transaction type records from the transaction type file (legacy: AWS.M2.CARDDEMO.TRANTYPE.PS), ensuring the data store reflects the complete and current reference data.

REQ-F-092: [Ubiquitous] The system shall ensure that after initialization completes, the transaction type keyed data store contains all transaction type records from the source, each keyed by its 2-byte transaction type code, with no residual records from any prior version of the data store.

### Non-Functional Requirements

REQ-N-011: [Unwanted] If the initialization process is interrupted after the prior data store contents have been removed but before the new data is fully loaded, the system shall support re-execution of the entire initialization process to restore the transaction type keyed data store to a consistent, fully populated state.

### Open Questions

OQ-16: The source transaction type file is loaded in its entirety during initialization. Is there a defined set of mandatory transaction type codes that must always be present after initialization (i.e., a completeness validation), or is the source file considered authoritative without further checks? — Owner: Reference Data domain expert

---

## 11. Job Dependencies

The following dependencies are inferred from shared data store access:

- **CREADB21.jcl** (Section 4: Reference Data Database Initialization) → **DEFGDGD.jcl** (Section 5: Reference Data Versioned Backup) (via `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TRANCATG.PS`, `AWS.M2.CARDDEMO.TRANTYPE.PS`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **CREADB21.jcl** (Section 4: Reference Data Database Initialization) → **DISCGRP.jcl** (Section 6: Disclosure Group Data Store Initialization) (via `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TRANCATG.PS`, `AWS.M2.CARDDEMO.TRANTYPE.PS`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **CREADB21.jcl** (Section 4: Reference Data Database Initialization) → **TRANCATG.jcl** (Section 8: Transaction Category Data Refresh) (via `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TRANCATG.PS`, `AWS.M2.CARDDEMO.TRANTYPE.PS`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **CREADB21.jcl** (Section 4: Reference Data Database Initialization) → **TRANEXTR.jcl** (Section 9: Transaction Reference Data Backup and Extract) (via `AWS.M2.CARDDEMO.TRANCATG.PS`, `AWS.M2.CARDDEMO.TRANTYPE.PS`)
- **CREADB21.jcl** (Section 4: Reference Data Database Initialization) → **TRANTYPE.jcl** (Section 10: Transaction Type Reference Data Initialization) (via `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TRANCATG.PS`, `AWS.M2.CARDDEMO.TRANTYPE.PS`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **DEFGDGD.jcl** (Section 5: Reference Data Versioned Backup) → **DISCGRP.jcl** (Section 6: Disclosure Group Data Store Initialization) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **DEFGDGD.jcl** (Section 5: Reference Data Versioned Backup) → **TRANCATG.jcl** (Section 8: Transaction Category Data Refresh) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **DEFGDGD.jcl** (Section 5: Reference Data Versioned Backup) → **TRANTYPE.jcl** (Section 10: Transaction Type Reference Data Initialization) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **DISCGRP.jcl** (Section 6: Disclosure Group Data Store Initialization) → **DEFGDGD.jcl** (Section 5: Reference Data Versioned Backup) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **DISCGRP.jcl** (Section 6: Disclosure Group Data Store Initialization) → **TRANCATG.jcl** (Section 8: Transaction Category Data Refresh) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **DISCGRP.jcl** (Section 6: Disclosure Group Data Store Initialization) → **TRANTYPE.jcl** (Section 10: Transaction Type Reference Data Initialization) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANCATG.jcl** (Section 8: Transaction Category Data Refresh) → **DEFGDGD.jcl** (Section 5: Reference Data Versioned Backup) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANCATG.jcl** (Section 8: Transaction Category Data Refresh) → **DISCGRP.jcl** (Section 6: Disclosure Group Data Store Initialization) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANCATG.jcl** (Section 8: Transaction Category Data Refresh) → **TRANTYPE.jcl** (Section 10: Transaction Type Reference Data Initialization) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANEXTR.jcl** (Section 9: Transaction Reference Data Backup and Extract) → **DEFGDGD.jcl** (Section 5: Reference Data Versioned Backup) (via `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TRANCATG.PS`, `AWS.M2.CARDDEMO.TRANTYPE.PS`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **TRANEXTR.jcl** (Section 9: Transaction Reference Data Backup and Extract) → **DISCGRP.jcl** (Section 6: Disclosure Group Data Store Initialization) (via `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TRANCATG.PS`, `AWS.M2.CARDDEMO.TRANTYPE.PS`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **TRANEXTR.jcl** (Section 9: Transaction Reference Data Backup and Extract) → **TRANCATG.jcl** (Section 8: Transaction Category Data Refresh) (via `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TRANCATG.PS`, `AWS.M2.CARDDEMO.TRANTYPE.PS`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **TRANEXTR.jcl** (Section 9: Transaction Reference Data Backup and Extract) → **TRANTYPE.jcl** (Section 10: Transaction Type Reference Data Initialization) (via `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TRANCATG.PS`, `AWS.M2.CARDDEMO.TRANTYPE.PS`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **TRANTYPE.jcl** (Section 10: Transaction Type Reference Data Initialization) → **DEFGDGD.jcl** (Section 5: Reference Data Versioned Backup) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANTYPE.jcl** (Section 10: Transaction Type Reference Data Initialization) → **DISCGRP.jcl** (Section 6: Disclosure Group Data Store Initialization) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **TRANTYPE.jcl** (Section 10: Transaction Type Reference Data Initialization) → **TRANCATG.jcl** (Section 8: Transaction Category Data Refresh) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)

*These dependencies represent data flow ordering constraints. The implementation should ensure predecessor jobs complete before successors execute.*

---



## Shared Capability Dependencies

This capability depends on the following shared capabilities.
Do not reimplement their behavior — integrate with the shared service.

- **COSGN00C** (`_shared/COSGN00C/`) — As a user of the CardDemo application, I want to sign on with my credentials so that I am authenticated and directed to the appropriate application functions based on my role.
