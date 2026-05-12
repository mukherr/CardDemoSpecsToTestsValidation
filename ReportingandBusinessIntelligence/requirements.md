# Reporting and Business Intelligence — Requirements

## 1. Global Preconditions

- User must be authenticated via the sign-on process before accessing reporting functions.
- A valid session context must exist containing the authenticated user ID and security credentials.
- Only users with appropriate role authorization may access the report submission screen.
- Batch reporting jobs require that prerequisite data stores (transaction data store, cross-reference data store, transaction category data store) are available and accessible.
- The versioned report output store must be configured to retain up to 10 historical generations.


> **Note:** The legacy system implements some of this processing as scheduled batch jobs. The modernized system may implement these requirements as batch, event-driven, or real-time processing provided the requirements below are met.


### Navigation Context

- From the main application menu, the user selects the reporting option, which transfers control to the transaction report submission screen.
- On the report submission screen, the user selects a report type (Monthly, Yearly, or Custom date range) and submits the request; the system queues the request for batch processing.
- Batch report generation (transaction report, transaction category balance report) executes via scheduled batch processing, not direct online navigation.
- Backup operations (transaction data backup, category balance data backup) execute as batch prerequisites or scheduled maintenance tasks prior to report generation.
- Upon completion of the report submission screen interaction, control returns to the main application menu.

OQ-1: What specific menu option or navigation path leads to the report submission screen (CORPT00C), and are there any intermediate screens? Owner: Business/Application SME.

---

## 2. Transaction Report Submission


As a business user, I want to generate transaction reports by selecting a report type (Monthly, Yearly, or Custom date range) and submitting the request for batch processing so that I can analyze transaction data for the desired time period.

### Requirements

**Navigation and Session Management**

REQ-F-001: [Event-driven] When no session context is provided, the system shall navigate the user to the sign-on screen.

REQ-F-002: [Complex] While the application is re-entered with existing session context, when the user requests to go back (PF3), the system shall navigate to the menu screen, passing the current session context including customer information, account information, card information, and navigation history.

REQ-F-003: [Ubiquitous] The system shall validate that a target program is designated before navigation; if no target is set, the system shall default to the sign-on screen.

REQ-F-004: [Ubiquitous] The system shall record the current program identifier and transaction identifier in the session context and reset the program context flag to indicate fresh entry before transferring to the target program.

**Screen Display**

REQ-F-005: [Ubiquitous] The system shall display the report selection screen with the current system date formatted as MM/DD/YY, the current system time formatted as HH:MM:SS, the application title, program name, and transaction identifier.

**Report Type Processing**

REQ-F-006: [Event-driven] When the user presses Enter, the system shall process the report request based on the selected report type.

REQ-F-007: [Event-driven] When the user presses any key other than Enter or PF3, the system shall display an invalid key message and redisplay the report selection screen.

REQ-F-008: [Event-driven] When the user selects the monthly report option, the system shall set the report date range to the first day through the last day of the current month and submit the batch report job.

REQ-F-009: [Event-driven] When the user selects the yearly report option, the system shall set the report date range to January 1st through December 31st of the current year and submit the batch report job.

REQ-F-010: [Event-driven] When the user selects the custom date range option, the system shall validate the user-entered start and end date fields and, if valid, submit the batch report job with the specified date range.

REQ-F-011: [Unwanted] If the user does not select any report type, the system shall display an error message 'Select a report type to print report...' and redisplay the screen.

**Custom Date Range Validation**

REQ-F-012: [Unwanted] If any required custom date field (start month, start day, start year, end month, end day, or end year) is empty, the system shall display an error message identifying the missing field and redisplay the screen without submitting.

REQ-F-013: [Ubiquitous] The system shall convert the start month, start day, start year, end month, end day, and end year fields from character strings to numeric values for validation.

REQ-F-014: [Unwanted] If the start month is non-numeric or greater than 12, the system shall display an error message 'Start Date - Not a valid Month...' and redisplay the screen.

REQ-F-015: [Unwanted] If the start day is non-numeric or greater than 31, the system shall display an error message 'Start Date - Not a valid Day...' and redisplay the screen.

REQ-F-016: [Unwanted] If the start year is non-numeric, the system shall display an error message 'Start Date - Not a valid Year...' and redisplay the screen.

REQ-F-017: [Unwanted] If the end month is non-numeric or greater than 12, the system shall display an error message 'End Date - Not a valid Month...' and redisplay the screen.

REQ-F-018: [Unwanted] If the end day is non-numeric or greater than 31, the system shall display an error message 'End Date - Not a valid Day...' and redisplay the screen.

REQ-F-019: [Unwanted] If the end year is non-numeric, the system shall display an error message 'End Date - Not a valid Year...' and redisplay the screen.

REQ-F-020: [Ubiquitous] The system shall invoke an external date validation service to confirm that the start date and end date are valid calendar dates.

REQ-F-021: [Unwanted] If the start date fails validation by the external date service, the system shall display an error message 'Start Date - Not a valid date...' and redisplay the screen.

REQ-F-022: [Unwanted] If the end date fails validation by the external date service, the system shall display an error message 'End Date - Not a valid date...' and redisplay the screen.

REQ-F-023: [Ubiquitous] The system shall set the report name to 'Custom' and copy the validated start and end dates into the job parameters after successful validation.

**Confirmation**

REQ-F-024: [Event-driven] When the user has not yet entered a confirmation value, the system shall display a confirmation message asking the user to confirm the report submission and redisplay the screen.

REQ-F-025: [Event-driven] When the user confirms with 'Y' or 'y', the system shall proceed with batch job submission.

REQ-F-026: [Event-driven] When the user declines with 'N' or 'n', the system shall clear all input fields and redisplay the screen without submitting.

REQ-F-027: [Unwanted] If the user enters any value other than 'Y', 'y', 'N', or 'n' in the confirmation field, the system shall display an error message and redisplay the screen.

**Batch Job Submission**

REQ-F-028: [State-driven] While the record index is not greater than 1000 and no end-of-file marker or empty record has been encountered and no errors have occurred, the system shall retrieve the job control record at the current index, write it to the batch job queue named 'JOBS', and increment the index.

REQ-F-029: [Event-driven] When a job control record equals '/*EOF', is empty, or contains only spaces, the system shall terminate the job record processing loop.

REQ-F-030: [Unwanted] If the write operation to the batch job queue fails, the system shall display an error message 'Unable to Write TDQ (JOBS)...' and redisplay the screen without completing submission.

REQ-F-031: [State-driven] While no errors have occurred during job submission, the system shall display a success message indicating the report type that was submitted.

**Field Reset**

REQ-F-032: [Event-driven] When the screen is reset (on decline or fresh display), the system shall clear all user-editable input fields including monthly, yearly, custom selection, start date components, end date components, and confirmation field.

### Open Questions

OQ-1: The job control records are retrieved from an internal data structure indexed up to 1000 records. What is the source of these job control templates (e.g., are they statically defined or configurable), and should the 1000-record limit be preserved? — Owner: batch operations team

OQ-2: The external date validation service is invoked for custom date ranges. What service contract or interface specification governs this validation? — Owner: application architecture team

---

## 3. Transaction Category Balance Data Backup


As a batch operations team, I want the transaction category balance data backed up to a versioned store so that a point-in-time copy is available for recovery or audit purposes.

### Requirements

REQ-F-033: [Event-driven] When the transaction category balance backup process is initiated, the system shall read all records from the transaction catalog keyed store (legacy: AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS) and write them to a new version of the transaction catalog backup (legacy: AWS.M2.CARDDEMO.TCATBALF.BKUP).

REQ-F-034: [Unwanted] If the backup operation fails, the system shall discard the incomplete backup version to prevent retention of partial data.

### Non-Functional Requirements

REQ-N-001: [Ubiquitous] The system shall read the transaction catalog keyed store with shared access so that concurrent read operations by other processes are not blocked during the backup.

### Open Questions

OQ-3: The job purpose states it "generates a formatted Transaction Category Balance report by sorting records and formatting balance amounts," but the only rule extracted describes a backup copy operation with no sorting or report formatting logic. Is the report generation handled by a separate downstream job, or is additional logic missing from this job's specification? — Owner: batch operations / reporting team

---

## 4. Transaction Data Backup


As a batch operations team, I want all transaction records periodically backed up to a versioned data store so that point-in-time snapshots are available for recovery and audit purposes.

### Requirements

REQ-F-035: [Event-driven] When the transaction backup process is initiated, the system shall copy all records from the transaction keyed dataset (legacy: AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS) to a new version of the transaction backup file (legacy: AWS.M2.CARDDEMO.TRANSACT.BKUP), creating a complete point-in-time snapshot of all transaction data.

REQ-F-036: [Ubiquitous] The system shall retain multiple historical versions of the transaction backup to support recovery from any prior backup point and to maintain an audit trail.

### Non-Functional Requirements

REQ-N-002: [Ubiquitous] The system shall perform the backup without blocking concurrent read access to the primary transaction keyed dataset.

REQ-N-003: [Unwanted] If the backup process is interrupted before completion, the system shall not treat the incomplete backup as a valid recovery point.

---

## 5. Transaction Report Version Management


As a reporting operations team, I want the system to maintain versioned transaction report outputs so that up to 10 historical generations of transaction reports are retained for audit and analysis purposes.

**Category:** setup
**Purpose:** Establishes the versioning structure for the transaction report file (legacy: AWS.M2.CARDDEMO.TRANREPT), enabling automatic retention of historical report generations.
**Migration relevance:** Defines the retention policy for transaction report outputs consumed by downstream reporting and audit processes.

### Requirements

REQ-F-037: [Ubiquitous] The system shall maintain up to 10 versions of the transaction report file, automatically removing the oldest version when a new version is created and the retention limit is exceeded.

REQ-F-038: [Event-driven] When a new transaction report is generated, the system shall assign it as the latest version within the transaction report version set.

### Open Questions

OQ-4: When the oldest transaction report version is aged out, should it be archived to long-term storage or permanently deleted? — Owner: compliance/audit team

---

## 6. Transaction Report Generation


As a business analyst, I want a comprehensive transaction report generated for a specified date range that includes enriched transaction details with cross-reference, type, and category information so that I can review transaction activity by account with page-level, account-level, and grand totals for analysis and compliance purposes.

**Restart/Recovery:** The job first creates a backup of the transaction master file before any filtering or transformation occurs, ensuring data recoverability. The report generation phase reads from the filtered dataset and produces a new version of the report output; if interrupted, the report output is incomplete but source data remains intact.

### Requirements

**Data Preparation**

REQ-F-039: [Event-driven] When the transaction report job executes, the system shall create a backup copy of the transaction master file before any filtering or transformation occurs.

REQ-F-040: [Event-driven] When the backup is complete, the system shall filter the transaction daily file (legacy: AWS.M2.CARDDEMO.TRANSACT.DALY) to include only transactions with a processing date between the configured start date and end date (inclusive).

REQ-F-041: [Ubiquitous] The system shall sort the filtered transactions by card number in ascending order before report generation.

**Date Parameters**

REQ-F-042: [Event-driven] When report generation begins, the system shall read the date parameters file (legacy: AWS.M2.CARDDEMO.DATEPARM) to obtain the start date and end date for the report period.

REQ-F-043: [Unwanted] If the date parameters file contains no records, the system shall halt report processing.

**Report Header**

REQ-F-044: [Event-driven] When the first transaction record is processed, the system shall write report headers consisting of the report title with the start and end date range, a blank line, column headers, and a separator line.

**Data Enrichment and Detail Output**

REQ-F-045: [Event-driven] When a transaction with a different card number than the previous transaction is encountered, the system shall retrieve the customer identifier and account identifier from the card cross-reference data store (legacy: AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS) using the card number as the lookup key.

REQ-F-046: [Event-driven] When a transaction is processed, the system shall retrieve the transaction type description from the transaction type data store (legacy: AWS.M2.CARDDEMO.TRANTYPE.VSAM.KSDS) using the transaction type code as the lookup key.

REQ-F-047: [Event-driven] When a transaction is processed, the system shall retrieve the transaction category description from the transaction category data store (legacy: AWS.M2.CARDDEMO.TRANCATG.VSAM.KSDS) using the composite key of transaction type code and transaction category code.

REQ-F-048: [Event-driven] When a transaction is ready to be written to the report, the system shall write a detail line containing the transaction ID, account ID, transaction type code, transaction type description, transaction category code, transaction category description, transaction source, and transaction amount.

**Totals and Accumulation**

REQ-F-049: [Event-driven] When a transaction detail line is written, the system shall accumulate the transaction amount into both the current page total and the current account total.

REQ-F-050: [Event-driven] When a page break is needed (every 20 detail lines), the system shall write the accumulated page total, add the page total to the grand total, reset the page total to zero, write a separator line, and write new page headers.

REQ-F-051: [Event-driven] When a transaction with a different card number than the previous transaction is encountered (and it is not the first transaction), the system shall write the accumulated account total for the previous card number, reset the account total to zero, and write a separator line.

REQ-F-052: [Event-driven] When all transactions have been processed, the system shall write the final page total, add it to the grand total, and write the grand total representing the sum of all transaction amounts in the report period.

**Report Output**

REQ-F-053: [Ubiquitous] The system shall write the completed transaction report to the transaction report file (legacy: AWS.M2.CARDDEMO.TRANREPT) as a new version.

### Non-Functional Requirements

REQ-N-004: [Unwanted] If the report generation process is interrupted after the backup is created, the original transaction master file shall remain unmodified, allowing the job to be re-executed from the beginning.

### Open Questions

OQ-5: The filtering date range is specified as 2022-01-01 to 2022-07-06 in the legacy rules, but the system also reads a date parameters file at runtime. Are the hard-coded dates a legacy artifact that should be replaced entirely by the date parameters file values, or do they serve as defaults? — Owner: Business operations team

OQ-6: The page break occurs every 20 lines. Is this a fixed business requirement for the report format, or should it be configurable? — Owner: Reporting team

---

## 7. Job Dependencies

The following dependencies are inferred from shared data store access:

- **PRTCATBL.jcl** (Section 3: Transaction Category Balance Data Backup) → **REPTFILE.jcl** (Section 5: Transaction Report Version Management) (via `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TRANCATG.PS`, `AWS.M2.CARDDEMO.TRANSACT.COMBINED`, `AWS.M2.CARDDEMO.TRANTYPE.PS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **PRTCATBL.jcl** (Section 3: Transaction Category Balance Data Backup) → **TRANREPT.jcl** (Section 6: Transaction Report Generation) (via `AWS.M2.CARDDEMO.TRANSACT.DALY`)
- **REPTFILE.jcl** (Section 5: Transaction Report Version Management) → **PRTCATBL.jcl** (Section 3: Transaction Category Balance Data Backup) (via `AWS.M2.CARDDEMO.TCATBALF.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`)
- **REPTFILE.jcl** (Section 5: Transaction Report Version Management) → **TRANREPT.jcl** (Section 6: Transaction Report Generation) (via `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.BKUP`, `AWS.M2.CARDDEMO.TRANCATG.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.BKUP`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANTYPE.VSAM.KSDS`)
- **TRANREPT.jcl** (Section 6: Transaction Report Generation) → **REPTFILE.jcl** (Section 5: Transaction Report Version Management) (via `AWS.M2.CARDDEMO.TRANSACT.COMBINED`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)

*These dependencies represent data flow ordering constraints. The implementation should ensure predecessor jobs complete before successors execute.*

---



## Shared Capability Dependencies

This capability depends on the following shared capabilities.
Do not reimplement their behavior — integrate with the shared service.

- **COSGN00C** (`_shared/COSGN00C/`) — As a user of the CardDemo application, I want to sign on with my credentials so that I am authenticated and directed to the appropriate application functions based on my role.
- **CSUTLDTC** (`_shared/CSUTLDTC/`) — As any system component requiring date validation, I want to submit a date string with its format specification and receive a structured validation result so that I can determine whether the date is valid and, if not, understand the specific reason for failure.
