package com.carddemo.integration;

import com.carddemo.entity.*;
import com.carddemo.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private UserSecurityRepository userSecurityRepository;

    private String adminSessionCookie;
    private String adminCsrfToken;

    private static final String ACCOUNT_ID = "00000000001";
    private static final String CUSTOMER_ID = "000000001";
    private static final String CARD_NUMBER = "4111111111111111";

    @BeforeEach
    void setUp() throws Exception {
        userSecurityRepository.deleteAll();
        cardXrefRepository.deleteAll();
        customerRepository.deleteAll();
        accountRepository.deleteAll();

        UserSecurity admin = new UserSecurity();
        admin.setUserId("ADMIN001");
        admin.setPassword("PASSWORDA");
        admin.setFirstName("Admin");
        admin.setLastName("One");
        admin.setUserType("A");
        userSecurityRepository.save(admin);

        Account account = new Account();
        account.setAccountId(ACCOUNT_ID);
        account.setActiveStatus("Y");
        account.setCurrentBalance(new BigDecimal("5000.00"));
        account.setCreditLimit(new BigDecimal("10000.00"));
        account.setCashCreditLimit(new BigDecimal("2000.00"));
        account.setOpenDate("2020-01-15");
        account.setExpirationDate("2027-12-31");
        account.setReissueDate("2025-01-15");
        account.setCurrentCycleCreditAmount(new BigDecimal("200.00"));
        account.setCurrentCycleDebitAmount(new BigDecimal("300.00"));
        account.setGroupId("DEFAULT");
        account.setAddressZip("90210");
        account.setVersion(1);
        accountRepository.save(account);

        Customer customer = new Customer();
        customer.setCustomerId(CUSTOMER_ID);
        customer.setFirstName("John");
        customer.setMiddleName("M");
        customer.setLastName("Doe");
        customer.setAddressLine1("123 Main St");
        customer.setStateCode("CA");
        customer.setCountryCode("US");
        customer.setZipCode("90210");
        customer.setPhoneNumber1("(310)555-1234");
        customer.setFicoScore(750);
        customer.setDateOfBirth("1985-06-15");
        customer.setVersion(1);
        customerRepository.save(customer);

        CardXref xref = new CardXref();
        xref.setCardNumber(CARD_NUMBER);
        xref.setAccountId(ACCOUNT_ID);
        xref.setCustomerId(CUSTOMER_ID);
        cardXrefRepository.save(xref);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"ADMIN001\",\"password\":\"PASSWORDA\"}"))
            .andExpect(status().isOk())
            .andReturn();

        adminSessionCookie = loginResult.getResponse().getCookie("SESSION") != null ?
            loginResult.getResponse().getCookie("SESSION").getValue() : "";
        adminCsrfToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
            .get("csrfToken").asText();
    }

    @Nested
    class AccountLookup {

        @Test
        void shouldRetrieveAccountAndCustomerData() throws Exception {
            mockMvc.perform(get("/api/account/" + ACCOUNT_ID)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID))
                .andExpect(jsonPath("$.activeStatus").value("Y"))
                .andExpect(jsonPath("$.currentBalance").value(5000.00))
                .andExpect(jsonPath("$.creditLimit").value(10000.00))
                .andExpect(jsonPath("$.customer.firstName").value("John"))
                .andExpect(jsonPath("$.customer.lastName").value("Doe"));
        }

        @Test
        void shouldReturnNotFoundForNonExistentAccount() throws Exception {
            mockMvc.perform(get("/api/account/99999999999")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found"));
        }

        @Test
        void shouldRejectNonNumericAccountId() throws Exception {
            mockMvc.perform(get("/api/account/ABCDEFGHIJK")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectZeroAccountId() throws Exception {
            mockMvc.perform(get("/api/account/00000000000")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldMaskSsnInResponse() throws Exception {
            mockMvc.perform(get("/api/account/" + ACCOUNT_ID)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.ssn").value(org.hamcrest.Matchers.matchesPattern("\\*\\*\\*-\\*\\*-\\d{4}")));
        }
    }

    @Nested
    class AccountUpdateValidation {

        @Test
        void shouldRejectInvalidActiveStatus() throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID)
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"activeStatus\":\"X\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid account status"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Y", "N"})
        void shouldAcceptValidActiveStatus(String status) throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID)
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"activeStatus\":\"" + status + "\"}"))
                .andExpect(status().isOk());
        }

        @Test
        void shouldRejectInvalidDateFormat() throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID)
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"openDate\":\"05/11/2026\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid date format"));
        }

        @Test
        void shouldRejectNonNumericBalance() throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID)
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"currentBalance\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid numeric value"));
        }

        @Test
        void shouldRejectFicoScoreBelow300() throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID)
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"customer\":{\"ficoScore\":250}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("FICO score must be between 300 and 850"));
        }

        @Test
        void shouldRejectFicoScoreAbove850() throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID)
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"customer\":{\"ficoScore\":900}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("FICO score must be between 300 and 850"));
        }

        @Test
        void shouldRejectInvalidSsnFormat() throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID)
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"customer\":{\"ssn\":\"123456789\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid SSN format"));
        }

        @Test
        void shouldAcceptValidSsnFormat() throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID)
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"customer\":{\"ssn\":\"123-45-6789\"}}"))
                .andExpect(status().isOk());
        }

        @Test
        void shouldRejectInvalidStateCode() throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID)
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"customer\":{\"stateCode\":\"XX\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid state code"));
        }

        @Test
        void shouldRejectInvalidPhoneNumber() throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID)
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"customer\":{\"phoneNumber1\":\"12345\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid phone number"));
        }

        @Test
        void shouldRejectNonAlphabeticName() throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID)
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"customer\":{\"firstName\":\"John123\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid characters"));
        }
    }

    @Nested
    class FieldClearing {

        @Test
        void shouldClearFieldWhenAsteriskEntered() throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID)
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"customer\":{\"middleName\":\"*\"}}"))
                .andExpect(status().isOk());

            Customer updated = customerRepository.findByCustomerId(CUSTOMER_ID).orElseThrow();
            assertThat(updated.getMiddleName()).isNullOrEmpty();
        }
    }

    @Nested
    class ChangeDetection {

        @Test
        void shouldDetectNoChangesWhenAllFieldsSame() throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID)
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"activeStatus\":\"Y\",\"customer\":{\"firstName\":\"John\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No changes detected"));
        }

        @Test
        void shouldDetectNoChangesWithCaseDifference() throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID)
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"customer\":{\"firstName\":\"john\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No changes detected"));
        }

        @Test
        void shouldDetectChangesWhenFieldDiffers() throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID)
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"customer\":{\"firstName\":\"Jane\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmationRequired").value(true));
        }
    }

    @Nested
    class ConfirmAndPersist {

        @Test
        void shouldPersistChangesOnConfirmation() throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID + "/confirm")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"creditLimit\":15000.00,\"customer\":{\"stateCode\":\"TX\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

            Account updated = accountRepository.findByAccountId(ACCOUNT_ID).orElseThrow();
            assertThat(updated.getCreditLimit()).isEqualByComparingTo(new BigDecimal("15000.00"));

            Customer updatedCustomer = customerRepository.findByCustomerId(CUSTOMER_ID).orElseThrow();
            assertThat(updatedCustomer.getStateCode()).isEqualTo("TX");
        }

        @Test
        void shouldRejectOnConcurrentModification() throws Exception {
            Account account = accountRepository.findByAccountId(ACCOUNT_ID).orElseThrow();
            account.setVersion(account.getVersion() + 1);
            accountRepository.save(account);

            mockMvc.perform(put("/api/account/" + ACCOUNT_ID + "/confirm")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"creditLimit\":15000.00,\"version\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Lock error. Record is being modified by another user"));
        }

        @Test
        void shouldDiscardChangesOnCancel() throws Exception {
            mockMvc.perform(put("/api/account/" + ACCOUNT_ID + "/cancel")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            Account unchanged = accountRepository.findByAccountId(ACCOUNT_ID).orElseThrow();
            assertThat(unchanged.getCreditLimit()).isEqualByComparingTo(new BigDecimal("10000.00"));
        }
    }

    @Nested
    class RateLimiting {

        @Test
        void shouldEnforceRateLimitOf20UpdatesPerHour() throws Exception {
            for (int i = 0; i < 20; i++) {
                mockMvc.perform(put("/api/account/" + ACCOUNT_ID + "/confirm")
                        .header("X-CSRF-TOKEN", adminCsrfToken)
                        .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\":{\"firstName\":\"Name" + i + "\"}}"))
                    .andExpect(status().isOk());
            }

            mockMvc.perform(put("/api/account/" + ACCOUNT_ID + "/confirm")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"customer\":{\"firstName\":\"Overflow\"}}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many requests"));
        }
    }

    @Nested
    class AccountInquiryService {

        @Test
        void shouldReturnAccountDetailsForValidInquiry() throws Exception {
            mockMvc.perform(post("/api/account/inquiry")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"function\":\"INQA\",\"accountKey\":\"" + ACCOUNT_ID + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID))
                .andExpect(jsonPath("$.activeStatus").value("Y"))
                .andExpect(jsonPath("$.currentBalance").value(5000.00))
                .andExpect(jsonPath("$.creditLimit").value(10000.00))
                .andExpect(jsonPath("$.cashCreditLimit").value(2000.00))
                .andExpect(jsonPath("$.openDate").value("2020-01-15"))
                .andExpect(jsonPath("$.expirationDate").value("2027-12-31"))
                .andExpect(jsonPath("$.groupId").value("DEFAULT"));
        }

        @Test
        void shouldReturnErrorForInvalidAccountInInquiry() throws Exception {
            mockMvc.perform(post("/api/account/inquiry")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"function\":\"INQA\",\"accountKey\":\"99999999999\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Invalid account ID"));
        }

        @Test
        void shouldRejectInvalidFunctionCode() throws Exception {
            mockMvc.perform(post("/api/account/inquiry")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"function\":\"XXXX\",\"accountKey\":\"" + ACCOUNT_ID + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
        }

        @Test
        void shouldRejectZeroAccountKey() throws Exception {
            mockMvc.perform(post("/api/account/inquiry")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"function\":\"INQA\",\"accountKey\":\"00000000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
        }
    }
}
