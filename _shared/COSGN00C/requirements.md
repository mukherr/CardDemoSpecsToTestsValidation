## 1. User Authentication and Role-Based Routing

As a user of the CardDemo application, I want to sign on with my credentials so that I am authenticated and directed to the appropriate application functions based on my role.

### Requirements

REQ-F-001: [Event-driven] When the sign-on screen is invoked with no prior session context, the system shall display the sign-on screen with the input focus positioned on the user ID field.

REQ-F-002: [Ubiquitous] The system shall retrieve the current system date and time and display them on the sign-on screen formatted as MM-DD-YY (date) and HH-MM-SS (time).

REQ-F-003: [Ubiquitous] The system shall retrieve the application identifier and system identifier from the runtime environment and display them on the sign-on screen header along with the application title, transaction identifier, and program name.

REQ-F-004: [Event-driven] When the user presses a key other than Enter or the exit key (PF3), the system shall display the error message "Invalid key pressed" and redisplay the sign-on screen.

REQ-F-005: [Event-driven] When the user submits the sign-on form, the system shall validate that the user ID field is not empty; if empty, the system shall display the error message "Please enter User ID ..." and return focus to the user ID field.

REQ-F-006: [Event-driven] When the user submits the sign-on form, the system shall validate that the password field is not empty; if empty, the system shall display the error message "Please enter Password ..." and return focus to the password field.

REQ-F-007: [Ubiquitous] The system shall convert the user ID and password to uppercase before performing credential validation, ensuring case-insensitive matching.

REQ-F-008: [State-driven] While no input validation errors have been detected, the system shall retrieve the user record from the user security data store (legacy: AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS) using the uppercase user ID as the lookup key.

REQ-F-009: [Event-driven] When the user record retrieval returns a not-found condition, the system shall display the error message "User not found. Try again ..." and return focus to the user ID field.

REQ-F-010: [Event-driven] When the user record retrieval encounters an error condition other than not-found, the system shall display the error message "Unable to verify the User ..." and return focus to the user ID field.

REQ-F-011: [Event-driven] When the user record is found and the stored password does not match the entered password, the system shall display the error message "Wrong Password. Try again ..." and return focus to the password field.

REQ-F-012: [Complex] While the user record has been successfully retrieved, when the stored password matches the entered password and the user type is administrator ('A'), the system shall populate the session context with the authenticated user ID, user type, and program routing information, then transfer control to the administrator program.

REQ-F-013: [Complex] While the user record has been successfully retrieved, when the stored password matches the entered password and the user type is standard user ('U'), the system shall populate the session context with the authenticated user ID, user type, and program routing information, then transfer control to the standard user program.

REQ-F-014: [Event-driven] When the sign-on screen is redisplayed after a validation or authentication error, the system shall populate the screen header with current date, time, application identifier, and system identifier, and display the applicable error message.

### Open Questions

OQ-001: The rules specify routing for user types 'A' (administrator) and 'U' (standard user). What behavior should occur if a valid user record contains a user type value other than 'A' or 'U'? — Owner: security/identity team

OQ-002: The session context passed upon successful authentication includes user ID, user type, and "program routing information." What specific program routing data elements are included beyond user ID and user type? — Owner: application architecture team