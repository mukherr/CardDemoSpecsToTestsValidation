package com.carddemo.integration;

import com.carddemo.entity.*;
import com.carddemo.repository.*;
import com.carddemo.service.AuthorizationService;
import com.carddemo.dto.AuthorizationRequest;
import com.carddemo.dto.AuthorizationResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransactionProcessingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionCategoryBalanceRepository tcatBalRepository;

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
        tcatBalRepository.deleteAll();
        cardXrefRepository.deleteAll();
        cardRepository.deleteAll();
        customerRepository.deleteAll();
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
        account.setCurrentBalance(new BigDecimal("2000.00"));
        account.setCreditLimit(new BigDecimal("10000.00"));
        account.setCashCreditLimit(new BigDecimal("2000.00"));
        account.setCurrentCycleCreditAmount(BigDecimal.ZERO);
        account.setCurrentCycleDebitAmount(BigDecimal.ZERO);
        account.setOpenDate("2020-01-01");
        account.setExpirationDate("2027-12-31");
        account.setGroupId("DEFAULT");
        accountRepository.save(account);

        Card card = new Card();
        card.setCardNumber(CARD_NUMBER);
        card.setActiveStatus("Y");
        card.setEmbossedName("TEST USER");
        card.setExpiryDate("2027-12-31");
        cardRepository.save(card);

        Customer customer = new Customer();
        customer.setCustomerId(CUSTOMER_ID);
        customer.setFirstName("Test");
        customer.setLastName("User");
        customerRepository.save(customer);

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
        csrfToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
            .get("csrfToken").asText();
    }

    @Nested
    class AuthorizationDecision {

        @Test
        void shouldApproveWhenAmountWithinCredit() {
            AuthorizationRequest request = new AuthorizationRequest();
            request.setCardNumber(CARD_NUMBER);
            request.setTransactionAmount(new BigDecimal("500.00"));

            AuthorizationResponse response = authorizationService.processAuthorization(request);

            assertThat(response.getResponseCode()).isEqualTo("00");
            assertThat(response.getApprovedAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        }

        @Test
        void shouldDeclineWhenAmountExceedsAvailableCredit() {
            AuthorizationRequest request = new AuthorizationRequest();
            request.setCardNumber(CARD_NUMBER);
            request.setTransactionAmount(new BigDecimal("9000.00"));

            AuthorizationResponse response = authorizationService.processAuthorization(request);

            assertThat(response.getResponseCode()).isEqualTo("05");
            assertThat(response.getReasonCode()).isEqualTo("4100");
            assertThat(response.getApprovedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldDeclineWhenCardNotFound() {
            AuthorizationRequest request = new AuthorizationRequest();
            request.setCardNumber("9999999999999999");
            request.setTransactionAmount(new BigDecimal("100.00"));

            AuthorizationResponse response = authorizationService.processAuthorization(request);

            assertThat(response.getResponseCode()).isEqualTo("05");
            assertThat(response.getReasonCode()).isEqualTo("3100");
            assertThat(response.getApprovedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldDeclineWhenCardInactive() {
            Card inactiveCard = cardRepository.findByCardNumber(CARD_NUMBER).orElseThrow();
            inactiveCard.setActiveStatus("N");
            cardRepository.save(inactiveCard);

            AuthorizationRequest request = new AuthorizationRequest();
            request.setCardNumber(CARD_NUMBER);
            request.setTransactionAmount(new BigDecimal("100.00"));

            AuthorizationResponse response = authorizationService.processAuthorization(request);

            assertThat(response.getResponseCode()).isEqualTo("05");
            assertThat(response.getReasonCode()).isEqualTo("4200");
        }

        @Test
        void shouldDeclineWhenAccountClosed() {
            Account closedAccount = accountRepository.findByAccountId(ACCOUNT_ID).orElseThrow();
            closedAccount.setActiveStatus("N");
            accountRepository.save(closedAccount);

            AuthorizationRequest request = new AuthorizationRequest();
            request.setCardNumber(CARD_NUMBER);
            request.setTransactionAmount(new BigDecimal("100.00"));

            AuthorizationResponse response = authorizationService.processAuthorization(request);

            assertThat(response.getResponseCode()).isEqualTo("05");
            assertThat(response.getReasonCode()).isEqualTo("4300");
        }
    }

    @Nested
    class TransactionList {

        @BeforeEach
        void setUpTransactions() {
            for (int i = 1; i <= 25; i++) {
                Transaction txn = new Transaction();
                txn.setTransactionId(String.format("%016d", i));
                txn.setCardNumber(CARD_NUMBER);
                txn.setAccountId(ACCOUNT_ID);
                txn.setTypeCode("01");
                txn.setCategoryCode("001");
                txn.setSource("TEST");
                txn.setAmount(new BigDecimal(i * 10));
                txn.setTimestamp("2026-05-01 10:00:00");
                transactionRepository.save(txn);
            }
        }

        @Test
        void shouldDisplayFirstPageOfTransactions() throws Exception {
            mockMvc.perform(get("/api/transactions")
                    .param("page", "0")
                    .param("size", "10")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(10));
        }

        @Test
        void shouldPaginateTransactions() throws Exception {
            mockMvc.perform(get("/api/transactions")
                    .param("page", "1")
                    .param("size", "10")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10));

            mockMvc.perform(get("/api/transactions")
                    .param("page", "2")
                    .param("size", "10")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5));
        }

        @Test
        void shouldRejectNonNumericTransactionIdSearch() throws Exception {
            mockMvc.perform(get("/api/transactions")
                    .param("searchId", "ABCDEF")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tran ID must be Numeric"));
        }

        @Test
        void shouldRedirectToSignOnWithoutSession() throws Exception {
            mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class TransactionDetail {

        @Test
        void shouldDisplayTransactionDetails() throws Exception {
            Transaction txn = new Transaction();
            txn.setTransactionId("0000000000000001");
            txn.setCardNumber(CARD_NUMBER);
            txn.setAccountId(ACCOUNT_ID);
            txn.setTypeCode("01");
            txn.setCategoryCode("001");
            txn.setSource("POS");
            txn.setAmount(new BigDecimal("100.00"));
            txn.setMerchant("TEST STORE");
            txn.setMerchantCity("LOS ANGELES");
            txn.setMerchantZip("90001");
            txn.setTimestamp("2026-05-01 10:00:00");
            transactionRepository.save(txn);

            mockMvc.perform(get("/api/transactions/0000000000000001")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("0000000000000001"))
                .andExpect(jsonPath("$.cardNumber").value(CARD_NUMBER))
                .andExpect(jsonPath("$.typeCode").value("01"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.merchant").value("TEST STORE"));
        }

        @Test
        void shouldReturnNotFoundForNonExistentTransaction() throws Exception {
            mockMvc.perform(get("/api/transactions/9999999999999999")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction ID NOT found"));
        }

        @Test
        void shouldRejectEmptyTransactionId() throws Exception {
            mockMvc.perform(get("/api/transactions/")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class AddTransaction {

        @Test
        void shouldCreateTransactionWithValidData() throws Exception {
            String txnJson = "{" +
                "\"cardNumber\":\"" + CARD_NUMBER + "\"," +
                "\"typeCode\":\"01\"," +
                "\"categoryCode\":\"001\"," +
                "\"source\":\"POS\"," +
                "\"description\":\"Test purchase\"," +
                "\"amount\":\"100.00\"," +
                "\"merchantId\":\"123456789\"," +
                "\"merchantName\":\"Test Store\"," +
                "\"merchantCity\":\"Los Angeles\"," +
                "\"merchantZip\":\"90001\"," +
                "\"originalDate\":\"2026-05-11\"," +
                "\"processingDate\":\"2026-05-11\"," +
                "\"confirm\":\"Y\"}";

            mockMvc.perform(post("/api/transactions")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(txnJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void shouldRejectMissingCardAndAccount() throws Exception {
            String txnJson = "{" +
                "\"typeCode\":\"01\"," +
                "\"categoryCode\":\"001\"," +
                "\"amount\":\"100.00\"," +
                "\"confirm\":\"Y\"}";

            mockMvc.perform(post("/api/transactions")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(txnJson))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectNonNumericTypeCode() throws Exception {
            String txnJson = "{" +
                "\"cardNumber\":\"" + CARD_NUMBER + "\"," +
                "\"typeCode\":\"AB\"," +
                "\"categoryCode\":\"001\"," +
                "\"amount\":\"100.00\"," +
                "\"confirm\":\"Y\"}";

            mockMvc.perform(post("/api/transactions")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(txnJson))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectInvalidDateInTransaction() throws Exception {
            String txnJson = "{" +
                "\"cardNumber\":\"" + CARD_NUMBER + "\"," +
                "\"typeCode\":\"01\"," +
                "\"categoryCode\":\"001\"," +
                "\"source\":\"POS\"," +
                "\"description\":\"Test\"," +
                "\"amount\":\"100.00\"," +
                "\"merchantId\":\"123456789\"," +
                "\"merchantName\":\"Store\"," +
                "\"merchantCity\":\"City\"," +
                "\"merchantZip\":\"90001\"," +
                "\"originalDate\":\"2026-13-45\"," +
                "\"processingDate\":\"2026-05-11\"," +
                "\"confirm\":\"Y\"}";

            mockMvc.perform(post("/api/transactions")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(txnJson))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRequireCsrfTokenForCreation() throws Exception {
            String txnJson = "{\"cardNumber\":\"" + CARD_NUMBER + "\",\"typeCode\":\"01\",\"amount\":\"100.00\",\"confirm\":\"Y\"}";

            mockMvc.perform(post("/api/transactions")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(txnJson))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    class DailyTransactionPosting {

        @Test
        void shouldRejectTransactionWithInvalidCard() {
            Transaction dailyTxn = new Transaction();
            dailyTxn.setTransactionId("0000000000000099");
            dailyTxn.setCardNumber("9999999999999999");
            dailyTxn.setAmount(new BigDecimal("100.00"));
            dailyTxn.setTimestamp("2026-05-01 10:00:00");

            var result = authorizationService.validateDailyTransaction(dailyTxn);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getReasonCode()).isEqualTo(100);
            assertThat(result.getDescription()).isEqualTo("INVALID CARD NUMBER FOUND");
        }

        @Test
        void shouldRejectTransactionWhenAccountNotFound() {
            cardXrefRepository.deleteAll();
            CardXref orphanXref = new CardXref();
            orphanXref.setCardNumber("5555555555555555");
            orphanXref.setAccountId("99999999999");
            orphanXref.setCustomerId(CUSTOMER_ID);
            cardXrefRepository.save(orphanXref);

            Transaction dailyTxn = new Transaction();
            dailyTxn.setTransactionId("0000000000000099");
            dailyTxn.setCardNumber("5555555555555555");
            dailyTxn.setAmount(new BigDecimal("100.00"));
            dailyTxn.setTimestamp("2026-05-01 10:00:00");

            var result = authorizationService.validateDailyTransaction(dailyTxn);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getReasonCode()).isEqualTo(101);
            assertThat(result.getDescription()).isEqualTo("ACCOUNT RECORD NOT FOUND");
        }

        @Test
        void shouldRejectOverlimitTransaction() {
            Transaction dailyTxn = new Transaction();
            dailyTxn.setTransactionId("0000000000000099");
            dailyTxn.setCardNumber(CARD_NUMBER);
            dailyTxn.setAmount(new BigDecimal("9500.00"));
            dailyTxn.setTimestamp("2026-05-01 10:00:00");

            var result = authorizationService.validateDailyTransaction(dailyTxn);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getReasonCode()).isEqualTo(102);
            assertThat(result.getDescription()).isEqualTo("OVERLIMIT TRANSACTION");
        }

        @Test
        void shouldRejectTransactionAfterAccountExpiration() {
            Account expiredAccount = accountRepository.findByAccountId(ACCOUNT_ID).orElseThrow();
            expiredAccount.setExpirationDate("2020-01-01");
            accountRepository.save(expiredAccount);

            Transaction dailyTxn = new Transaction();
            dailyTxn.setTransactionId("0000000000000099");
            dailyTxn.setCardNumber(CARD_NUMBER);
            dailyTxn.setAmount(new BigDecimal("100.00"));
            dailyTxn.setTimestamp("2026-05-01 10:00:00");

            var result = authorizationService.validateDailyTransaction(dailyTxn);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getReasonCode()).isEqualTo(103);
            assertThat(result.getDescription()).isEqualTo("TRANSACTION RECEIVED AFTER ACCT EXPIRATION");
        }

        @Test
        void shouldPostValidTransactionAndUpdateBalances() {
            Transaction dailyTxn = new Transaction();
            dailyTxn.setTransactionId("0000000000000099");
            dailyTxn.setCardNumber(CARD_NUMBER);
            dailyTxn.setAccountId(ACCOUNT_ID);
            dailyTxn.setTypeCode("01");
            dailyTxn.setCategoryCode("001");
            dailyTxn.setSource("POS");
            dailyTxn.setAmount(new BigDecimal("500.00"));
            dailyTxn.setTimestamp("2026-05-01 10:00:00");

            authorizationService.postDailyTransaction(dailyTxn);

            Account updated = accountRepository.findByAccountId(ACCOUNT_ID).orElseThrow();
            assertThat(updated.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(updated.getCurrentCycleCreditAmount()).isEqualByComparingTo(new BigDecimal("500.00"));

            var catBal = tcatBalRepository.findByAccountIdAndTypeCodeAndCategoryCode(ACCOUNT_ID, "01", "001");
            assertThat(catBal).isPresent();
            assertThat(catBal.get().getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
        }

        @Test
        void shouldCreateNewCategoryBalanceIfNotExists() {
            Transaction dailyTxn = new Transaction();
            dailyTxn.setTransactionId("0000000000000099");
            dailyTxn.setCardNumber(CARD_NUMBER);
            dailyTxn.setAccountId(ACCOUNT_ID);
            dailyTxn.setTypeCode("02");
            dailyTxn.setCategoryCode("003");
            dailyTxn.setSource("POS");
            dailyTxn.setAmount(new BigDecimal("200.00"));
            dailyTxn.setTimestamp("2026-05-01 10:00:00");

            authorizationService.postDailyTransaction(dailyTxn);

            var catBal = tcatBalRepository.findByAccountIdAndTypeCodeAndCategoryCode(ACCOUNT_ID, "02", "003");
            assertThat(catBal).isPresent();
            assertThat(catBal.get().getBalance()).isEqualByComparingTo(new BigDecimal("200.00"));
        }

        @Test
        void shouldAddToCategoryBalanceIfExists() {
            TransactionCategoryBalance existing = new TransactionCategoryBalance();
            existing.setAccountId(ACCOUNT_ID);
            existing.setTypeCode("01");
            existing.setCategoryCode("001");
            existing.setBalance(new BigDecimal("300.00"));
            tcatBalRepository.save(existing);

            Transaction dailyTxn = new Transaction();
            dailyTxn.setTransactionId("0000000000000099");
            dailyTxn.setCardNumber(CARD_NUMBER);
            dailyTxn.setAccountId(ACCOUNT_ID);
            dailyTxn.setTypeCode("01");
            dailyTxn.setCategoryCode("001");
            dailyTxn.setSource("POS");
            dailyTxn.setAmount(new BigDecimal("200.00"));
            dailyTxn.setTimestamp("2026-05-01 10:00:00");

            authorizationService.postDailyTransaction(dailyTxn);

            var catBal = tcatBalRepository.findByAccountIdAndTypeCodeAndCategoryCode(ACCOUNT_ID, "01", "001");
            assertThat(catBal.get().getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
        }
    }
}
