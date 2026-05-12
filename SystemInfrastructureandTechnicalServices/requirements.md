# System Infrastructure and Technical Services — Requirements

## 1. Global Preconditions

- All operations require valid input data and appropriate authorization.
- Processing constraints and scheduling dependencies are documented in the Job Dependencies section.


> **Note:** The legacy system implements some of this processing as scheduled batch jobs. The modernized system may implement these requirements as batch, event-driven, or real-time processing provided the requirements below are met.

---

## 2. Batch Program Compilation


As a development operations team, I want batch COBOL programs compiled and linked into executable modules so that updated application code can be deployed and executed in the card demo batch processing environment.

**Category:** setup

**Purpose:** Compiles the BATCHPGM source from the COBOL source library (legacy: AWS.M2.CARDDEMO.CBL) and produces a linked executable module in the load library (legacy: AWS.M2.CARDDEMO.LOADLIB), making the program available for batch execution.

**Migration relevance:** Defines the build process for batch application components. The modernized system must provide an equivalent build/deployment mechanism that produces deployable artifacts from source code.

### Requirements

REQ-F-001: [Event-driven] When a batch program compilation is requested for a named source member, the system shall compile the source code from the cobol source library and produce a deployable executable module in the load library.

REQ-F-002: [Ubiquitous] The system shall accept a source member name and a dataset qualifier as parameters to identify the compilation target and determine the output location for the executable module.

REQ-F-003: [Unwanted] If compilation of the source member fails, the system shall halt processing and not produce an executable module in the load library.

### Open Questions

OQ-1: Are there specific compiler options or linkage editor options that represent business constraints (e.g., required optimization levels, boundary checking) versus purely platform-specific settings? — Owner: development operations team

---

## 3. Screen Map Compilation and Deployment


As a development operations team, I want screen map definitions compiled and deployed to the running application environment so that updated screen layouts and data structures are immediately available to online application programs without requiring a full system restart.

### Requirements

REQ-F-004: [Event-driven] When a screen map source definition (legacy: CICSMAP) is submitted for compilation, the system shall compile the map source into an executable screen layout and generate corresponding data structure definitions (stored in the copybook library), then deploy the compiled output to the load library (legacy: AWS.M2.CARDDEMO.LOADLIB).

REQ-F-005: [Event-driven] When screen map compilation completes successfully, the system shall activate the updated screen map definition in the running application environment so that online programs immediately use the new layout without requiring a full environment restart.

### Open Questions

OQ-2: The legacy job accepts a map name parameter, implying multiple screen maps may be compiled independently. Should the modernized system support on-demand compilation of individual screen maps, or should all screen maps be deployed as a single versioned bundle? — Owner: development operations team

---

## 4. Application Resource Registration


As a system operations team, I want all application screens, programs, transactions, and runtime libraries registered in the application server environment so that the card management application is fully operational and accessible to users and administrators.

### Requirements

REQ-F-006: [Ubiquitous] The system shall register a runtime library resource pointing to the application load library, enabling the application server to locate and load compiled application programs.

REQ-F-007: [Ubiquitous] The system shall register the login screen and login program with transaction identifier CC00, enabling user authentication when the application is invoked.

REQ-F-008: [Ubiquitous] The system shall register the administration menu screen and administration program with transaction identifier CCDM, enabling administrators to access administrative functions.

REQ-F-009: [Ubiquitous] The system shall register four account management screens (account menu, view account, update account, and deactivate account) and their corresponding programs, each permitting unrestricted data access.

REQ-F-010: [Ubiquitous] The system shall register four card management screens (card menu, view card, update card, and deactivate card) and their corresponding programs, each permitting unrestricted data access.

REQ-F-011: [Ubiquitous] The system shall register four transaction management screens (transaction menu, transaction report, transaction details, and add transactions) and their corresponding programs, each permitting unrestricted data access.

REQ-F-012: [Ubiquitous] The system shall register the bill pay setup screen and the bill pay program, permitting unrestricted data access.

REQ-F-013: [Ubiquitous] The system shall register four test program screens and their corresponding programs with transaction identifiers CCT1, CCT2, CCT3, and CCT4 respectively, each permitting unrestricted data access.

REQ-F-014: [Ubiquitous] The system shall register five transactions — CCDM linked to the administration program, and CCT1 through CCT4 linked to test programs 1 through 4 respectively — each allowing any task data length.

REQ-F-015: [Event-driven] When resource registration completes, the system shall produce a listing of all registered resources in the application resource group to confirm successful installation.

### Open Questions

OQ-3: The legacy rules reference duplicate registration of the login screen mapset (described as a possible correction or intentional override). Should the modernized system treat this as a single registration, or is there a business reason for the duplication? — Owner: application operations team

OQ-4: The legacy rules describe identical program names for both account management and card management (COACT00C, COACTVWC, COACTUPC, COACTDEC). Are these truly the same programs serving dual purposes, or is this a documentation error where card-specific programs should have distinct identifiers? — Owner: application development team

---

## 5. CICS Application Program Deployment


As a development operations team, I want application program source compiled and immediately activated in the running application server so that program updates are deployed without system downtime or restart.

### Requirements

REQ-F-016: [Event-driven] When a CICS application source member is submitted for deployment, the system shall compile the source code from the COBOL source library (legacy: AWS.M2.CARDDEMO.CBL(&MEM)), link it with the required runtime libraries, and produce an executable module in the load library (legacy: AWS.M2.CARDDEMO.LOADLIB).

REQ-F-017: [Event-driven] When compilation and linking complete successfully, the system shall refresh the specified program in the active application server region so that the newly compiled version is immediately available to serve requests.

REQ-F-018: [Unwanted] If the compilation or linking step fails (indicating errors beyond minor warnings), the system shall not attempt to refresh the program in the active application server region.

### Open Questions

OQ-5: The legacy job uses a condition code threshold of less than 4 to gate the refresh step. Should the modernized system treat compiler warnings (non-zero but below threshold) as acceptable for deployment, or should a stricter zero-defect policy apply? — Owner: development operations team

---

## 6. CICS DB2 Program Compilation and Deployment


As a development operations team, I want CICS-enabled COBOL programs with DB2 database integration compiled and deployed to the load library so that the CardDemo application's online transaction processing programs are available for execution in the runtime environment.

**Category:** setup
**Purpose:** Compiles COBOL source code that uses both CICS transaction processing and DB2 database access, producing executable load modules deployed to the production load library.
**Migration relevance:** Defines the build pipeline for online programs that interact with DB2 databases. In a modernized system, this is replaced by the target platform's build and deployment toolchain (e.g., CI/CD pipeline). The business requirement is that compiled/built application components are validated and made available for runtime execution.

### Requirements

REQ-F-019: [Event-driven] When a CICS DB2 program build is initiated for a named source member, the system shall produce a deployable executable artifact in the load library (legacy: AWS.M2.CARDDEMO.LOADLIB) from the corresponding source in the COBOL source library (legacy: AWS.M2.CARDDEMO.CBL).

REQ-F-020: [Ubiquitous] The system shall resolve all DB2 SQL statements embedded in the source during the build process, producing a database request module in the DBRM library (legacy: AWS.M2.CARDDEMO.DBRMLIB) that corresponds to the compiled program.

REQ-F-021: [Ubiquitous] The system shall resolve all copybook dependencies from the copybook library (legacy: AWS.M2.CARDDEMO.CPY) during compilation.

REQ-F-022: [Unwanted] If the build process fails at any stage (SQL preprocessing, compilation, or link), the system shall not deploy a partial or invalid artifact to the load library.

### Open Questions

OQ-6: The legacy job compiles a single program (CICSDB2P) which has no documented data store access or business rules. Is this program still active and required, or is it a placeholder/template? — Owner: application development team

OQ-7: Should the DB2 plan binding step (associating the DBRM with a DB2 plan or package) be considered part of this build process or handled separately? — Owner: database administration team

---

## 7. File Closure for Batch Processing Preparation


As a batch operations team, I want critical online transaction processing files closed in the application region before batch jobs execute, so that batch processes can obtain exclusive access to these files without concurrent access conflicts.

### Requirements

REQ-F-023: [Event-driven] When batch processing requires exclusive access to transaction processing files, the system shall close the transaction log file (legacy: AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS), the cross-reference index file (legacy: AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS), the account master file (legacy: AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS), the card keyed dataset (legacy: AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS), and the user security keyed data store (legacy: AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS) in the online transaction processing region, making them unavailable to online users until explicitly reopened.

REQ-F-024: [Ubiquitous] The system shall close all five files — the transaction keyed dataset, the card xref keyed dataset, the account keyed dataset, the card keyed dataset, and the user security keyed data store — as a single coordinated operation to ensure data consistency during offline batch processing.

REQ-F-025: [Ubiquitous] The system shall record the results of each file closure operation, including success confirmations and any errors or warnings, to an audit log for troubleshooting purposes.

### Non-Functional Requirements

REQ-N-001: [Unwanted] If any individual file closure fails, the system shall report the failure in the audit log and continue attempting to close the remaining files rather than aborting the entire operation.

### Open Questions

OQ-8: Should the batch processing framework verify that all five files are confirmed closed before allowing downstream batch jobs to proceed, or is the closure operation fire-and-forget? — Owner: batch operations team

OQ-9: Is there a defined maximum time window within which all files must be closed before the operation is considered failed? — Owner: batch operations team

---

## 8. File Transfer to Remote Server


As an integration team, I want application data transferred to a remote server so that downstream external systems can consume the data for processing or integration purposes.

**Category:** interface
**Data flow:** Reads the FTP test file (legacy: AWS.M2.CARDEMO.FTP.TEST) and transfers it to a designated folder on the remote server as a text file.
**Modern equivalent:** The data exchange pattern (target location, file naming, text format, frequency) must be preserved; the transfer mechanism is implementation-specific.

### Requirements

REQ-F-026: [Event-driven] When the file transfer job executes, the system shall transfer the contents of the FTP test file to the remote server, placing the output in the `/ftpfolder` directory with the filename `welcome.txt`.

REQ-F-027: [Ubiquitous] The system shall convert the transferred file content to ASCII text format during the transfer.

REQ-F-028: [Ubiquitous] The system shall authenticate to the remote server at IP address 172.31.21.124 using the designated service account credentials before initiating the file transfer.

REQ-F-029: [Event-driven] When the file transfer completes, the system shall close the connection to the remote server and release associated resources.

### Non-Functional Requirements

REQ-N-002: [Unwanted] If authentication to the remote server fails, the system shall terminate the transfer job and report the failure.

REQ-N-003: [Unwanted] If the file transfer does not complete successfully, the system shall report the failure status for operational investigation.

### Open Questions

OQ-10: The legacy job uses a hardcoded IP address (172.31.21.124) and credentials (username: carddemousr). Should the modernized system externalize these as configurable connection parameters (e.g., via a secrets manager or configuration service)? — Owner: infrastructure/security team

OQ-11: Is the file transfer triggered on a schedule, on-demand, or as part of a larger batch workflow? The legacy JCL does not indicate scheduling dependencies. — Owner: operations team

OQ-12: Should the system verify successful delivery (e.g., confirm file exists on remote server after transfer) or is fire-and-forget acceptable? — Owner: integration team

---

## 9. Application Build and Deployment for IMS/MQ-Enabled Service


As a development operations team, I want the IMS and MQ-integrated transaction processing application compiled and deployed so that the executable service is available for online transaction processing, IMS database access, and message queue communication.

**Category:** setup
**Purpose:** Builds and deploys the executable module for a program that processes online transactions, accesses IMS databases, and exchanges messages via MQ services.
**Migration relevance:** Defines the build and deployment pipeline for the IMS/MQ-integrated service component. In a modernized environment, this translates to a CI/CD build pipeline that produces a deployable artifact with equivalent runtime dependencies (database access libraries, messaging client libraries, and transaction processing framework).

### Requirements

REQ-F-030: [Event-driven] When a build is initiated for the IMS/MQ-integrated transaction processing service, the system shall produce a deployable executable artifact from the COBOL source member (legacy: AWS.M2.COBOL.SRC(IMSMQPGM)) that incorporates transaction processing framework support, IMS database access capabilities, and message queue communication capabilities.

REQ-F-031: [Ubiquitous] The deployed executable shall be registered as a reentrant service entry point within the transaction processing framework, stored in the load library (legacy: AWS.M2.CICSLOAD).

REQ-F-032: [Ubiquitous] The build process shall include runtime libraries for the transaction processing framework, IMS database access, and message queue operations to ensure the deployed service can invoke all required platform services at execution time.

REQ-F-033: [Unwanted] If the source code preprocessing phase fails, the system shall halt the build process and not produce a deployable artifact.

REQ-F-034: [Unwanted] If the compilation phase fails, the system shall halt the build process and not proceed to link or deploy the artifact.

### Open Questions

OQ-13: What specific IMS databases and MQ queues does the IMSMQPGM program interact with at runtime? The build job references IMS and MQ libraries but no business rules for the program's runtime behavior are documented. — Owner: application development team

OQ-14: Is this build job executed on-demand during development or as part of a scheduled deployment pipeline? — Owner: release management team

---

## 10. FTP Test Data Backup and Subsequent Job Submission


As a batch operations team, I want the FTP test data backed up and a subsequent processing job automatically triggered so that data is preserved before further processing occurs and the workflow continues without manual intervention.

### Requirements

REQ-F-035: [Event-driven] When the backup and submission job executes, the system shall create a backup copy of the FTP test file (legacy: AWS.M2.CARDEMO.FTP.TEST) to the ftp test backup (legacy: AWS.M2.CARDEMO.FTP.TEST.BKUP) before any subsequent processing is initiated.

REQ-F-036: [Event-driven] When the backup completes successfully, the system shall automatically trigger execution of the subsequent processing job (INTRDRJ2) to continue the card demo application workflow.

### Open Questions

OQ-15: What is the business-level content and purpose of the FTP test file being backed up — is this operational data required for production processing, or is it test/validation data used during deployment verification? — Owner: application operations team

OQ-16: What is the required retention policy for the ftp test backup — should multiple versions be maintained, or is only the most recent backup needed? — Owner: data management team

---

## 11. FTP Test Environment Data Backup


As an operations team, I want the FTP test environment data backed up to a designated recovery location so that the test environment can be restored if data is lost or corrupted.

### Requirements

REQ-F-037: [Event-driven] When the FTP test backup job executes, the system shall copy the contents of the FTP test backup dataset (legacy: AWS.M2.CARDEMO.FTP.TEST.BKUP) to the FTP test internal reader backup dataset (legacy: AWS.M2.CARDEMO.FTP.TEST.BKUP.INTRDR), preserving all data without transformation.

### Open Questions

OQ-17: The job copies data between two backup-related datasets. Is the "internal reader backup" dataset consumed by another process (e.g., submitted as a job stream), or is it purely a secondary backup copy? — Owner: infrastructure/operations team

OQ-18: What is the required scheduling frequency for this backup, and are there dependencies on prior jobs completing successfully? — Owner: batch scheduling team

---

## 12. Infrastructure Jobs


### Application Dataset Catalog Inventory

**Category:** skip
**Purpose:** Generates a comprehensive catalog listing of all CARDDEMO application datasets for documentation, audit, and data management purposes.
**Modern equivalent:** Cloud-native infrastructure provides built-in asset inventory, metadata catalogs, and resource tagging capabilities that automatically track data store existence and attributes without requiring a dedicated batch process.

---

## 13. Online Processing Environment Initialization


As a batch operations team, I want the online transaction processing environment initialized with all required business data stores opened so that the application can process transactions, access account data, perform cross-reference lookups, and validate user security.

**Category:** setup
**Purpose:** Opens essential business data stores in the online processing region to enable transaction processing, account management, and security validation.
**Migration relevance:** Defines the prerequisite state for the online application to function. In a modernized system, data store availability may be managed by the platform (e.g., connection pools, always-on databases) rather than explicit open/close commands.

### Requirements

REQ-F-038: [Event-driven] When the environment initialization process executes, the system shall make the transaction file, cross-reference file, account data file, account index file, and user security file available for online application access.

REQ-F-039: [Ubiquitous] The system shall confirm that all five data stores (transaction file, cross-reference file, account data file, account index file, user security file) are accessible before the online application begins processing user requests.

### Non-Functional Requirements

REQ-N-004: [Unwanted] If any of the five required data stores cannot be made available during initialization, the system shall prevent the online application from accepting user requests until the issue is resolved.

### Open Questions

OQ-19: The legacy implementation opens files for "exclusive batch access," implying online users are locked out during batch windows. Should the modernized system support concurrent online and batch access to these data stores, or must a mutual-exclusion window be preserved? — Owner: operations/architecture team

OQ-20: Is there a required ordering dependency among the five data stores being opened (e.g., must the account index be available before the account data file), or can they be made available in any order? — Owner: application support team

---

## 14. Batch Processing Timing Control


As a batch operations team, I want the system to introduce a controlled delay between dependent batch operations so that preceding processes have sufficient time to complete before subsequent processing begins.

### Requirements

REQ-F-040: [Event-driven] When the timing control step is triggered during batch execution, the system shall pause processing for 36 seconds before allowing subsequent operations to proceed.

REQ-F-041: [Ubiquitous] The system shall accept the delay duration as a configurable parameter expressed in centiseconds (where 3600 centiseconds equals 36 seconds).

### Open Questions

OQ-21: What specific upstream processes or resources require the 36-second delay before downstream processing can safely begin? Is 36 seconds a validated minimum, or should the delay be dynamically determined based on upstream completion signals? — Owner: batch operations team

OQ-22: In the modernized system, should this timing control be replaced by an event-driven dependency mechanism (e.g., completion callbacks or message-based triggers) rather than a fixed-duration wait? — Owner: architecture team

---

## 15. Batch COBOL Source Compilation


As a development operations team, I want COBOL source programs compiled into executable load modules so that updated or new batch programs can be deployed to the production load library for execution.

**Category:** infrastructure
**Purpose:** Compiles a COBOL source member from the source library and produces an executable load module in the load library.
**Modern equivalent:** This represents a build/compilation pipeline step. In a modernized system, this is replaced by a CI/CD build pipeline that compiles, links, and packages application code for deployment.

### Requirements

REQ-F-042: [Event-driven] When a batch compilation is triggered for a named source member, the system shall compile the source from the COBOL source library (legacy: AWS.M2.CARDDEMO.CBL(&MEM)) using available copybooks from the copybook library (legacy: AWS.M2.CARDDEMO.CPY) and produce an executable module in the load library (legacy: AWS.M2.CARDDEMO.LOADLIB).

REQ-F-043: [Event-driven] When compilation completes successfully, the system shall store the resulting executable module in the batch compile load set (legacy: compile_batch.jcl.template-&&LOADSET) and make it available in the load library for subsequent batch execution.

REQ-F-044: [Unwanted] If compilation fails due to source errors, the system shall produce a compilation listing in the listing member (legacy: AWS.M2.CARDDEMO.LST(&MEM)) detailing the errors encountered, and shall not produce an executable module.

### Open Questions

OQ-23: Are there specific compilation options or standards (e.g., optimization level, debugging flags) that must be preserved as business constraints in the modernized build pipeline? — Owner: development operations team

---

## 16. Job Dependencies

The following dependencies are inferred from shared data store access:

- **IMSMQCMP.jcl** (Section 9: Application Build and Deployment for IMS/MQ-Enabled Service) → **INTRDRJ1.jcl** (Section 10: FTP Test Data Backup and Subsequent Job Submission) (via `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **IMSMQCMP.jcl** (Section 9: Application Build and Deployment for IMS/MQ-Enabled Service) → **INTRDRJ2.jcl** (Section 11: FTP Test Environment Data Backup) (via `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **IMSMQCMP.jcl** (Section 9: Application Build and Deployment for IMS/MQ-Enabled Service) → **LISTCAT.jcl** (Section 12: Infrastructure Jobs) (via `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **INTRDRJ1.jcl** (Section 10: FTP Test Data Backup and Subsequent Job Submission) → **INTRDRJ2.jcl** (Section 11: FTP Test Environment Data Backup) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **INTRDRJ1.jcl** (Section 10: FTP Test Data Backup and Subsequent Job Submission) → **LISTCAT.jcl** (Section 12: Infrastructure Jobs) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **INTRDRJ2.jcl** (Section 11: FTP Test Environment Data Backup) → **INTRDRJ1.jcl** (Section 10: FTP Test Data Backup and Subsequent Job Submission) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **INTRDRJ2.jcl** (Section 11: FTP Test Environment Data Backup) → **LISTCAT.jcl** (Section 12: Infrastructure Jobs) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`)
- **LISTCAT.jcl** (Section 12: Infrastructure Jobs) → **IMSMQCMP.jcl** (Section 9: Application Build and Deployment for IMS/MQ-Enabled Service) (via `AWS.M2.CARDDEMO.TRANCATG.PS`, `AWS.M2.CARDDEMO.TRANTYPE.PS`)
- **LISTCAT.jcl** (Section 12: Infrastructure Jobs) → **INTRDRJ1.jcl** (Section 10: FTP Test Data Backup and Subsequent Job Submission) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANCATG.PS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANTYPE.PS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **LISTCAT.jcl** (Section 12: Infrastructure Jobs) → **INTRDRJ2.jcl** (Section 11: FTP Test Environment Data Backup) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANCATG.PS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANTYPE.PS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`, `AWS.M2.CARDDEMO.USRSEC.PS`)

*These dependencies represent data flow ordering constraints. The implementation should ensure predecessor jobs complete before successors execute.*

---



---

## Security Hardening Requirements

**Based on Security Scan Findings:** #1, #8, #12

### Configuration and Secret Management

REQ-SEC-001: [Ubiquitous] The system shall load all sensitive configuration values (database passwords, API keys, encryption keys, session secrets) from secure external sources (environment variables, secrets manager, or secure configuration service) and shall never include hardcoded secrets in source code.

REQ-SEC-002: [Unwanted] If a required secret or sensitive configuration value is not available from the secure configuration source at application startup, the system shall fail to start and log an error indicating the missing configuration without exposing the secret name in user-facing messages.

REQ-SEC-003: [Ubiquitous] The system shall use cryptographically secure random values for session secret keys with a minimum entropy of 256 bits, generated using a cryptographically secure random number generator.

REQ-SEC-004: [Ubiquitous] The system shall rotate session secret keys according to organizational policy (e.g., every 90 days) and shall support graceful key rotation without invalidating active user sessions.

REQ-SEC-005: [Ubiquitous] The system shall store all secrets in encrypted form when at rest, using encryption provided by the secrets management service or operating system keystore.

REQ-SEC-006: [Event-driven] When the application starts, the system shall validate that all required configuration values are present and properly formatted before accepting user requests.

### Error Handling and Information Disclosure Prevention

REQ-SEC-007: [Unwanted] If an application error occurs, the system shall display a generic error message to users (e.g., "An error occurred. Please try again later.") without exposing internal system details, database schema, file paths, or stack traces.

REQ-SEC-008: [Ubiquitous] The system shall log detailed error information including exception messages, stack traces, database errors, and system state to internal application logs accessible only to authorized operations personnel.

REQ-SEC-009: [Ubiquitous] The system shall sanitize all error messages before displaying to users, removing or masking: file system paths, database connection strings, internal IP addresses, software version numbers, and SQL query fragments.

REQ-SEC-010: [Event-driven] When a database error occurs, the system shall log the full error details (including SQL state, error code, and query) internally while displaying only "A database error occurred" to the user.

REQ-SEC-011: [Ubiquitous] The system shall configure error handling frameworks to suppress detailed error pages in production environments, showing only generic error pages to users.

REQ-SEC-012: [Unwanted] If an exception contains sensitive data (PII, credentials, encryption keys), the system shall redact that data before logging the exception to prevent sensitive data exposure in log files.

### API Security and Authentication

REQ-SEC-013: [Ubiquitous] All API endpoints shall require authentication using a secure authentication mechanism (session-based, JWT, or OAuth tokens) and shall reject unauthenticated requests with HTTP 401 status.

REQ-SEC-014: [Event-driven] When an API endpoint is registered or deployed, the system shall verify that authentication middleware is properly configured and shall prevent deployment of unauthenticated endpoints to production.

REQ-SEC-015: [Ubiquitous] The system shall implement a centralized authentication decorator or middleware that is applied to all API endpoints by default, requiring explicit opt-out (with justification and approval) for public endpoints.

REQ-SEC-016: [Unwanted] If an API request does not include valid authentication credentials, the system shall reject the request with HTTP 401 status and a generic error message without processing the request.

REQ-SEC-017: [Ubiquitous] The system shall implement rate limiting on all API endpoints to prevent abuse, with different limits for authenticated vs. unauthenticated requests.

REQ-SEC-018: [Ubiquitous] The system shall log all API requests including: timestamp, endpoint, HTTP method, source IP, user ID (if authenticated), response status, and response time for security monitoring and audit.

### API Endpoint Registration Controls

REQ-SEC-019: [Ubiquitous] The system shall maintain a registry of all API endpoints with their authentication requirements, authorization rules, and rate limits, and shall enforce these requirements at runtime.

REQ-SEC-020: [Event-driven] When the application starts, the system shall validate that all registered API endpoints have appropriate authentication and authorization controls configured.

REQ-SEC-021: [Unwanted] If an API blueprint or route module is registered without proper authentication configuration, the system shall log a security warning and shall not activate the endpoint until authentication is properly configured.

REQ-SEC-022: [Ubiquitous] The system shall implement environment-based endpoint registration controls that prevent test or development API endpoints from being registered in production environments.

### Logging and Monitoring

REQ-SEC-023: [Ubiquitous] The system shall implement structured logging with consistent log levels (DEBUG, INFO, WARN, ERROR, CRITICAL) and shall configure production environments to log at INFO level or higher.

REQ-SEC-024: [Ubiquitous] The system shall log security-relevant events including: authentication attempts (success and failure), authorization failures, configuration changes, API access, and error conditions.

REQ-SEC-025: [Ubiquitous] The system shall implement log rotation and retention policies to prevent log files from consuming excessive disk space while retaining logs for the required audit period.

REQ-SEC-026: [Ubiquitous] The system shall protect log files from unauthorized access using file system permissions, ensuring only authorized operations personnel can read application logs.

REQ-SEC-027: [Unwanted] If log files contain sensitive data (PII, credentials, encryption keys), the system shall redact or mask that data before writing to logs, or shall encrypt log files at rest.

### Environment Configuration

REQ-SEC-028: [Ubiquitous] The system shall support multiple deployment environments (development, test, staging, production) with environment-specific configuration that is loaded based on an environment identifier.

REQ-SEC-029: [Ubiquitous] The system shall implement different security controls for different environments, with production environments having the strictest controls (e.g., debug mode disabled, detailed errors suppressed, HTTPS required).

REQ-SEC-030: [Event-driven] When the application starts, the system shall detect the current environment and apply appropriate security configuration, logging the detected environment for verification.

REQ-SEC-031: [Unwanted] If the system cannot determine the current environment, it shall default to production-level security controls to ensure maximum security.

### Debug and Development Features

REQ-SEC-032: [Ubiquitous] The system shall disable all debug features, development endpoints, and verbose error reporting in production environments.

REQ-SEC-033: [Unwanted] If debug mode is enabled in a production environment, the system shall log a critical security warning and shall disable debug features automatically.

REQ-SEC-034: [Ubiquitous] The system shall remove or disable any development-only features (test data generators, debug endpoints, profiling tools) before deploying to production.

### Non-Functional Security Requirements

REQ-SEC-N-001: [Ubiquitous] The system shall implement a secure software development lifecycle (SSDLC) including: security requirements analysis, threat modeling, secure coding practices, security testing, and security code review.

REQ-SEC-N-002: [Ubiquitous] The system shall implement automated security scanning in the CI/CD pipeline including: static application security testing (SAST), dependency vulnerability scanning, and secrets detection.

REQ-SEC-N-003: [Ubiquitous] The system shall maintain a software bill of materials (SBOM) listing all dependencies and their versions, and shall monitor for known vulnerabilities in dependencies.

REQ-SEC-N-004: [Ubiquitous] The system shall implement security headers in HTTP responses including: Content-Security-Policy, X-Frame-Options, X-Content-Type-Options, Strict-Transport-Security, and X-XSS-Protection.

### Open Questions

OQ-SEC-01: What secrets management service should be used (AWS Secrets Manager, HashiCorp Vault, Azure Key Vault, etc.)? — Owner: Security architecture team / DevOps team

OQ-SEC-02: What is the required log retention period for application logs, security logs, and audit logs? — Owner: Compliance team / Security team

OQ-SEC-03: What centralized logging and monitoring solution should be used (CloudWatch, Splunk, ELK stack, etc.)? — Owner: Operations team / Security team

OQ-SEC-04: What is the required session secret key rotation schedule? — Owner: Security team

OQ-SEC-05: Should the system implement a feature flag system to control API endpoint availability in different environments? — Owner: DevOps team / Application architecture team
