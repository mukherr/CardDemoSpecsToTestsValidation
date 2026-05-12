# User Security and Access Management — Requirements

## 1. Global Preconditions

- Users must be authenticated via the sign-on process (COSGN00C) before accessing any online function in this group.
- Only users with an administrator user type may access user management functions (add, update, delete, browse user list).
- A valid session context must be established and maintained across all online navigation transfers within this function.
- Batch security initialization and profile administration jobs require appropriate scheduling authority and run independently of online sessions.
- Transaction-level access control is enforced via security profiles; users must be connected to the appropriate access group to invoke specific transactions.


> **Note:** The legacy system implements some of this processing as scheduled batch jobs. The modernized system may implement these requirements as batch, event-driven, or real-time processing provided the requirements below are met.


### Navigation Context

- From the main application menu, an administrator selects the user management option, which transfers control to the user list browse screen.
- From the user list browse screen, the administrator can select an individual user record and choose an action (update or delete), which transfers control to the corresponding user update or user delete screen.
- The administrator can navigate to the add-new-user screen from the user list or menu to create a new user account.
- The credit card list display is accessible as a separate menu option for card system users and supports selection of individual card records for detail viewing.
- All online screens provide a return path to the previous screen or main menu via standard navigation keys.

**OQ-NAV-01** *(Owner: Business Analyst)*: Is the credit card list display (COCRDLIC) intentionally grouped within User Security and Access Management, or does it belong to a separate Card Management workflow?

---

## 2. Credit Card List Display and Navigation


As a card system user, I want to search, filter, browse, and select credit card records from a paginated list so that I can view card details or navigate to update a specific card.

### Requirements

**Input Reception and Extraction**

REQ-F-001: [Event-driven] When the user submits the card list screen, the system shall receive the input and extract the account identifier, card number, and row selection codes (one per displayed row, up to 7 rows) into working variables for validation.

**Input Validation**

REQ-F-002: [Unwanted] If the account identifier contains non-numeric characters, the system shall mark the input as invalid and display an error to the user.

REQ-F-003: [Unwanted] If the card number contains non-numeric characters, the system shall mark the input as invalid and display an error to the user.

REQ-F-004: [State-driven] While validating row selection codes, the system shall verify that each row's selection code is blank, 'S' (view detail), or 'U' (update), and mark the input as invalid if any row contains a value other than these.

REQ-F-005: [Unwanted] If more than one row is marked with a selection code ('S' or 'U'), the system shall mark the input as invalid.

REQ-F-006: [Event-driven] When user input fails validation and no filter validation errors exist, the system shall read the first page of card records and redisplay the screen with the error message and the user's original input preserved for correction.

REQ-F-007: [Event-driven] When user input fails validation and filter validation errors exist, the system shall redisplay the screen with the error message and the user's original input preserved for correction without re-reading card records.

**Function Key Handling**

REQ-F-008: [Ubiquitous] The system shall translate user input keys to standardized function key identifiers, mapping PF13–PF24 to their PF1–PF12 equivalents.

REQ-F-009: [Unwanted] If the user presses a function key other than Enter, PF3, PF7, or PF8, the system shall treat the input as Enter and proceed with the default list action.

**Card Record Retrieval and Filtering**

REQ-F-010: [Event-driven] When a card record is read from the card data store (legacy: AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS), the system shall apply active filters: if the account identifier filter is active and the card's account identifier does not match, the record shall be excluded; if the card number filter is active and the card number does not match, the record shall be excluded; otherwise the record shall be included for display.

REQ-F-011: [Event-driven] When a card record passes the filter criteria, the system shall add the card number, account identifier, and card status to the next available row in the display buffer (up to 7 rows per page) and, if this is the first record on a fresh start, set the page number to 1.

REQ-F-012: [Event-driven] When the display buffer is full (7 rows), the system shall read one additional record to determine whether more pages exist and set the next-page indicator accordingly.

REQ-F-013: [Event-driven] When the end of the data store is reached during record retrieval, the system shall stop reading, set the next-page indicator to indicate no more pages, and if no records were retrieved on the first page, indicate that no records match the search criteria.

REQ-F-014: [Event-driven] When a read error occurs during card record retrieval, the system shall stop reading and display an error message containing the operation name, file name, response code, and reason code.

**Pagination**

REQ-F-015: [Complex] While more pages are available, when the user presses PF8 (page down), the system shall increment the page counter, read the next page of card records starting from the last card of the current page, and redisplay the screen.

REQ-F-016: [Complex] While the user is on a page other than the first page, when the user presses PF7 (page up), the system shall decrement the page counter, read the previous page of card records in backward order, and redisplay the screen.

REQ-F-017: [Event-driven] When the user presses PF7 (page up) while on the first page, the system shall redisplay the first page and present the error message 'NO PREVIOUS PAGES TO DISPLAY'.

REQ-F-018: [Event-driven] When the user presses PF8 (page down) and no more pages exist and the last page has already been shown, the system shall display the error message 'NO MORE PAGES TO DISPLAY'.

REQ-F-019: [Event-driven] When the user navigates using a key other than PF8, the system shall reset the last-page indicator to indicate that the last page has not yet been displayed.

**Informational Messages**

REQ-F-020: [Ubiquitous] The system shall display the informational message 'TYPE S FOR DETAIL, U TO UPDATE ANY RECORD' when no error condition exists and more pages are available or the last page has not yet been shown.

**Row Protection**

REQ-F-021: [Ubiquitous] The system shall set each card row to protected (read-only) if the row contains no card data, and to selectable if the row contains card data.

**Navigation to Card Detail (View)**

REQ-F-022: [Complex] While the user has selected exactly one card row marked 'S' (view), when the user presses Enter, the system shall populate the session context with the selected card's account identifier and card number, record the current screen location for return navigation, set the program state to re-entry, and transfer control to the card detail program passing the session context containing: originating transaction identifier, originating program name, user type, program context flag, selected card number (numeric, 16 digits), and selected account identifier (numeric, 11 digits).

**Navigation to Card Update**

REQ-F-023: [Complex] While the user has selected exactly one card row marked 'U' (update), when the user presses Enter, the system shall populate the session context with the transaction identifier, program name, user type, entry flag, selected card number (numeric, 16 digits), and selected account identifier (numeric, 11 digits), and transfer control to the card update program with the session context.

**Exit Navigation**

REQ-F-024: [Event-driven] When the user presses PF3 (exit) and the program is in its initial entry state, the system shall record the current transaction identifier and program name as the originating source, set the user type, capture the current screen map information, designate the main menu program as the destination, mark the context as a fresh entry, and transfer control to the main menu program passing the session context containing: originating transaction identifier (alphanumeric, 4 characters), originating program name (alphanumeric, 8 characters), destination transaction identifier (alphanumeric, 4 characters), destination program name (alphanumeric, 8 characters), user identifier (alphanumeric, 8 characters), user type (alphanumeric, 1 character), and program context flag (numeric, 1 digit).

REQ-F-025: [Complex] While the program is in a re-entry context from a different program, when the user presses PF3 (exit), the system shall reset all session state, reinitialize the program context as entry, reset pagination to the first page, read the first page of card records, and redisplay the screen.

**Context Reset on External Entry**

REQ-F-026: [Event-driven] When the program is entered from a different program, the system shall discard all prior pagination and navigation state and reset to the first page.

**Default Action**

REQ-F-027: [Event-driven] When no other navigation condition matches (unrecognized key or default), the system shall read the first page of card records and redisplay the screen.

### Open Questions

OQ-1: Rule 895e2922 states that on PF3 exit the user type is set to "standard user," but the program serves both admin and regular users. Should the exit navigation preserve the user's actual type from the session context rather than defaulting to standard user? — Owner: business analyst / security team

---

## 3. User List Browse and Administration


As a system administrator, I want to browse a paginated list of system users and select individual users for update or deletion so that I can manage user accounts efficiently.

### Requirements

REQ-F-028: [Event-driven] When the user list screen is accessed for the first time, the system shall retrieve the first page of user records from the user security data store (legacy: AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS) and display up to ten user records showing user ID, first name, last name, and user type.

REQ-F-029: [Ubiquitous] The system shall display the current date (formatted MM/DD/YY), current time (formatted HH:MM:SS), transaction identifier, and program name in the screen header each time the user list is rendered.

REQ-F-030: [Event-driven] When the administrator presses the page-forward key, the system shall load and display the next page of user records if additional records exist beyond the current page.

REQ-F-031: [Unwanted] If the administrator presses the page-forward key and no additional user records exist, the system shall display a message indicating the user is at the bottom of the list and re-display the current page.

REQ-F-032: [Event-driven] When the administrator presses the page-backward key and the current page is greater than one, the system shall load and display the previous page of user records.

REQ-F-033: [Unwanted] If the administrator presses the page-backward key while on the first page, the system shall display a message indicating the user is at the top of the list and re-display the current page.

REQ-F-034: [Unwanted] If the administrator presses any key other than Enter, page-forward, page-backward, or PF3, the system shall display an invalid key message and re-display the current screen.

REQ-F-035: [Event-driven] When the administrator presses Enter, the system shall scan all ten list rows in order and capture the selection code and corresponding user ID from the first row that contains a non-blank selection entry.

REQ-F-036: [Event-driven] When the administrator presses Enter and no row contains a non-blank selection entry, the system shall clear the selection indicator and selected user ID to indicate no selection was made.

REQ-F-037: [Complex] While a valid selection has been captured with both a non-blank selection code and user ID present, when the selection code is 'U' or 'u' (update), the system shall transfer control to the user details program, passing the selected user ID, the source program identity, the source transaction identifier, and a program context indicating new entry.

REQ-F-038: [Complex] While a valid selection has been captured with both a non-blank selection code and user ID present, when the selection code is 'D' or 'd' (delete), the system shall transfer control to the user delete program, passing the selected user ID, the source program identity, the source transaction identifier, and a program context indicating new entry.

REQ-F-039: [Unwanted] If the selection code is not a recognized value ('U', 'u', 'D', or 'd'), the system shall display an error message indicating an invalid selection code and re-display the user list screen.

REQ-F-040: [Unwanted] If an error occurs while reading user records during forward browsing, the system shall display an error message and re-display the current screen.

REQ-F-041: [Unwanted] If an error occurs while reading user records during backward browsing, the system shall display an error message and re-display the current screen.

REQ-F-042: [Event-driven] When the administrator presses PF3, the system shall transfer control to the administration menu, passing the authenticated user ID, user type, source program identity, and source transaction identifier in the session context.

REQ-F-043: [Unwanted] If the program is invoked without an active session context, the system shall transfer control to the sign-on screen.

REQ-F-044: [Unwanted] If the target program for navigation is empty or blank, the system shall default the navigation target to the sign-on screen.

### Open Questions

OQ-2: The selection code validation accepts 'U'/'u' for update and 'D'/'d' for delete. Are there additional valid selection codes (e.g., 'V' for view) that should be supported? — Owner: business analyst

OQ-3: The page size is fixed at ten user records per page. Should this be configurable in the modernized system? — Owner: UX team

---

## 4. Add New User Account


As a system administrator, I want to create new user accounts by entering user details and having them validated and stored so that new users can be granted access to the CardDemo application.

### Requirements

**Navigation and Session Management**

REQ-F-045: [Event-driven] When the application is invoked with no session context, the system shall navigate the user to the signon screen.

REQ-F-046: [Event-driven] When session context is received from the calling program, the system shall extract the user ID, user type, originating transaction identifier, and originating program identifier from the incoming session context and load them into the internal session structure.

REQ-F-047: [Complex] While the user is re-entering the current screen, when the user presses PF3, the system shall navigate to the administration screen, passing the current program identifier, current transaction identifier, authenticated user ID, and user type as session context.

REQ-F-048: [Ubiquitous] The system shall record the current program identifier and current transaction identifier as the navigation source and reset the program context flag to indicate a fresh entry before transferring control to any target program.

REQ-F-049: [Unwanted] If the target program is empty or contains only spaces, the system shall default the navigation destination to the signon screen.

REQ-F-050: [Ubiquitous] The system shall transfer control to the target program and pass the session context containing the authenticated user ID, user type, originating transaction identifier, and originating program identifier.

**User Input Routing**

REQ-F-051: [Event-driven] When the user presses Enter on the user registration screen, the system shall initiate validation of the entered user details.

REQ-F-052: [Event-driven] When the user presses PF4 on the user registration screen, the system shall clear all input fields (first name, last name, user ID, password, user type) and the error message, then redisplay the screen in a clean state.

REQ-F-053: [Unwanted] If the user presses any key other than Enter, PF3, or PF4, the system shall display the message 'Invalid key pressed. Please see below...' and redisplay the screen.

**Validation**

REQ-F-054: [Unwanted] If the first name field is empty or contains only spaces, the system shall reject the submission and display the error message 'First Name can NOT be empty...'.

REQ-F-055: [Unwanted] If the last name field is empty or contains only spaces, the system shall reject the submission and display the error message 'Last Name can NOT be empty...'.

REQ-F-056: [Unwanted] If the user ID field is empty or contains only spaces, the system shall reject the submission and display the error message 'User ID can NOT be empty...'.

REQ-F-057: [Unwanted] If the password field is empty or contains only spaces, the system shall reject the submission and display the error message 'Password can NOT be empty...'.

REQ-F-058: [Unwanted] If the user type field is empty or contains only spaces, the system shall reject the submission and display the error message 'User Type can NOT be empty...'.

**User Record Creation**

REQ-F-059: [State-driven] While all required fields (first name, last name, user ID, password, user type) pass validation, the system shall populate the user record with the user ID, first name, last name, password, and user type from the screen input and write the record to the user security data store (legacy: AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS) using the user ID as the key.

REQ-F-060: [Unwanted] If the user ID already exists in the user security data store, the system shall reject the submission and display the error message 'User ID already exist...'.

REQ-F-061: [Unwanted] If the write operation fails for a reason other than a duplicate key, the system shall display the error message 'Unable to Add User...'.

REQ-F-062: [Event-driven] When the user record is successfully written to the user security data store, the system shall clear all input fields, display a success message that includes the created user ID, and redisplay the screen ready for new input.

**Screen Header Population**

REQ-F-063: [Ubiquitous] The system shall populate the screen header with the current date in MM/DD/YY format, the current time in HH:MM:SS format, the transaction identifier, the program name, and the application screen titles before displaying the user registration screen.

### Open Questions

OQ-4: The rules reference "user type" as a required field but do not specify the valid values beyond the constants for admin and regular user. What are the exact permissible user type values and their business meanings? — Owner: security team

OQ-5: The password field is stored as entered with no mention of hashing, encryption, or complexity rules. Should the modernized system enforce password complexity requirements or encryption at rest? — Owner: security team

---

## 5. User Record Update


As a system administrator, I want to update existing user records (first name, last name, password, and user type) so that I can maintain accurate user information and access levels in the system.

### Requirements

REQ-F-064: [Event-driven] When the program is invoked without a session context, the system shall navigate to the signon program to require authentication before accessing user management functions.

REQ-F-065: [Event-driven] When the program is invoked with a session context indicating first entry and a pre-selected user ID is provided, the system shall pre-populate the user ID field and automatically initiate the user record lookup.

REQ-F-066: [Event-driven] When the program is invoked with a session context indicating first entry and no pre-selected user ID is provided, the system shall display the user update screen with the user ID field ready for input.

REQ-F-067: [Event-driven] When the user presses Enter, the system shall validate that the user ID field is not empty and, if valid, retrieve the corresponding user record from the user security data store (legacy: AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS) using the user ID as the key.

REQ-F-068: [Event-driven] When the user ID field is empty upon submission, the system shall reject the request and display the error message 'User ID can NOT be empty...'.

REQ-F-069: [Event-driven] When the user record is successfully retrieved, the system shall display the stored first name, last name, password, user type, and a message prompting the user to press PF5 to save updates.

REQ-F-070: [Event-driven] When the user record is not found in the user security data store, the system shall display the error message 'User ID NOT found...'.

REQ-F-071: [Event-driven] When an error other than not-found occurs during user record retrieval, the system shall display the error message 'Unable to lookup User...'.

REQ-F-072: [Event-driven] When the user presses PF5 to save updates, the system shall validate that user ID, first name, last name, password, and user type are all non-empty before proceeding.

REQ-F-073: [Event-driven] When any required field (user ID, first name, last name, password, or user type) is empty during update validation, the system shall reject the request and display the corresponding error message: 'User ID can NOT be empty...', 'First Name can NOT be empty...', 'Last Name can NOT be empty...', 'Password can NOT be empty...', or 'User Type can NOT be empty...'.

REQ-F-074: [Event-driven] When all update validations pass, the system shall retrieve the existing user record from the user security data store and compare each entered field (first name, last name, password, user type) against the stored values.

REQ-F-075: [Event-driven] When the entered first name differs from the stored first name, the system shall update the record with the new first name and mark the record as modified.

REQ-F-076: [Event-driven] When the entered last name differs from the stored last name, the system shall update the record with the new last name and mark the record as modified.

REQ-F-077: [Event-driven] When the entered password differs from the stored password, the system shall update the record with the new password and mark the record as modified.

REQ-F-078: [Event-driven] When the entered user type differs from the stored user type, the system shall update the record with the new user type and mark the record as modified.

REQ-F-079: [Event-driven] When at least one field has been marked as modified, the system shall persist the updated user record to the user security data store using the user ID as the key and display a success message 'User [user ID] has been updated ...'.

REQ-F-080: [Event-driven] When no fields have been modified after comparison, the system shall display the message 'Please modify to update ...' without persisting any changes.

REQ-F-081: [Event-driven] When the user record is not found during the persist operation, the system shall display the error message 'User ID NOT found...'.

REQ-F-082: [Event-driven] When an error other than not-found occurs during the persist operation, the system shall display the error message 'Unable to Update User...'.

REQ-F-083: [Event-driven] When the user presses PF4, the system shall clear all input fields (user ID, first name, last name, password, user type) and any pending messages, and redisplay the screen in its initial state.

REQ-F-084: [Event-driven] When the user presses PF3 (exit) and a calling program is recorded in the session context, the system shall navigate to that calling program, passing the current transaction ID, current program name as the originator, and the program context flag reset to indicate fresh entry.

REQ-F-085: [Event-driven] When the user presses PF3 (exit) and no calling program is recorded in the session context, the system shall navigate to the administration program, passing the current transaction ID, current program name as the originator, and the program context flag reset to indicate fresh entry.

REQ-F-086: [Event-driven] When the user presses PF12 (logout), the system shall navigate to the administration program regardless of any calling program context, passing the current transaction ID, current program name as the originator, and the program context flag reset to indicate fresh entry.

REQ-F-087: [Unwanted] If the destination program is empty or contains only spaces at the point of navigation, the system shall default the destination to the signon program.

REQ-F-088: [Event-driven] When the user presses any key other than Enter, PF3, PF4, PF5, or PF12, the system shall display the message 'Invalid key pressed. Please see below...'.

REQ-F-089: [Ubiquitous] The system shall display the current date (formatted MM/DD/YY), current time (formatted HH:MM:SS), transaction identifier, and program name in the screen header on every screen presentation.

REQ-F-090: [Event-driven] When the user presses PF3 to save updates, the system shall perform the same validation and update logic as PF5 (validate required fields, compare against stored record, and persist changes if modified).

### Open Questions

OQ-6: The rules indicate both PF3 and PF5 trigger the update workflow, yet PF3 is also described as the exit key that navigates away. The exact precedence (does PF3 save then exit, or only exit?) needs clarification. — Owner: business analyst / product owner

---

## 6. User Deletion


As a system administrator, I want to search for a user by ID, view their details, and delete them from the system so that I can remove user accounts that are no longer needed.

### Requirements

REQ-F-091: [Event-driven] When the application is invoked with no session context, the system shall navigate to the sign-on screen.

REQ-F-092: [Event-driven] When session context is provided and this is the first entry into the function, the system shall pre-populate the user ID field with any previously selected user ID from the session context (containing the authenticated user ID, user type, originating transaction identifier, and originating program identifier) and display the user deletion screen.

REQ-F-093: [Event-driven] When the administrator submits a user ID (presses Enter), the system shall validate that the user ID field is not empty; if empty, the system shall display an error message and highlight the user ID field.

REQ-F-094: [Complex] While the user ID has been validated as non-empty, when the system retrieves the user record from the user security data store (legacy: AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS), the system shall display the user's first name, last name, and user type, along with a confirmation prompt instructing the administrator to press PF5 to confirm deletion.

REQ-F-095: [Unwanted] If the user record is not found during lookup, the system shall display a "user not found" error message.

REQ-F-096: [Unwanted] If a system error occurs during user record lookup, the system shall display an error message indicating the failure.

REQ-F-097: [Event-driven] When the administrator presses PF5 to confirm deletion, the system shall validate that the user ID field is not empty; if empty, the system shall display an error message and highlight the user ID field.

REQ-F-098: [Complex] While the user ID has been validated as non-empty for deletion, when the system retrieves the user record with an update lock, the system shall proceed to delete the record from the user security data store.

REQ-F-099: [Unwanted] If the user record cannot be found or a system error occurs during the retrieval-for-deletion step, the system shall display an error message and abort the deletion.

REQ-F-100: [Complex] While the user record has been retrieved and locked, when the delete operation succeeds, the system shall clear all input fields (user ID, first name, last name, user type) and all messages, display a success message confirming the user has been deleted, and redisplay the screen.

REQ-F-101: [Unwanted] If a system error occurs during the delete operation, the system shall display an error message and redisplay the screen.

REQ-F-102: [Event-driven] When the administrator presses PF4, the system shall clear all input fields (user ID, first name, last name, user type) and all messages, and redisplay the screen in a clean state.

REQ-F-103: [Event-driven] When the administrator presses any key other than Enter, PF3, PF4, PF5, or PF12, the system shall display an "invalid key pressed" error message.

REQ-F-104: [Ubiquitous] The system shall display the screen header with the application title, transaction identifier, program name, current date formatted as MM/DD/YY, and current time formatted as HH:MM:SS.

REQ-F-105: [Complex] While the administrator is in an active session, when the administrator presses the exit key (PF3) and a previous program is recorded in the session context, the system shall transfer control to that previous program, passing the current program identifier, current transaction identifier, authenticated user ID, and user type in the session context.

REQ-F-106: [Complex] While the administrator is in an active session, when the administrator presses the exit key (PF3) and no previous program is recorded, the system shall transfer control to the administration screen, passing the current program identifier, current transaction identifier, authenticated user ID, and user type in the session context.

REQ-F-107: [Event-driven] When the administrator presses the main menu key (PF12), the system shall transfer control to the administration screen, passing the current program identifier, current transaction identifier, authenticated user ID, and user type in the session context.

REQ-F-108: [Ubiquitous] The system shall validate the target program before navigation; if no valid target program is set, the system shall default to the sign-on screen.

REQ-F-109: [Ubiquitous] The system shall reset the session re-entry flag to indicate initial entry before transferring control to the target program.

### Non-Functional Requirements

REQ-N-001: [Unwanted] If the user record is modified or deleted by another session between the confirmation lookup and the delete operation, the system shall detect the conflict (record not found) and display an error message rather than proceeding with an invalid deletion.

---

## 7. User Security Data Store Initialization


As a system administrator, I want the user security data store pre-populated with a defined set of administrator and regular user credentials so that the application has a baseline set of authenticated users available immediately upon deployment.

**Category:** setup
**Purpose:** Creates and populates the user security data store with initial user credential records for system authentication and authorization.
**Migration relevance:** Defines the initial data state for user authentication. The specific user records and their role classifications represent seed data required for the system to function.

### Requirements

REQ-F-110: [Event-driven] When the user security initialization process executes, the system shall replace any existing user security data store (legacy: AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS) with a freshly created instance, ensuring no stale credential data persists from prior initializations.

REQ-F-111: [Ubiquitous] The system shall populate the user security data store with exactly five administrator user records (user identifiers ADMIN001 through ADMIN005), each assigned the administrator password classification "PASSWORDA".

REQ-F-112: [Ubiquitous] The system shall populate the user security data store with exactly five standard user records (user identifiers USER0001 through USER0005), each assigned the standard user password classification "PASSWORDU".

REQ-F-113: [Ubiquitous] Each user record in the user security data store shall be uniquely keyed by an 8-character user identifier and shall contain the user identifier, user name, and password classification.

REQ-F-114: [Ubiquitous] The user security data store shall support indexed retrieval by user identifier to enable authentication lookups during application operation.

### Open Questions

OQ-7: The seed data uses "PASSWORDA" and "PASSWORDU" as password classifications rather than actual hashed credentials. Should the modernized system treat these as literal passwords, role-type indicators, or placeholders requiring a separate credential-setting step before go-live? — Owner: security team

OQ-8: Are the specific user identifiers (ADMIN001–ADMIN005, USER0001–USER0005) and associated names required to remain identical in the modernized system, or is only the count and role distribution (5 admins, 5 standard users) the business requirement? — Owner: application owner

---

## 8. User Security Data Store Initialization


As a system administrator, I want the user security credential repositories initialized with predefined administrator and regular user accounts so that the authentication system has a baseline set of users available for access control.

**Category:** setup
**Purpose:** Creates and populates the user security data stores with initial credential data for system authentication.
**Migration relevance:** Defines the initial user security data state required before the application can authenticate users.

### Requirements

REQ-F-115: [Event-driven] When the user security initialization process executes, the system shall populate the user security entry-sequenced data store (legacy: AWS.M2.CARDDEMO.USRSEC.VSAM.ESDS) and the user security relative-record data store (legacy: AWS.M2.CARDDEMO.USRSEC.VSAM.RRDS) with ten predefined user credential records: five administrative users (ADMIN001 through ADMIN005) and five regular users (USER0001 through USER0005), each containing a user identifier, first name, last name, and password.

REQ-F-116: [Ubiquitous] The system shall maintain two independent user security data stores — one supporting sequential access and one supporting direct record-number lookup — each containing identical user credential records.

REQ-F-117: [Event-driven] When the initialization process executes and a prior version of either user security data store exists, the system shall replace the existing data store contents entirely with the predefined credential set.

### Open Questions

OQ-9: Are the five administrative users (ADMIN001–ADMIN005) and five regular users (USER0001–USER0005) the complete initial user set, or should the modernized system support a configurable seed data mechanism? — Owner: security team

OQ-10: The legacy implementation stores credentials in plaintext within 80-byte fixed records. What credential storage and encryption standards apply to the modernized system? — Owner: security team

---

## 9. Security Profile and Group Membership Administration


As a security administrator, I want CICS transaction resources added to security profiles and users connected to appropriate access groups in batch so that transaction-level access control and group-based permissions are maintained without manual intervention.

**Category:** setup
**Purpose:** Provisions security access by adding a transaction to a CICS security profile and connecting a user to a development group.
**Migration relevance:** Defines security access state; the modernized system must ensure equivalent access control provisioning exists (e.g., via identity provider role assignments or access policy configuration).

### Requirements

REQ-F-118: [Event-driven] When the security provisioning job executes, the system shall add transaction CT02 as a member of the GCICSTRN CARD security profile, enabling transaction-level access control for users who hold that profile.

REQ-F-119: [Event-driven] When the security provisioning job executes, the system shall connect user AWSCODR to the M2APPDEV group, granting that user all resource access and permissions associated with the M2APPDEV group.

REQ-F-120: [Ubiquitous] The system shall log the outcome of each security provisioning action (profile member addition and group connection) for audit and verification purposes.

### Open Questions

OQ-11: Are the specific identifiers (transaction CT02, profile GCICSTRN CARD, user AWSCODR, group M2APPDEV) representative of a repeatable provisioning pattern that should be parameterized, or are they one-time setup values that will not recur in the modernized system? — Owner: security administration team

OQ-12: Should the modernized system replace RACF-based access control with an equivalent identity and access management mechanism (e.g., IAM roles, RBAC policies), and if so, what is the target access control model? — Owner: security architecture team

---

## 10. Job Dependencies

The following dependencies are inferred from shared data store access:

- **DUSRSECJ.jcl** (Section 7: User Security Data Store Initialization) → **ESDSRRDS.jcl** (Section 8: User Security Data Store Initialization) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANCATG.PS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANTYPE.PS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`, `AWS.M2.CARDDEMO.USRSEC.PS`)
- **ESDSRRDS.jcl** (Section 8: User Security Data Store Initialization) → **DUSRSECJ.jcl** (Section 7: User Security Data Store Initialization) (via `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX`, `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`, `AWS.M2.CARDDEMO.ESDSRRDS.PS`, `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANCATG.PS`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX`, `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`, `AWS.M2.CARDDEMO.TRANTYPE.PS`, `AWS.M2.CARDDEMO.TRXFL.SEQ`, `AWS.M2.CARDDEMO.USRSEC.PS`)

*These dependencies represent data flow ordering constraints. The implementation should ensure predecessor jobs complete before successors execute.*

---



## Shared Capability Dependencies

This capability depends on the following shared capabilities.
Do not reimplement their behavior — integrate with the shared service.

- **COSGN00C** (`_shared/COSGN00C/`) — As a user of the CardDemo application, I want to sign on with my credentials so that I am authenticated and directed to the appropriate application functions based on my role.


---

## 11. Security Hardening Requirements

**Based on Security Scan Findings:** #1, #7, #10, #16, #17, #19, #20, #25

### Session and Secret Management

REQ-SEC-001: [Ubiquitous] The system shall load the session secret key from a secure configuration source (environment variable or secrets manager) and shall never use hardcoded secret values in source code.

REQ-SEC-002: [Event-driven] When a user successfully authenticates, the system shall regenerate the session identifier before setting any authenticated session data to prevent session fixation attacks.

REQ-SEC-003: [Event-driven] When a user's user type is modified (e.g., admin to regular user), the system shall immediately invalidate all active sessions for that user ID to prevent privilege escalation via stale sessions.

REQ-SEC-004: [Event-driven] When a user account is deleted, the system shall immediately invalidate all active sessions for that user ID to prevent ghost session access.

REQ-SEC-005: [Ubiquitous] The system shall use cryptographically secure random values for all session tokens with a minimum entropy of 128 bits.

### Password Security

REQ-SEC-006: [Unwanted] If a password does not meet minimum complexity requirements (at least 8 characters, containing at least one uppercase letter, one lowercase letter, one digit, and one special character), the system shall reject the password and display an error message.

REQ-SEC-007: [Ubiquitous] The system shall hash passwords using a secure one-way hashing algorithm (bcrypt, scrypt, or Argon2) with appropriate work factor before storage, and shall never store passwords in plaintext or reversible encryption.

REQ-SEC-008: [Ubiquitous] The system shall preserve the original case of passwords during authentication and shall not perform case normalization (uppercase/lowercase conversion) that reduces password entropy.

REQ-SEC-009: [Unwanted] If a password field is submitted empty or contains only whitespace during user creation or update, the system shall reject the submission with an error message.

REQ-SEC-010: [Event-driven] When the system is initialized or deployed, the system shall not include any default user accounts with predictable credentials (username equals password, or well-known default passwords).

REQ-SEC-011: [Ubiquitous] The system shall enforce a password change requirement for any accounts created with temporary or system-generated passwords on first login.

### User Type Validation

REQ-SEC-012: [Unwanted] If the user type field contains a value other than the explicitly allowed values ('A' for administrator, 'U' for regular user), the system shall reject the submission and display an error message indicating valid user type values.

REQ-SEC-013: [Ubiquitous] The system shall validate user type values against a defined allowlist in all user creation and update operations, rejecting any value not in the allowlist.

REQ-SEC-014: [Ubiquitous] The system shall use consistent user type validation logic across all modules (UI, API, batch) to prevent authorization bypass through inconsistent checks.

### Authentication Error Handling

REQ-SEC-015: [Event-driven] When authentication fails due to invalid user ID or incorrect password, the system shall display a generic error message (e.g., "Invalid credentials") that does not reveal whether the user ID exists or the password was incorrect.

REQ-SEC-016: [Ubiquitous] The system shall log authentication failures with full details (user ID, timestamp, source IP) to an internal audit log for security monitoring, while displaying only generic messages to users.

REQ-SEC-017: [Event-driven] When multiple failed authentication attempts occur from the same user ID or source IP within a defined time window (e.g., 5 failures in 15 minutes), the system shall implement progressive delays or temporary account lockout to prevent brute-force attacks.

### Session Lifecycle Management

REQ-SEC-018: [Ubiquitous] The system shall set a maximum session lifetime (e.g., 8 hours for regular users, 4 hours for administrators) after which the session automatically expires and requires re-authentication.

REQ-SEC-019: [Ubiquitous] The system shall set a session idle timeout (e.g., 30 minutes) after which inactive sessions automatically expire.

REQ-SEC-020: [Event-driven] When a session expires due to timeout or maximum lifetime, the system shall clear all session data and redirect the user to the sign-on screen with a message indicating the session has expired.

### Authorization Verification

REQ-SEC-021: [Ubiquitous] The system shall verify user authorization by querying the current user record from the data store on each privileged operation, rather than relying solely on session data, to ensure authorization changes take effect immediately.

REQ-SEC-022: [Event-driven] When an administrator attempts to delete their own user account, the system shall prevent the deletion and display an error message indicating self-deletion is not permitted.

### Non-Functional Security Requirements

REQ-SEC-N-001: [Ubiquitous] All user management operations (create, update, delete) shall be logged to an audit trail including: timestamp, administrator user ID, operation type, target user ID, and changed fields.

REQ-SEC-N-002: [Ubiquitous] The system shall implement rate limiting on authentication endpoints to prevent automated brute-force attacks (e.g., maximum 10 authentication attempts per minute per source IP).

REQ-SEC-N-003: [Ubiquitous] Session cookies shall be configured with Secure flag (HTTPS only), HttpOnly flag (not accessible to JavaScript), and SameSite=Strict or Lax to prevent session hijacking and CSRF attacks.

### Open Questions

OQ-SEC-01: What is the required password complexity policy for the organization (minimum length, character requirements, password history, expiration period)? — Owner: Security team

OQ-SEC-02: What is the appropriate session timeout duration for different user types (regular users vs. administrators)? — Owner: Security team / Business analyst

OQ-SEC-03: Should the system integrate with an external identity provider (LDAP, Active Directory, SAML, OAuth) or maintain its own user credential store? — Owner: Security architecture team

OQ-SEC-04: What is the required account lockout policy (number of failed attempts, lockout duration, unlock mechanism)? — Owner: Security team
