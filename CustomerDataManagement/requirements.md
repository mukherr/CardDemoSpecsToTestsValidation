# Customer Data Management — Requirements

## 1. Global Preconditions

- All operations require valid input data and appropriate authorization.
- Processing constraints and scheduling dependencies are documented in the Job Dependencies section.


> **Note:** The legacy system implements some of this processing as scheduled batch jobs. The modernized system may implement these requirements as batch, event-driven, or real-time processing provided the requirements below are met.

---

## 2. Customer Master Data Refresh


As a batch operations team, I want the customer master data store refreshed from the authoritative sequential source so that the online transaction processing system operates against a complete and consistent set of customer records.

**Restart/Recovery:** The refresh process is idempotent — re-execution recreates the customer keyed dataset from the source file, producing the same result regardless of prior state.

### Requirements

REQ-F-001: [Event-driven] When the customer master data refresh is initiated, the system shall replace the entire contents of the customer keyed dataset (legacy: AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS) with all customer records from the customer sequential file (legacy: AWS.M2.CARDDEMO.CUSTDATA.PS).

REQ-F-002: [Ubiquitous] The system shall ensure the customer keyed dataset is indexed by customer identifier (9-digit numeric key) to support efficient key-based retrieval of individual customer records.

REQ-F-003: [Ubiquitous] Each customer record in the customer keyed dataset shall contain: customer identifier, customer first name, customer middle name, customer last name, address lines 1–3, state code, country code, zip code, phone numbers 1–2, SSN, government-issued identifier, date of birth, EFT account identifier, primary card holder indicator, and FICO credit score.

REQ-F-004: [Event-driven] When the customer master data refresh is initiated, the system shall make the customer keyed dataset unavailable to online users before beginning the replacement, and restore online availability after the refresh completes successfully.

### Non-Functional Requirements

REQ-N-001: [Unwanted] If the refresh process is interrupted after the customer keyed dataset has been cleared but before all records are loaded, the system shall support re-execution of the entire refresh to restore a consistent state.

REQ-N-002: [Ubiquitous] The customer keyed dataset shall support concurrent read access by multiple online sessions.

### Open Questions

OQ-1: The job purpose states this is a "refresh" operation that replaces all customer data. Is this intended to run on a recurring schedule (e.g., daily), or only for initial setup and ad-hoc recovery scenarios? — Owner: batch operations team

OQ-2: The sequential source file is treated as the authoritative source of customer records. What upstream process produces or maintains this file, and what guarantees exist regarding its completeness? — Owner: data management team

---

## 3. Customer Data Listing


As a business analyst, I want a complete listing of all customer records from the customer master file so that I can review and validate customer demographic information, contact details, and credit profiles.

### Requirements

REQ-F-005: [Event-driven] When the customer data listing process executes, the system shall read all customer records sequentially from the customer keyed dataset (legacy: AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS) and produce a listing containing each customer's identifier, name (first, middle, last), address (lines 1–3, state code, country code, zip code), phone numbers, SSN, government-issued ID, date of birth, EFT account identifier, primary card holder indicator, and FICO credit score.

REQ-F-006: [Ubiquitous] The system shall operate in read-only mode, producing no modifications to any customer data during the listing process.

REQ-F-007: [Unwanted] If a customer record cannot be retrieved due to a data access error (other than end-of-collection), the system shall report the error condition and terminate processing.

---

## 4. Job Dependencies

The following dependencies are inferred from shared data store access:

- **CUSTFILE.jcl** (Section 2: Customer Master Data Refresh) → **READCUST.jcl** (Section 3: Customer Data Listing) (via `AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS`)

*These dependencies represent data flow ordering constraints. The implementation should ensure predecessor jobs complete before successors execute.*

---



---

## Security Hardening Requirements

**Based on Security Scan Findings:** #4, #15

### SSN and PII Protection

REQ-SEC-001: [Ubiquitous] The system shall encrypt Social Security Numbers (SSN) at rest in the database using field-level encryption with AES-256 or equivalent encryption algorithm and proper key management.

REQ-SEC-002: [Ubiquitous] The system shall mask SSN values in all user interface displays, showing only the last 4 digits (e.g., "***-**-1234") and never displaying the full SSN.

REQ-SEC-003: [Ubiquitous] The system shall mask SSN values in all API responses, log files, error messages, and reports, ensuring SSN values are never exposed outside the secure processing layer.

REQ-SEC-004: [Event-driven] When a customer record is retrieved for display, the system shall decrypt the SSN only if required for authorized processing and shall immediately mask it for display purposes.

REQ-SEC-005: [Ubiquitous] The system shall restrict SSN decryption to only those system components that have a legitimate business need, implementing least-privilege access controls.

REQ-SEC-006: [Ubiquitous] The system shall log all SSN access events (decryption operations) to a secure audit trail including: timestamp, user ID, customer ID, and operation type for compliance monitoring.

### Government ID and Date of Birth Protection

REQ-SEC-007: [Ubiquitous] The system shall encrypt government-issued identification numbers at rest in the database using field-level encryption with AES-256 or equivalent.

REQ-SEC-008: [Ubiquitous] The system shall mask government ID values in displays, showing only partial information (e.g., last 4 characters) unless full access is explicitly authorized and logged.

REQ-SEC-009: [Ubiquitous] The system shall treat date of birth as sensitive PII and shall mask or limit its display to authorized users only, logging all access to date of birth fields.

### Customer Data Access Authorization

REQ-SEC-010: [Event-driven] When a user attempts to view or update customer information, the system shall verify the user has authorization to access that customer's data based on their role and assigned customer relationships.

REQ-SEC-011: [Unwanted] If a user attempts to access customer data they are not authorized to view, the system shall reject the request with a generic "customer not found" error rather than revealing the customer exists but is unauthorized.

REQ-SEC-012: [Ubiquitous] Regular users shall only access their own customer profile; customer service representatives shall access only customers assigned to them; administrators may access all customer records with audit logging.

REQ-SEC-013: [Ubiquitous] The system shall log all customer data access operations including: timestamp, user ID, customer ID, fields accessed, and operation type (view, update, delete).

### Sensitive Data Storage

REQ-SEC-014: [Ubiquitous] The system shall encrypt all sensitive customer PII at rest, including: SSN, government ID, date of birth, email address, phone number, and physical address using field-level encryption.

REQ-SEC-015: [Ubiquitous] The system shall store encryption keys separately from encrypted data, using a secure key management service or hardware security module (HSM), and shall rotate encryption keys according to organizational policy.

REQ-SEC-016: [Ubiquitous] The system shall implement data classification policies that identify which customer fields contain PII and apply appropriate encryption, masking, and access controls to each classification level.

### Input Validation

REQ-SEC-017: [Unwanted] If an SSN does not match the expected format (9 digits, optionally formatted as XXX-XX-XXXX), the system shall reject the input and display an error message.

REQ-SEC-018: [Unwanted] If a government ID contains invalid characters or does not match expected format patterns, the system shall reject the input and display an error message.

REQ-SEC-019: [Unwanted] If a date of birth is in the future or represents an age less than 18 years (for credit card applicants), the system shall reject the input and display an appropriate error message.

REQ-SEC-020: [Ubiquitous] The system shall validate all customer data inputs server-side before processing, treating client-side validation as user experience enhancements only.

### Data Minimization

REQ-SEC-021: [Ubiquitous] The system shall collect and store only the minimum customer PII necessary for business operations and regulatory compliance, avoiding collection of unnecessary sensitive data.

REQ-SEC-022: [Ubiquitous] The system shall implement data retention policies that automatically purge or anonymize customer PII after the required retention period expires.

REQ-SEC-023: [Event-driven] When a customer account is closed, the system shall retain only the minimum data required for regulatory compliance and shall encrypt or anonymize all other PII.

### Error Handling

REQ-SEC-024: [Unwanted] If a customer data operation error occurs, the system shall display a generic error message to the user without exposing internal system details, database errors, or PII values.

REQ-SEC-025: [Ubiquitous] The system shall ensure that error messages, stack traces, and debug logs never contain unencrypted SSN, government ID, or other sensitive PII values.

### Non-Functional Security Requirements

REQ-SEC-N-001: [Ubiquitous] The system shall comply with data protection regulations including GDPR, CCPA, and other applicable privacy laws regarding collection, storage, and processing of customer PII.

REQ-SEC-N-002: [Ubiquitous] The system shall implement role-based access control (RBAC) for customer data operations, with different permission levels for: customers (own data only), customer service (assigned customers), compliance officers (read-only all customers), and administrators (full access with audit logging).

REQ-SEC-N-003: [Ubiquitous] The system shall provide customers with the ability to view, correct, and request deletion of their personal data in compliance with data subject rights under privacy regulations.

REQ-SEC-N-004: [Ubiquitous] The system shall implement data breach notification procedures that can identify affected customers and notify them within required timeframes if PII is compromised.

### Open Questions

OQ-SEC-01: What is the organization's data classification policy and which customer fields are classified as sensitive PII requiring encryption? — Owner: Data governance team / Privacy officer

OQ-SEC-02: What is the required data retention period for customer PII after account closure? — Owner: Compliance team / Legal team

OQ-SEC-03: What key management system (KMS) or hardware security module (HSM) should be used for PII encryption key storage and management? — Owner: Security architecture team

OQ-SEC-04: What is the required encryption key rotation schedule for customer PII encryption keys? — Owner: Security team / Compliance team
