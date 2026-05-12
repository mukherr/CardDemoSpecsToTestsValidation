package com.carddemo.integration;

import com.carddemo.entity.*;
import com.carddemo.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BillPaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserSecurityRepository userSecurityRepository;

    private String sessionCookie;
    private String csrfToken;

    private static final String ACCOUNT_ID = "00000000001";
    private static final String CARD_NUMBER = "4111111111111111";
    private static final String CUSTOMER_ID = "000000001";

    @BeforeEach
    void setUp() throws Exception {
        transactionRepository.deleteAll();
        cardXrefRepository.deleteAll();
        accountRepository.deleteAll();
        userSecurityRepository.deleteAll();

        UserSecurity user = new UserSecurity();
        user.setUserId("USER0001");
        user.setPassword("PASSWORDU");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setUserType("U");
        userSecurityRepository.save(user);

        Account account = new Account();
        account.setAccountId(ACCOUNT_ID);
        account.setActiveStatus("Y");
        account.setCurrentBalance(new BigDecimal("5000.00"));
        account.setCreditLimit(new BigDecimal("10000.00"));
        account.setCashCreditLimit(new BigDecimal("2000.00"));
        account.setCurrentCycleCreditAmount(BigDecimal.ZERO);
        account.setCurrentCycleDebitAmount(BigDecimal.ZERO);
        account.setOpenDate("2020-01-01");
        account.setExpirationDate("2027-12-31");
        account.setGroupId("DEFAULT");
        accountRepository.save(account);

        CardXref xref = new CardXref();
        xref.setCardNumber(CARD_NUMBER);
        xref.setAccountId(ACCOUNT_ID);
        xref.setCustomerId(CUSTOMER_ID);
        cardXrefRepository.save(xref);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"USER0001\",\"password\":\"PASSWORDU\"}"))
            .andExpect(status().isOk())
            .andReturn();

        sessionCookie = loginResult.getResponse().getCookie("SESSION") != null ?
            loginResult.getResponse().getCookie("SESSION").getValue() : "";
        String response = loginResult.getResponse().getContentAsString();
        csrfToken = objectMapper.readTree(response).get("csrfToken").asText();
    }

    @Nested
    class PaymentValidation {

        @Test
        void shouldRejectEmptyAccountId() throws Exception {
            mockMvc.perform(post("/api/billing/payment")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":500.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please enter account ID"));
        }

        @Test
        void shouldRejectNonExistentAccount() throws Exception {
            mockMvc.perform(post("/api/billing/payment")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"99999999999\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":500.00}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found"));
        }

        @Test
        void shouldRejectNegativeAmount() throws Exception {
            mockMvc.perform(post("/api/billing/payment")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":-100.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid payment amount"));
        }

        @Test
        void shouldRejectZeroAmount() throws Exception {
            mockMvc.perform(post("/api/billing/payment")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":0.00}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectAmountExceedingBalance() throws Exception {
            mockMvc.perform(post("/api/billing/payment")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":6000.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient credit"));
        }

        @Test
        void shouldRejectAmountExceeding50000Limit() throws Exception {
            Account bigAccount = accountRepository.findByAccountId(ACCOUNT_ID).orElseThrow();
            bigAccount.setCurrentBalance(new BigDecimal("60000.00"));
            bigAccount.setCreditLimit(new BigDecimal("100000.00"));
            accountRepository.save(bigAccount);

            mockMvc.perform(post("/api/billing/payment")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":50001.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount exceeds limit"));
        }

        @Test
        void shouldRejectZeroBalance() throws Exception {
            Account zeroAccount = accountRepository.findByAccountId(ACCOUNT_ID).orElseThrow();
            zeroAccount.setCurrentBalance(BigDecimal.ZERO);
            accountRepository.save(zeroAccount);

            mockMvc.perform(post("/api/billing/payment")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":100.00}"))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class PaymentConfirmation {

        @Test
        void shouldShowConfirmationOnValidPayment() throws Exception {
            mockMvc.perform(post("/api/billing/payment")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":500.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmationRequired").value(true))
                .andExpect(jsonPath("$.accountBalance").value(5000.00));

            assertThat(transactionRepository.count()).isZero();
        }

        @Test
        void shouldCreateTransactionOnConfirmation() throws Exception {
            mockMvc.perform(post("/api/billing/payment/confirm")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":500.00,\"confirm\":\"Y\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.transactionId").isNotEmpty());

            assertThat(transactionRepository.count()).isEqualTo(1);
            Transaction txn = transactionRepository.findAll().get(0);
            assertThat(txn.getTypeCode()).isEqualTo("02");
            assertThat(txn.getCategoryCode()).isEqualTo("2");
            assertThat(txn.getSource()).isEqualTo("POS TERM");
            assertThat(txn.getMerchant()).isEqualTo("BILL PAYMENT");
            assertThat(txn.getAmount()).isEqualByComparingTo(new BigDecimal("-500.00"));
        }

        @Test
        void shouldUpdateAccountBalanceOnConfirmation() throws Exception {
            mockMvc.perform(post("/api/billing/payment/confirm")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":500.00,\"confirm\":\"Y\"}"))
                .andExpect(status().isOk());

            Account updated = accountRepository.findByAccountId(ACCOUNT_ID).orElseThrow();
            assertThat(updated.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("4500.00"));
            assertThat(updated.getCurrentCycleDebitAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        }

        @Test
        void shouldNotCreateTransactionOnCancellation() throws Exception {
            mockMvc.perform(post("/api/billing/payment/confirm")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":500.00,\"confirm\":\"N\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelled").value(true));

            assertThat(transactionRepository.count()).isZero();
            Account unchanged = accountRepository.findByAccountId(ACCOUNT_ID).orElseThrow();
            assertThat(unchanged.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("5000.00"));
        }

        @Test
        void shouldRejectInvalidConfirmationValue() throws Exception {
            mockMvc.perform(post("/api/billing/payment/confirm")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":500.00,\"confirm\":\"X\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valid values are Y or N"));
        }
    }

    @Nested
    class TransactionRecordFields {

        @Test
        void shouldPopulateTransactionFieldsCorrectly() throws Exception {
            mockMvc.perform(post("/api/billing/payment/confirm")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":1000.00,\"confirm\":\"Y\"}"))
                .andExpect(status().isOk());

            Transaction txn = transactionRepository.findAll().get(0);
            assertThat(txn.getTransactionId()).isNotNull();
            assertThat(txn.getTransactionId()).hasSize(16);
            assertThat(txn.getCardNumber()).isEqualTo(CARD_NUMBER);
            assertThat(txn.getAccountId()).isEqualTo(ACCOUNT_ID);
            assertThat(txn.getTypeCode()).isEqualTo("02");
            assertThat(txn.getCategoryCode()).isEqualTo("2");
            assertThat(txn.getSource()).isEqualTo("POS TERM");
            assertThat(txn.getAmount()).isEqualByComparingTo(new BigDecimal("-1000.00"));
            assertThat(txn.getMerchantId()).isEqualTo("999999999");
            assertThat(txn.getMerchant()).isEqualTo("BILL PAYMENT");
            assertThat(txn.getMerchantCity()).isEqualTo("N/A");
            assertThat(txn.getMerchantZip()).isEqualTo("N/A");
            assertThat(txn.getTimestamp()).isNotNull();
        }
    }

    @Nested
    class SecurityControls {

        @Test
        void shouldRejectRequestWithoutCsrfToken() throws Exception {
            mockMvc.perform(post("/api/billing/payment")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":500.00}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Invalid request"));
        }

        @Test
        void shouldRejectRequestWithInvalidCsrfToken() throws Exception {
            mockMvc.perform(post("/api/billing/payment")
                    .header("X-CSRF-TOKEN", "invalid-token-12345")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":500.00}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Invalid request"));
        }

        @Test
        void shouldRejectUnauthenticatedRequest() throws Exception {
            mockMvc.perform(post("/api/billing/payment")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":500.00}"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldEnforceRateLimitOf10PaymentsPerHour() throws Exception {
            for (int i = 0; i < 10; i++) {
                mockMvc.perform(post("/api/billing/payment/confirm")
                        .header("X-CSRF-TOKEN", csrfToken)
                        .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":10.00,\"confirm\":\"Y\"}"))
                    .andExpect(status().isOk());
            }

            mockMvc.perform(post("/api/billing/payment/confirm")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":10.00,\"confirm\":\"Y\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many requests"));
        }

        @Test
        void shouldRejectUnauthorizedAccountAccess() throws Exception {
            Account otherAccount = new Account();
            otherAccount.setAccountId("99999999999");
            otherAccount.setActiveStatus("Y");
            otherAccount.setCurrentBalance(new BigDecimal("1000.00"));
            otherAccount.setCreditLimit(new BigDecimal("5000.00"));
            accountRepository.save(otherAccount);

            mockMvc.perform(post("/api/billing/payment")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"99999999999\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":100.00}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found"));
        }
    }

    @Nested
    class Atomicity {

        @Test
        void shouldRollbackOnAccountUpdateFailure() throws Exception {
            long initialTxnCount = transactionRepository.count();
            BigDecimal initialBalance = accountRepository.findByAccountId(ACCOUNT_ID).orElseThrow().getCurrentBalance();

            accountRepository.delete(accountRepository.findByAccountId(ACCOUNT_ID).orElseThrow());

            mockMvc.perform(post("/api/billing/payment/confirm")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":500.00,\"confirm\":\"Y\"}"))
                .andExpect(status().isInternalServerError());

            assertThat(transactionRepository.count()).isEqualTo(initialTxnCount);
        }
    }

    @Nested
    class TransactionIdGeneration {

        @Test
        void shouldGenerateIncrementingTransactionIds() throws Exception {
            mockMvc.perform(post("/api/billing/payment/confirm")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":100.00,\"confirm\":\"Y\"}"))
                .andExpect(status().isOk());

            mockMvc.perform(post("/api/billing/payment/confirm")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":100.00,\"confirm\":\"Y\"}"))
                .andExpect(status().isOk());

            var transactions = transactionRepository.findAll();
            assertThat(transactions).hasSize(2);
            long id1 = Long.parseLong(transactions.get(0).getTransactionId());
            long id2 = Long.parseLong(transactions.get(1).getTransactionId());
            assertThat(id2).isGreaterThan(id1);
        }

        @Test
        void shouldStartFromZeroWhenNoTransactionsExist() throws Exception {
            transactionRepository.deleteAll();

            MvcResult result = mockMvc.perform(post("/api/billing/payment/confirm")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":100.00,\"confirm\":\"Y\"}"))
                .andExpect(status().isOk())
                .andReturn();

            assertThat(transactionRepository.count()).isEqualTo(1);
            Transaction txn = transactionRepository.findAll().get(0);
            assertThat(txn.getTransactionId()).isNotNull();
        }
    }
}
