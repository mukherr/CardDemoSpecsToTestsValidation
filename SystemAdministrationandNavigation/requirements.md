# System Administration and Navigation — Requirements

## 1. Global Preconditions

- Users must be authenticated via the sign-on subroutine before accessing any menu or service within this function.
- Session context must contain the authenticated user identifier and assigned role (regular user or administrator).
- Administrator-only screens are restricted to users whose session context indicates an administrator role; regular users are routed to the standard main menu.
- All screens display the current system date and time, obtained via the date/time inquiry service.


### Navigation Context

- After successful sign-on, the system evaluates the user's role: administrators are routed to the administrative menu; regular users are routed to the main menu.
- From the main menu, a regular user selects a numbered option corresponding to an application function; the system transfers control to the selected function based on the user's access level.
- From the administrative menu, an administrator selects a numbered option corresponding to an administrative function; the system transfers control to the selected function.
- The date/time inquiry service is invoked as a background request (via message queue) by screens that need formatted date and time; it is not directly navigable by users.
- Selecting an invalid or unauthorized option on either menu displays an error message and returns the user to the same menu.

- **OQ-NAV-01** *(Owner: Business/Architecture)*: Can an administrator also access regular-user main menu functions, or are the two menus mutually exclusive?

---

## 2. Administrative Menu Display and Navigation


As an administrator user, I want to access an administrative menu that displays available functions and routes me to the selected function so that I can perform administrative tasks within the application.

### Requirements

REQ-F-001: [Event-driven] When the application is invoked with no session context, the system shall transfer control to the signon screen.

REQ-F-002: [Event-driven] When the application is invoked with session context from a calling program, the system shall load the session context (including user identity, account information, and navigation state) into working memory and proceed to display the administrative menu.

REQ-F-003: [Event-driven] When the program is entered for the first time (initial entry), the system shall clear the screen buffer and display the administrative menu screen.

REQ-F-004: [Event-driven] When the program is re-entered after returning from a called administrative function, the system shall redisplay the administrative menu screen.

REQ-F-005: [Ubiquitous] The system shall populate the menu screen header with the transaction identifier, program name, current date formatted as MM-DD-YY, current time formatted as HH-MM-SS, and predefined title lines.

REQ-F-006: [State-driven] While the menu option list contains up to 12 available options, the system shall iterate through each option and format it as '[number]. [name]' in the corresponding screen display field (fields 1 through 12), displaying only options up to the configured maximum option count.

REQ-F-007: [Ubiquitous] The system shall display the formatted administrative menu screen to the user, clearing the display area before presenting the new content.

REQ-F-008: [Complex] While the menu screen has been displayed and the user has interacted with it, when the user presses the Enter key, the system shall process the selected menu option.

REQ-F-009: [Complex] While the menu screen has been displayed and the user has interacted with it, when the user presses any key other than Enter or the exit key (PF3), the system shall display an error message indicating an invalid key was pressed and redisplay the menu.

REQ-F-010: [Event-driven] When the user submits a menu option selection, the system shall extract the option value from the input field, trim trailing spaces, and normalize remaining spaces to zeros to produce a numeric option value.

REQ-F-011: [Event-driven] When the user enters a menu option number, the system shall validate that the input is numeric, is not zero, and does not exceed the maximum number of available options (up to 6); if validation fails, the system shall display a validation error message and redisplay the menu.

REQ-F-012: [Complex] While a valid menu option has been selected and no validation errors have occurred, when the corresponding target program is available, the system shall update the session context to record the current program and transaction as the source, reset the re-entry flag to indicate initial entry into the target program, and transfer control to the target administrative program passing the session context (including user identity, account information, and navigation history).

REQ-F-013: [Unwanted] If the program for the selected menu option is not available or installed, the system shall display a message indicating that the selected option is not currently installed and redisplay the menu.

REQ-F-014: [Complex] While the user is interacting with the administrative menu, when the user presses the exit key (PF3), the system shall set the target program to the standard signon screen and transfer control to it.

REQ-F-015: [Ubiquitous] The system shall default the target program to the standard signon screen if the target program field is empty or contains only spaces before transferring control.

### Open Questions

OQ-1: The validation rules reference both a maximum of 12 display options and a maximum valid option count of 6 for input validation. Is the maximum selectable option count always 6, or is it dynamically determined by the number of configured options? — Owner: application configuration team

---

## 3. Date and Time Inquiry Service


As an application component, I want to submit a date/time request to a message queue and receive the current system date and time in a formatted reply so that I can display or use accurate date/time information without direct system clock access.

### Requirements

REQ-F-016: [Event-driven] When the service is started via a trigger message, the system shall extract the input queue name from the trigger message and set the reply queue name to 'CARD.DEMO.REPLY.DATE'.

REQ-F-017: [Unwanted] If the trigger message cannot be retrieved at startup, the system shall record the failure details and transition to error handling.

REQ-F-018: [Event-driven] When initialization completes successfully, the system shall open the reply queue 'CARD.DEMO.REPLY.DATE' for output and save the queue handle.

REQ-F-019: [Event-driven] When initialization completes successfully, the system shall open the error queue 'CARD.DEMO.ERROR' for output and save the queue handle.

REQ-F-020: [Event-driven] When the error queue is opened successfully, the system shall open the input queue (identified from the trigger message) for shared input operations and save the queue handle.

REQ-F-021: [Complex] While the input queue is open and messages may be available, when the system is ready to retrieve the next message, the system shall retrieve a message from the input queue with a 5-second wait timeout, extracting the message ID, correlation ID, reply-to queue name, and message content.

REQ-F-022: [Event-driven] When a request message is successfully retrieved, the system shall obtain the current system time and format it as a reply containing the date in MM-DD-YYYY format and the time in HH:MM:SS format.

REQ-F-023: [Event-driven] When the date/time reply message is ready to send, the system shall send the reply to the reply queue, preserving the original message ID and correlation ID from the inbound request.

REQ-F-024: [Event-driven] When a reply message is successfully sent, the system shall commit the current transaction to persist the message processing.

REQ-F-025: [State-driven] While messages remain available in the input queue, the system shall continue retrieving and processing messages in a loop; when no more messages are available (5-second wait expires with no message), the system shall exit the loop and transition to termination.

REQ-F-026: [Event-driven] When an error occurs during queue operations or request processing, the system shall send an error message to the error queue containing the error source identifier, condition code, reason code, and queue name.

REQ-F-027: [Event-driven] When the system is terminating and the input queue is open, the system shall close the input queue.

REQ-F-028: [Event-driven] When the system is terminating and the reply queue is open, the system shall close the reply queue.

REQ-F-029: [Event-driven] When the system is terminating and the error queue is open, the system shall close the error queue.

REQ-F-030: [State-driven] While the system is in termination, the system shall evaluate which queues were successfully opened and close only those queues.

### Non-Functional Requirements

REQ-N-001: [Ubiquitous] The system shall commit each request-reply cycle as an individual transaction, ensuring that message retrieval and reply delivery are atomically persisted per message.

### Open Questions

OQ-2: The reply message buffer is fixed at 1000 bytes, but the formatted date/time content is significantly smaller. Should the reply message size be constrained to the actual content length, or is the 1000-byte buffer a contractual interface size? — Owner: integration team

OQ-3: The input queue name is dynamically obtained from the trigger message, but the reply queue name 'CARD.DEMO.REPLY.DATE' is hardcoded. Should the reply queue be configurable or should the reply-to queue from the inbound message descriptor be used instead? — Owner: messaging architecture team

---

## 4. Main Menu Navigation


As a regular user, I want to view available application functions on a main menu and select an option so that I can navigate to the desired feature based on my access level.

### Requirements

REQ-F-031: [Event-driven] When the application is invoked with no session context, the system shall navigate the user to the sign-on screen.

REQ-F-032: [Event-driven] When session context data is provided in the incoming communication, the system shall load the session context (including user identity, account details, and navigation history) into the working session for use during menu processing.

REQ-F-033: [Event-driven] When the program is entered for the first time with valid session context, the system shall display the main menu screen to the user.

REQ-F-034: [Ubiquitous] The system shall populate the menu screen header with the application title, transaction identifier, program name, current date formatted as MM-DD-YY, and current time formatted as HH:MM:SS.

REQ-F-035: [State-driven] While the menu contains up to 11 available options, the system shall build and display a numbered list of menu options, formatting each as 'number. option name' in the corresponding screen position.

REQ-F-036: [Ubiquitous] The system shall clear any previous error state and reset the message display area at the start of each menu interaction.

REQ-F-037: [Event-driven] When the user submits input from the menu screen, the system shall capture the user's option selection from the input.

REQ-F-038: [Event-driven] When the user presses the Enter key on the menu screen, the system shall process the selected menu option.

REQ-F-039: [Unwanted] If the user presses any key other than Enter or the exit key, the system shall display an invalid key message and redisplay the menu.

REQ-F-040: [Complex] While the user is in a re-entry state (session context indicates a previous interaction), when the user presses the exit key, the system shall navigate the user to the sign-on screen.

REQ-F-041: [Ubiquitous] The system shall extract the menu option input, trim trailing spaces, replace leading spaces with zeros, and convert the result to a numeric option identifier.

REQ-F-042: [Unwanted] If the menu option is non-numeric, exceeds 11, or equals zero, the system shall set an error state, display the message 'Please enter a valid option number...', and redisplay the menu.

REQ-F-043: [Unwanted] If the selected menu option is marked as admin-only and the current user is not an administrator, the system shall set an error state, display the message 'No access - Admin Only option... ', and redisplay the menu.

REQ-F-044: [Complex] While a valid menu option has been selected and access control has passed, when the target program for the option is not available, the system shall display an error message indicating that the option is not installed and redisplay the menu.

REQ-F-045: [Event-driven] When a user selects a menu option with a placeholder program name (name starts with 'DUMMY'), the system shall display a message indicating that the option is coming soon and redisplay the menu.

REQ-F-046: [Event-driven] When a valid menu option is selected and the user has permission to access it and the target program is available, the system shall update the session context with the current transaction identifier and program name as the navigation source, reset the program context to indicate first entry into the target, and transfer control to the selected program passing the session context (including user identity, account details, and navigation history).

REQ-F-047: [Ubiquitous] The system shall place any accumulated error or status message into the error message field on the menu screen before displaying it to the user.

### Open Questions

OQ-4: The maximum menu option count is stated as 11 in some rules and the validation checks "exceeds 11," but one rule references "up to 11 options" while the menu structure may support 12 display positions. What is the definitive maximum number of menu options? — Owner: application design team

---



## Shared Capability Dependencies

This capability depends on the following shared capabilities.
Do not reimplement their behavior — integrate with the shared service.

- **COSGN00C** (`_shared/COSGN00C/`) — As a user of the CardDemo application, I want to sign on with my credentials so that I am authenticated and directed to the appropriate application functions based on my role.
