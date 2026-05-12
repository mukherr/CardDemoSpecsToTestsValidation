## 1. Date Validation Service

As any system component requiring date validation, I want to submit a date string with its format specification and receive a structured validation result so that I can determine whether the date is valid and, if not, understand the specific reason for failure.

### Requirements

REQ-F-001: [Event-driven] When the date validation service is invoked, the system shall accept a date string, a date format specification, and a result buffer from the caller.

REQ-F-002: [Ubiquitous] The system shall convert the supplied date string according to the provided format specification and compute the corresponding Lillian day number to determine validity.

REQ-F-003: [Ubiquitous] The system shall extract the severity code and message code from the conversion feedback and store them in the result message structure along with the original date string.

REQ-F-004: [Complex] While the date conversion has returned a feedback code, when the feedback code indicates the date is valid, the system shall set the validation status text to 'Date is valid'.

REQ-F-005: [Complex] While the date conversion has returned a feedback code, when the feedback code indicates insufficient data was provided, the system shall set the validation status text to 'Insufficient'.

REQ-F-006: [Complex] While the date conversion has returned a feedback code, when the feedback code indicates a bad date value, the system shall set the validation status text to 'Datevalue error'.

REQ-F-007: [Complex] While the date conversion has returned a feedback code, when the feedback code indicates an invalid era, the system shall set the validation status text to 'Invalid Era'.

REQ-F-008: [Complex] While the date conversion has returned a feedback code, when the feedback code indicates an unsupported date range, the system shall set the validation status text to 'Unsupp. Range'.

REQ-F-009: [Complex] While the date conversion has returned a feedback code, when the feedback code indicates an invalid month, the system shall set the validation status text to 'Invalid month'.

REQ-F-010: [Complex] While the date conversion has returned a feedback code, when the feedback code indicates a malformed picture string, the system shall set the validation status text to 'Bad Pic String'.

REQ-F-011: [Complex] While the date conversion has returned a feedback code, when the feedback code indicates non-numeric data in the date string, the system shall set the validation status text to 'Nonnumeric data'.

REQ-F-012: [Complex] While the date conversion has returned a feedback code, when the feedback code indicates the year in era is zero, the system shall set the validation status text to 'YearInEra is 0'.

REQ-F-013: [Complex] While the date conversion has returned a feedback code, when the feedback code does not match any of the explicitly recognized conditions, the system shall set the validation status text to 'Date is invalid'.

REQ-F-014: [Ubiquitous] The system shall return the complete result structure containing the severity code, message code, validation status text, and original date string to the caller.

### Open Questions

OQ-001: The specific numeric feedback codes that map to each validation failure condition are not documented in the business rules — are these codes standardized (e.g., Language Environment CEEDATM feedback codes) and should they be preserved as-is in the modernized implementation? — Owner: platform engineering team

OQ-002: The format specification accepted by this service (picture string) — should the modernized service support the same set of date format patterns, or should it be aligned to a modern standard (e.g., ISO 8601 patterns)? — Owner: architecture team