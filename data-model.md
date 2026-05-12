# Data Model — Cross-Cutting Entity View

This document consolidates all data entities across all business functions into a single reference. Each entity appears once with its full field list, all access paths, and every function that reads or writes it. This view is technology-neutral — it describes *what* data exists and *how* it is accessed, not *where* it should be stored.

*Generated from 13 function(s): BillPaymentProcessing, CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, FinancialProcessingandInterestCalculation, PaymentAuthorizationManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemAdministrationandNavigation, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement.*

---

## 1. Acctdata store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS` |
| Legacy Type | VSAM KSDS |
| Record Size | 300 bytes |
| Primary Access Key | ID (11 bytes, position 1) |
| Access Pattern | Keyed lookup + update |
| Reader Programs | CBACT01C, CBACT04C, CBSTM03B, CBTRN02C, COACCT01, COACTUPC, COACTVWC, COBIL00C, COPAUA0C, COPAUS0C |
| Writer Programs | CBACT04C, CBTRN02C, COACTUPC, COBIL00C, IDCAMS |
| Reader Functions | BillPaymentProcessing, CreditCardAccountManagement, FinancialProcessingandInterestCalculation, PaymentAuthorizationManagement, StatementGenerationandCustomerCommunication, TransactionProcessingandAuthorization |
| Writer Functions | BillPaymentProcessing, CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, FinancialProcessingandInterestCalculation, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

**Fields** (from copybook CVACT01Y):

| Field | Type | Length | Offset |
|-------|------|--------|--------|
| ID | Numeric | 11 | 1 |
| Active Status | Alphanumeric | 1 | 12 |
| Curr Balance | Decimal | 12 | 13 |
| Credit Limit | Decimal | 12 | 25 |
| Cash Credit Limit | Decimal | 12 | 37 |
| Open Date | Alphanumeric | 10 | 49 |
| Expiraion Date | Alphanumeric | 10 | 59 |
| Reissue Date | Alphanumeric | 10 | 69 |
| Curr Cyc Credit | Decimal | 12 | 79 |
| Curr Cyc Debit | Decimal | 12 | 91 |
| Address Zip | Alphanumeric | 10 | 103 |
| Group ID | Alphanumeric | 10 | 113 |

---

## 2. Acctdata store (ARRYPS)

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.ACCTDATA.ARRYPS` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | None |
| Writer Programs | CBACT01C |
| Reader Functions | None |
| Writer Functions | CreditCardAccountManagement |

*No field definitions available (no copybook found).*

---

## 3. Acctdata store (PSCOMP)

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.ACCTDATA.PSCOMP` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | None |
| Writer Programs | CBACT01C |
| Reader Functions | None |
| Writer Functions | CreditCardAccountManagement |

*No field definitions available (no copybook found).*

---

## 4. Acctdata store (VBPS)

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.ACCTDATA.VBPS` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | None |
| Writer Programs | CBACT01C |
| Reader Functions | None |
| Writer Functions | CreditCardAccountManagement |

*No field definitions available (no copybook found).*

---

## 5. Bmscmp store (jcl-&&TEMPM)

| Property | Value |
|---|---|
| Legacy Identifier | `BMSCMP.jcl-&&TEMPM` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | None |
| Writer Programs | IEBGENER |
| Reader Functions | None |
| Writer Functions | ReferenceDataManagement, SystemInfrastructureandTechnicalServices, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 6. Carddata store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS` |
| Legacy Type | VSAM KSDS |
| Record Size | 150 bytes |
| Primary Access Key | Number (16 bytes, position 1) |
| Access Pattern | Keyed lookup + update |
| Reader Programs | CBACT02C, COCRDLIC, COCRDSLC, COCRDUPC, IDCAMS |
| Writer Programs | COCRDUPC, IDCAMS |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

**Alternate access paths:**

- `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX.PATH` (VSAM PATH) — provides lookup by alternate key

**Fields** (from copybook CVACT02Y):

| Field | Type | Length | Offset |
|-------|------|--------|--------|
| Number | Alphanumeric | 16 | 1 |
| Acct ID | Numeric | 11 | 17 |
| Cvv Code | Numeric | 3 | 28 |
| Embossed Name | Alphanumeric | 50 | 31 |
| Expiraion Date | Alphanumeric | 10 | 81 |
| Active Status | Alphanumeric | 1 | 91 |

---

## 7. Carddata store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.CARDDATA.VSAM.AIX` |
| Legacy Type | VSAM-AIX |
| Record Size | Unknown |
| Primary Access Key | [INFERRED: Alternate index — key field determined by AIX definition] |
| Access Pattern | Keyed lookup via alternate index |
| Reader Programs | IDCAMS |
| Writer Programs | IDCAMS |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 8. Cardxref store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS` |
| Legacy Type | VSAM KSDS |
| Record Size | 50 bytes |
| Primary Access Key | Card Number (16 bytes, position 1) |
| Access Pattern | Keyed lookup + update |
| Reader Programs | CBACT03C, CBACT04C, CBSTM03B, CBTRN02C, CBTRN03C, COACTUPC, COACTVWC, COBIL00C, COPAUA0C, COPAUS0C, COTRN02C, IDCAMS |
| Writer Programs | IDCAMS |
| Reader Functions | BillPaymentProcessing, CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, FinancialProcessingandInterestCalculation, PaymentAuthorizationManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

**Alternate access paths:**

- `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX.PATH` (VSAM PATH) — provides lookup by alternate key

**Fields** (from copybook CVACT03Y):

| Field | Type | Length | Offset |
|-------|------|--------|--------|
| Card Number | Alphanumeric | 16 | 1 |
| Cust ID | Numeric | 9 | 17 |
| Acct ID | Numeric | 11 | 26 |

---

## 9. Cardxref store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.CARDXREF.VSAM.AIX` |
| Legacy Type | VSAM-AIX |
| Record Size | Unknown |
| Primary Access Key | [INFERRED: Alternate index — key field determined by AIX definition] |
| Access Pattern | Keyed lookup via alternate index |
| Reader Programs | IDCAMS |
| Writer Programs | IDCAMS |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 10. Ciccmp store (jcl-&&COPYLINK)

| Property | Value |
|---|---|
| Legacy Identifier | `CICCMP.jcl-&&COPYLINK` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | None |
| Writer Programs | IEBGENER |
| Reader Functions | None |
| Writer Functions | ReferenceDataManagement, SystemInfrastructureandTechnicalServices, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 11. Custdata store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS` |
| Legacy Type | VSAM KSDS |
| Record Size | 500 bytes |
| Primary Access Key | ID (9 bytes, position 1) |
| Access Pattern | Keyed lookup + update |
| Reader Programs | CBCUS01C, CBSTM03B, COACTUPC, COACTVWC, COPAUA0C, COPAUS0C |
| Writer Programs | COACTUPC, IDCAMS |
| Reader Functions | CreditCardAccountManagement, CustomerDataManagement, PaymentAuthorizationManagement, StatementGenerationandCustomerCommunication, TransactionProcessingandAuthorization |
| Writer Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

**Fields** (from copybook CVCUS01Y):

| Field | Type | Length | Offset |
|-------|------|--------|--------|
| ID | Numeric | 9 | 1 |
| First Name | Alphanumeric | 25 | 10 |
| Middle Name | Alphanumeric | 25 | 35 |
| Last Name | Alphanumeric | 25 | 60 |
| Address Line 1 | Alphanumeric | 50 | 85 |
| Address Line 2 | Alphanumeric | 50 | 135 |
| Address Line 3 | Alphanumeric | 50 | 185 |
| Address State Code | Alphanumeric | 2 | 235 |
| Address Country Code | Alphanumeric | 3 | 237 |
| Address Zip | Alphanumeric | 10 | 240 |
| Phone Number 1 | Alphanumeric | 15 | 250 |
| Phone Number 2 | Alphanumeric | 15 | 265 |
| Ssn | Numeric | 9 | 280 |
| Govt Issued ID | Alphanumeric | 20 | 289 |
| Dob Yyyy Mm Dd | Alphanumeric | 10 | 309 |
| Eft Account ID | Alphanumeric | 10 | 319 |
| Pri Card Holder Indicator | Alphanumeric | 1 | 329 |
| Fico Credit Score | Numeric | 3 | 330 |

---

## 12. Dalyrejs store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.DALYREJS` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | None |
| Writer Programs | CBTRN02C |
| Reader Functions | None |
| Writer Functions | TransactionProcessingandAuthorization |

*No field definitions available (no copybook found).*

---

## 13. Discgrp store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS` |
| Legacy Type | VSAM KSDS |
| Record Size | 50 bytes |
| Primary Access Key | Acct Group ID (10 bytes, position 1) |
| Access Pattern | Keyed lookup + update |
| Reader Programs | CBACT04C |
| Writer Programs | IDCAMS |
| Reader Functions | FinancialProcessingandInterestCalculation |
| Writer Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

**Fields** (from copybook CVTRA02Y):

| Field | Type | Length | Offset |
|-------|------|--------|--------|
| Acct Group ID | Alphanumeric | 10 | 1 |
| Tran Type Code | Alphanumeric | 2 | 11 |
| Tran Category Code | Numeric | 4 | 13 |
| Int Rate | Decimal | 6 | 17 |

---

## 14. Discgrp store (BKUP)

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.DISCGRP.BKUP` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | None |
| Writer Programs | IEBGENER |
| Reader Functions | None |
| Writer Functions | ReferenceDataManagement, SystemInfrastructureandTechnicalServices, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 15. Esdsrrds store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.ESDSRRDS.PS` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | IDCAMS |
| Writer Programs | IEBGENER |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | ReferenceDataManagement, SystemInfrastructureandTechnicalServices, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 16. Imsmqcmp store (jcl-&&COPYIMS)

| Property | Value |
|---|---|
| Legacy Identifier | `IMSMQCMP.jcl-&&COPYIMS` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | None |
| Writer Programs | IEBGENER |
| Reader Functions | None |
| Writer Functions | ReferenceDataManagement, SystemInfrastructureandTechnicalServices, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 17. Imsmqcmp store (jcl-&&COPYLINK)

| Property | Value |
|---|---|
| Legacy Identifier | `IMSMQCMP.jcl-&&COPYLINK` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | None |
| Writer Programs | IEBGENER |
| Reader Functions | None |
| Writer Functions | ReferenceDataManagement, SystemInfrastructureandTechnicalServices, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 18. Imsmqcmp store (jcl-&&COPYMQ)

| Property | Value |
|---|---|
| Legacy Identifier | `IMSMQCMP.jcl-&&COPYMQ` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | None |
| Writer Programs | IEBGENER |
| Reader Functions | None |
| Writer Functions | ReferenceDataManagement, SystemInfrastructureandTechnicalServices, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 19. Statemnt store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.STATEMNT.PS` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | None |
| Writer Programs | CBSTM03A |
| Reader Functions | None |
| Writer Functions | StatementGenerationandCustomerCommunication |

*No field definitions available (no copybook found).*

---

## 20. Statemnt store (HTML)

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.STATEMNT.HTML` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | None |
| Writer Programs | CBSTM03A |
| Reader Functions | None |
| Writer Functions | StatementGenerationandCustomerCommunication |

*No field definitions available (no copybook found).*

---

## 21. Systran store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.SYSTRAN` |
| Legacy Type | GDG Base |
| Record Size | 350 bytes |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | SORT |
| Writer Programs | CBACT04C |
| Reader Functions | ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, TransactionProcessingandAuthorization |
| Writer Functions | FinancialProcessingandInterestCalculation |

**Fields** (from copybook CVTRA05Y):

| Field | Type | Length | Offset |
|-------|------|--------|--------|
| ID | Alphanumeric | 16 | 1 |
| Type Code | Alphanumeric | 2 | 17 |
| Category Code | Numeric | 4 | 19 |
| Source | Alphanumeric | 10 | 23 |
| Description | Alphanumeric | 100 | 33 |
| Amount | Decimal | 11 | 133 |
| Merchant ID | Numeric | 9 | 144 |
| Merchant Name | Alphanumeric | 50 | 153 |
| Merchant City | Alphanumeric | 50 | 203 |
| Merchant Zip | Alphanumeric | 10 | 253 |
| Card Number | Alphanumeric | 16 | 263 |
| Orig Ts | Alphanumeric | 26 | 279 |
| Proc Ts | Alphanumeric | 26 | 305 |

---

## 22. Tcatbalf store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS` |
| Legacy Type | VSAM KSDS |
| Record Size | 50 bytes |
| Primary Access Key | Trancat Acct ID (11 bytes, position 1) |
| Access Pattern | Keyed lookup + update |
| Reader Programs | CBACT04C, CBTRN02C, IDCAMS |
| Writer Programs | CBTRN02C, IDCAMS |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, FinancialProcessingandInterestCalculation, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

**Fields** (from copybook CVTRA01Y):

| Field | Type | Length | Offset |
|-------|------|--------|--------|
| Trancat Acct ID | Numeric | 11 | 1 |
| Trancat Type Code | Alphanumeric | 2 | 12 |
| Trancat Code | Numeric | 4 | 14 |
| Category Balance | Decimal | 11 | 18 |

---

## 23. Tcatbalf store (BKUP)

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TCATBALF.BKUP` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | SORT |
| Writer Programs | IDCAMS |
| Reader Functions | ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, TransactionProcessingandAuthorization |
| Writer Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 24. Tcatbalf store (REPT)

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TCATBALF.REPT` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | None |
| Writer Programs | SORT |
| Reader Functions | None |
| Writer Functions | ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, TransactionProcessingandAuthorization |

*No field definitions available (no copybook found).*

---

## 25. Trancatg store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TRANCATG.VSAM.KSDS` |
| Legacy Type | VSAM KSDS |
| Record Size | 60 bytes |
| Primary Access Key | Type Code (2 bytes, position 1) |
| Access Pattern | Keyed lookup + update |
| Reader Programs | CBTRN03C |
| Writer Programs | IDCAMS |
| Reader Functions | ReportingandBusinessIntelligence |
| Writer Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

**Fields** (from copybook CVTRA04Y):

| Field | Type | Length | Offset |
|-------|------|--------|--------|
| Type Code | Alphanumeric | 2 | 1 |
| Category Code | Numeric | 4 | 3 |
| Category Type Description | Alphanumeric | 50 | 7 |

---

## 26. Trancatg store (BKUP)

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TRANCATG.PS.BKUP` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | None |
| Writer Programs | IEBGENER |
| Reader Functions | None |
| Writer Functions | ReferenceDataManagement, SystemInfrastructureandTechnicalServices, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 27. Tranrept store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TRANREPT` |
| Legacy Type | GDG Base |
| Record Size | 812 bytes |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | None |
| Writer Programs | CBTRN03C |
| Reader Functions | None |
| Writer Functions | ReportingandBusinessIntelligence |

**Fields** (from copybook CVTRA07Y):

| Field | Type | Length | Offset |
|-------|------|--------|--------|
| Short Name | Alphanumeric | 38 | 1 |
| Report Trans ID | Alphanumeric | 16 | 1 |
| Transaction Header 2 | Alphanumeric | 133 | 1 |
| Report Account ID | Alphanumeric | 11 | 18 |
| Report Type Code | Alphanumeric | 2 | 30 |
| Report Type Description | Alphanumeric | 15 | 33 |
| Long Name | Alphanumeric | 41 | 39 |
| Report Category Code | Numeric | 4 | 49 |
| Report Category Description | Alphanumeric | 29 | 54 |
| Date Header | Alphanumeric | 12 | 80 |
| Report Source | Alphanumeric | 10 | 84 |
| Start Date | Alphanumeric | 10 | 92 |
| Account Total | Decimal | 15 | 98 |
| Grand Total | Decimal | 15 | 98 |
| Page Total | Decimal | 15 | 98 |
| Report Amount | Decimal | 15 | 98 |
| End Date | Alphanumeric | 10 | 106 |

---

## 28. Transact store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS` |
| Legacy Type | VSAM KSDS |
| Record Size | 350 bytes |
| Primary Access Key | ID (16 bytes, position 1) |
| Access Pattern | Keyed lookup + update |
| Reader Programs | COBIL00C, COTRN00C, COTRN01C, COTRN02C, IDCAMS, SORT |
| Writer Programs | CBTRN02C, COBIL00C, COTRN02C, IDCAMS |
| Reader Functions | BillPaymentProcessing, CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | BillPaymentProcessing, CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

**Fields** (from copybook CVTRA05Y):

| Field | Type | Length | Offset |
|-------|------|--------|--------|
| ID | Alphanumeric | 16 | 1 |
| Type Code | Alphanumeric | 2 | 17 |
| Category Code | Numeric | 4 | 19 |
| Source | Alphanumeric | 10 | 23 |
| Description | Alphanumeric | 100 | 33 |
| Amount | Decimal | 11 | 133 |
| Merchant ID | Numeric | 9 | 144 |
| Merchant Name | Alphanumeric | 50 | 153 |
| Merchant City | Alphanumeric | 50 | 203 |
| Merchant Zip | Alphanumeric | 10 | 253 |
| Card Number | Alphanumeric | 16 | 263 |
| Orig Ts | Alphanumeric | 26 | 279 |
| Proc Ts | Alphanumeric | 26 | 305 |

---

## 29. Transact store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TRANSACT.VSAM.AIX` |
| Legacy Type | VSAM-AIX |
| Record Size | Unknown |
| Primary Access Key | [INFERRED: Alternate index — key field determined by AIX definition] |
| Access Pattern | Keyed lookup via alternate index |
| Reader Programs | IDCAMS |
| Writer Programs | IDCAMS |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 30. Transact store (BKUP)

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TRANSACT.BKUP` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | SORT |
| Writer Programs | IDCAMS |
| Reader Functions | ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, TransactionProcessingandAuthorization |
| Writer Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 31. Transact store (COMBINED)

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TRANSACT.COMBINED` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | IDCAMS |
| Writer Programs | SORT |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, TransactionProcessingandAuthorization |

*No field definitions available (no copybook found).*

---

## 32. Transact store (DALY)

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TRANSACT.DALY` |
| Legacy Type | GDG Base |
| Record Size | 350 bytes |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | CBTRN03C |
| Writer Programs | SORT |
| Reader Functions | ReportingandBusinessIntelligence |
| Writer Functions | ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, TransactionProcessingandAuthorization |

**Fields** (from copybook CVTRA05Y):

| Field | Type | Length | Offset |
|-------|------|--------|--------|
| ID | Alphanumeric | 16 | 1 |
| Type Code | Alphanumeric | 2 | 17 |
| Category Code | Numeric | 4 | 19 |
| Source | Alphanumeric | 10 | 23 |
| Description | Alphanumeric | 100 | 33 |
| Amount | Decimal | 11 | 133 |
| Merchant ID | Numeric | 9 | 144 |
| Merchant Name | Alphanumeric | 50 | 153 |
| Merchant City | Alphanumeric | 50 | 203 |
| Merchant Zip | Alphanumeric | 10 | 253 |
| Card Number | Alphanumeric | 16 | 263 |
| Orig Ts | Alphanumeric | 26 | 279 |
| Proc Ts | Alphanumeric | 26 | 305 |

---

## 33. Trantype store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TRANTYPE.VSAM.KSDS` |
| Legacy Type | VSAM KSDS |
| Record Size | 60 bytes |
| Primary Access Key | Type (2 bytes, position 1) |
| Access Pattern | Keyed lookup + update |
| Reader Programs | CBTRN03C |
| Writer Programs | IDCAMS |
| Reader Functions | ReportingandBusinessIntelligence |
| Writer Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

**Fields** (from copybook CVTRA03Y):

| Field | Type | Length | Offset |
|-------|------|--------|--------|
| Type | Alphanumeric | 2 | 1 |
| Type Description | Alphanumeric | 50 | 3 |

---

## 34. Trantype store (BKUP)

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TRANTYPE.BKUP` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | None |
| Writer Programs | IEBGENER |
| Reader Functions | None |
| Writer Functions | ReferenceDataManagement, SystemInfrastructureandTechnicalServices, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 35. Trxfl store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TRXFL.VSAM.KSDS` |
| Legacy Type | VSAM-KSDS |
| Record Size | Unknown |
| Primary Access Key | [UNCLEAR: No copybook fields available to identify key] |
| Access Pattern | Keyed lookup + update |
| Reader Programs | CBSTM03B |
| Writer Programs | IDCAMS |
| Reader Functions | StatementGenerationandCustomerCommunication |
| Writer Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 36. Trxfl store (SEQ)

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TRXFL.SEQ` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | IDCAMS |
| Writer Programs | SORT |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, TransactionProcessingandAuthorization |

*No field definitions available (no copybook found).*

---

## 37. Usrsec store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.USRSEC.VSAM.KSDS` |
| Legacy Type | VSAM KSDS |
| Record Size | 80 bytes |
| Primary Access Key | Usr ID (8 bytes, position 1) |
| Access Pattern | Keyed lookup + update |
| Reader Programs | COSGN00C, COUSR00C, COUSR02C, COUSR03C |
| Writer Programs | COUSR01C, COUSR02C, IDCAMS |
| Reader Functions | BillPaymentProcessing, CreditCardAccountManagement, CreditCardManagement, PaymentAuthorizationManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, SystemAdministrationandNavigation, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

**Fields** (from copybook CSUSR01Y):

| Field | Type | Length | Offset |
|-------|------|--------|--------|
| Usr ID | Alphanumeric | 8 | 1 |
| Usr Fname | Alphanumeric | 20 | 9 |
| Usr Lname | Alphanumeric | 20 | 29 |
| Usr Pwd | Alphanumeric | 8 | 49 |
| Usr Type | Alphanumeric | 1 | 57 |
| Usr Filler | Alphanumeric | 23 | 58 |

---

## 38. Usrsec store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.USRSEC.PS` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential write |
| Reader Programs | IDCAMS |
| Writer Programs | IEBGENER |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | ReferenceDataManagement, SystemInfrastructureandTechnicalServices, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 39. Usrsec store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.USRSEC.VSAM.ESDS` |
| Legacy Type | VSAM-ESDS |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential append |
| Reader Programs | None |
| Writer Programs | IDCAMS |
| Reader Functions | None |
| Writer Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 40. Usrsec store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.USRSEC.VSAM.RRDS` |
| Legacy Type | VSAM-RRDS |
| Record Size | Unknown |
| Primary Access Key | [UNCLEAR: No copybook fields available to identify key] |
| Access Pattern | Relative record + update |
| Reader Programs | None |
| Writer Programs | IDCAMS |
| Reader Functions | None |
| Writer Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |

*No field definitions available (no copybook found).*

---

## 41. Acctdata store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.ACCTDATA.PS` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential read |
| Reader Programs | IDCAMS |
| Writer Programs | None |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## 42. Bms(&mapname) store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.BMS(&MAPNAME)` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential read |
| Reader Programs | IEBGENER |
| Writer Programs | None |
| Reader Functions | ReferenceDataManagement, SystemInfrastructureandTechnicalServices, UserSecurityandAccessManagement |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## 43. Carddata store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.CARDDATA.PS` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential read |
| Reader Programs | IDCAMS |
| Writer Programs | None |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## 44. Cardxref store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.CARDXREF.PS` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential read |
| Reader Programs | IDCAMS |
| Writer Programs | None |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## 45. Custdata store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.CUSTDATA.PS` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential read |
| Reader Programs | IDCAMS |
| Writer Programs | None |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## 46. Dalytran store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.DALYTRAN.PS` |
| Legacy Type | Non VSAM |
| Record Size | 350 bytes |
| Primary Access Key | None (sequential) |
| Access Pattern | [UNCLEAR: Unknown store type 'Non VSAM' — add to DSType.PROPERTIES in contracts.py] |
| Reader Programs | CBTRN02C |
| Writer Programs | None |
| Reader Functions | TransactionProcessingandAuthorization |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## 47. Dalytran store (INIT)

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.DALYTRAN.PS.INIT` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential read |
| Reader Programs | IDCAMS |
| Writer Programs | None |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## 48. Dateparm store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.DATEPARM` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential read |
| Reader Programs | CBTRN03C |
| Writer Programs | None |
| Reader Functions | ReportingandBusinessIntelligence |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## 49. Discgrp store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.DISCGRP.PS` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential read |
| Reader Programs | IDCAMS, IEBGENER |
| Writer Programs | None |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## 50. Jcl(intrdrj2) store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.JCL(INTRDRJ2)` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential read |
| Reader Programs | IEBGENER |
| Writer Programs | None |
| Reader Functions | ReferenceDataManagement, SystemInfrastructureandTechnicalServices, UserSecurityandAccessManagement |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## 51. Listing(&mem) store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.LISTING(&MEM)` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential read |
| Reader Programs | IEBGENER |
| Writer Programs | None |
| Reader Functions | ReferenceDataManagement, SystemInfrastructureandTechnicalServices, UserSecurityandAccessManagement |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## 52. Lst(&mapname) store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.LST(&MAPNAME)` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential read |
| Reader Programs | IEBGENER |
| Writer Programs | None |
| Reader Functions | ReferenceDataManagement, SystemInfrastructureandTechnicalServices, UserSecurityandAccessManagement |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## 53. Lst(&mem) store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.LST(&MEM)` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential read |
| Reader Programs | IEBGENER |
| Writer Programs | None |
| Reader Functions | ReferenceDataManagement, SystemInfrastructureandTechnicalServices, UserSecurityandAccessManagement |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## 54. Oem store (SDFHSAMP(DFHEILID))

| Property | Value |
|---|---|
| Legacy Identifier | `OEM.CICSTS.V05R06M0.CICS.SDFHSAMP(DFHEILID)` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential read |
| Reader Programs | IEBGENER |
| Writer Programs | None |
| Reader Functions | ReferenceDataManagement, SystemInfrastructureandTechnicalServices, UserSecurityandAccessManagement |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## 55. Tcatbalf store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TCATBALF.PS` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential read |
| Reader Programs | IDCAMS |
| Writer Programs | None |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## 56. Trancatg store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TRANCATG.PS` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential read |
| Reader Programs | IDCAMS, IEBGENER |
| Writer Programs | None |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## 57. Trantype store

| Property | Value |
|---|---|
| Legacy Identifier | `AWS.M2.CARDDEMO.TRANTYPE.PS` |
| Legacy Type | QSAM |
| Record Size | Unknown |
| Primary Access Key | None (sequential) |
| Access Pattern | Sequential read |
| Reader Programs | IDCAMS, IEBGENER |
| Writer Programs | None |
| Reader Functions | CreditCardAccountManagement, CreditCardManagement, CustomerDataManagement, ReferenceDataManagement, ReportingandBusinessIntelligence, StatementGenerationandCustomerCommunication, SystemInfrastructureandTechnicalServices, TransactionProcessingandAuthorization, UserSecurityandAccessManagement |
| Writer Functions | None |

*No field definitions available (no copybook found).*

---

## Entity Relationships

The following relationships are inferred from shared key fields across entities:

- **Acctdata store** ↔ **Custdata store** (shared: ID)
- **Acctdata store** ↔ **Systran store** (shared: ID)
- **Acctdata store** ↔ **Transact store** (shared: ID)
- **Acctdata store** ↔ **Transact store (DALY)** (shared: ID)
- **Cardxref store** ↔ **Systran store** (shared: Card Number)
- **Cardxref store** ↔ **Transact store** (shared: Card Number)
- **Cardxref store** ↔ **Transact store (DALY)** (shared: Card Number)
- **Custdata store** ↔ **Systran store** (shared: ID)
- **Custdata store** ↔ **Transact store** (shared: ID)
- **Custdata store** ↔ **Transact store (DALY)** (shared: ID)
- **Systran store** ↔ **Trancatg store** (shared: Category Code, Type Code)
- **Systran store** ↔ **Transact store** (shared: Card Number, Category Code, ID, Merchant City, Merchant ID, Merchant Name, Merchant Zip, Orig Ts, Proc Ts, Source, Type Code)
- **Systran store** ↔ **Transact store (DALY)** (shared: Card Number, Category Code, ID, Merchant City, Merchant ID, Merchant Name, Merchant Zip, Orig Ts, Proc Ts, Source, Type Code)
- **Trancatg store** ↔ **Transact store** (shared: Category Code, Type Code)
- **Trancatg store** ↔ **Transact store (DALY)** (shared: Category Code, Type Code)
- **Transact store** ↔ **Transact store (DALY)** (shared: Card Number, Category Code, ID, Merchant City, Merchant ID, Merchant Name, Merchant Zip, Orig Ts, Proc Ts, Source, Type Code)

*These relationships describe the logical connections between entities. The designer should determine how to implement these (foreign keys, joins, denormalization, etc.) based on the chosen storage technology.*

