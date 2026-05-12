package com.carddemo.e2e;

import com.carddemo.entity.*;
import com.carddemo.repository.*;
import com.carddemo.service.InterestCalculationService;
import com.carddemo.service.StatementGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CardDemoEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionCategoryBalanceRepository tcatBalRepository;

    @Autowired
    private DiscountGroupRepository discountGroupRepository;

    @Autowired
    private UserSecurityRepository userSecurityRepository;

    @Autowired
    private InterestCalculationService interestCalculationService;

    @Autowired
    private StatementGenerationService statementGenerationService;

    private static final String ACCOUNT_ID = "00000000001";
    private static final String CARD_NUMBER = "4111111111111111";
    private static final String CUSTOMER_ID = "000000001";

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        tcatBalRepository.deleteAll();
        discountGroupRepository.deleteAll();
        cardXrefRepository.deleteAll();
        cardRepository.deleteAll();
        customerRepository.deleteAll();
        accountRepository.deleteAll();
        userSecurityRepository.deleteAll();

        UserSecurity admin = new UserSecurity();
        admin.setUserId("ADMIN001");
        admin.setPassword("PASSWORDA");
        admin.setFirstName("Admin");
        admin.setLastName("One");
        admin.setUserType("A");
        userSecurityRepository.save(admin);

        UserSecurity user = new UserSecurity();
        user.setUserId("USER0001");
        user.setPassword("PASSWORDU");
        user.setFirstName("Regular");
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
        account.setReissueDate("2025-01-01");
        account.setGroupId("DEFAULT");
        account.setAddressZip("90210");
        accountRepository.save(account);

        Customer customer = new Customer();
        customer.setCustomerId(CUSTOMER_ID);
        customer.setFirstName("John");
        customer.setMiddleName("M");
        customer.setLastName("Doe");
        customer.setAddressLine1("123 Main Street");
        customer.setAddressLine2("Apt 4B");
        customer.setStateCode("CA");
        customer.setCountryCode("US");
        customer.setZipCode("90210");
        customer.setPhoneNumber1("(310)555-1234");
        customer.setFicoScore(750);
        customer.setDateOfBirth("1985-06-15");
        customerRepository.save(customer);

        Card card = new Card();
        card.setCardNumber(CARD_NUMBER);
        card.setActiveStatus("Y");
        card.setEmbossedName("JOHN M DOE");
        card.setExpiryDate("2027-12-31");
        cardRepository.save(card);

        CardXref xref = new CardXref();
        xref.setCardNumber(CARD_NUMBER);
        xref.setAccountId(ACCOUNT_ID);
        xref.setCustomerId(CUSTOMER_ID);
        cardXrefRepository.save(xref);

        DiscountGroup discountGroup = new DiscountGroup();
        discountGroup.setGroupId("DEFAULT");
        discountGroup.setTypeCode("02");
        discountGroup.setCategoryCode("002");
        discountGroup.setInterestRate(new BigDecimal("18.0000"));
        discountGroupRepository.save(discountGroup);
    }

    @Nested
    class PaymentToInterestToStatementFlow {

        @Test
        void shouldCompleteFullPaymentCycle() throws Exception {
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"USER0001\",\"password\":\"PASSWORDU\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.userType").value("U"))
                .andReturn();

            String session = loginResult.getResponse().getCookie("SESSION").getValue();
            String csrf = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("csrfToken").asText();

            mockMvc.perform(post("/api/billing/payment/confirm")
                    .header("X-CSRF-TOKEN", csrf)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", session))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"cardNumber\":\"" + CARD_NUMBER + "\",\"amount\":500.00,\"confirm\":\"Y\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.transactionId").isNotEmpty());

            Account afterPayment = accountRepository.findByAccountId(ACCOUNT_ID).orElseThrow();
            assertThat(afterPayment.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("4500.00"));
            assertThat(afterPayment.getCurrentCycleDebitAmount()).isEqualByComparingTo(new BigDecimal("500.00"));

            assertThat(transactionRepository.count()).isEqualTo(1);
            Transaction paymentTxn = transactionRepository.findAll().get(0);
            assertThat(paymentTxn.getTypeCode()).isEqualTo("02");
            assertThat(paymentTxn.getCategoryCode()).isEqualTo("2");
            assertThat(paymentTxn.getAmount()).isEqualByComparingTo(new BigDecimal("-500.00"));
        }

        @Test
        void shouldCalculateInterestAndGenerateStatement() throws Exception {
            TransactionCategoryBalance catBal = new TransactionCategoryBalance();
            catBal.setAccountId(ACCOUNT_ID);
            catBal.setTypeCode("02");
            catBal.setCategoryCode("002");
            catBal.setBalance(new BigDecimal("1000.00"));
            tcatBalRepository.save(catBal);

            interestCalculationService.calculateMonthlyInterest();

            Account afterInterest = accountRepository.findByAccountId(ACCOUNT_ID).orElseThrow();
            BigDecimal expectedInterest = new BigDecimal("1000.00")
                .multiply(new BigDecimal("18.0000"))
                .divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);
            assertThat(afterInterest.getCurrentBalance())
                .isEqualByComparingTo(new BigDecimal("5000.00").add(expectedInterest));
            assertThat(afterInterest.getCurrentCycleCreditAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(afterInterest.getCurrentCycleDebitAmount()).isEqualByComparingTo(BigDecimal.ZERO);

            var systemTxns = transactionRepository.findAll().stream()
                .filter(t -> "01".equals(t.getTypeCode()) && "05".equals(t.getCategoryCode()))
                .toList();
            assertThat(systemTxns).hasSize(1);
            assertThat(systemTxns.get(0).getSource()).isEqualTo("System");
            assertThat(systemTxns.get(0).getDetails()).contains(ACCOUNT_ID);

            var statementResult = statementGenerationService.generateStatements();
            assertThat(statementResult.getPrintStatement()).isNotEmpty();
            assertThat(statementResult.getHtmlStatement()).contains("John");
            assertThat(statementResult.getHtmlStatement()).contains("Doe");
            assertThat(statementResult.getHtmlStatement()).contains(ACCOUNT_ID);
            assertThat(statementResult.getHtmlStatement()).contains("750");
        }
    }

    @Nested
    class UserManagementLifecycle {

        @Test
        void shouldCompleteUserLifecycle() throws Exception {
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"ADMIN001\",\"password\":\"PASSWORDA\"}"))
                .andExpect(status().isOk())
                .andReturn();

            String session = loginResult.getResponse().getCookie("SESSION").getValue();
            String csrf = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("csrfToken").asText();

            mockMvc.perform(post("/api/users")
                    .header("X-CSRF-TOKEN", csrf)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", session))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"TESTUSER\",\"firstName\":\"Test\",\"lastName\":\"User\",\"password\":\"Pass1234!\",\"userType\":\"U\"}"))
                .andExpect(status().isCreated());

            assertThat(userSecurityRepository.findByUserId("TESTUSER")).isPresent();
            assertThat(userSecurityRepository.findByUserId("TESTUSER").get().getUserType()).isEqualTo("U");

            mockMvc.perform(put("/api/users/TESTUSER")
                    .header("X-CSRF-TOKEN", csrf)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", session))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"firstName\":\"Test\",\"lastName\":\"Updated\",\"password\":\"Pass1234!\",\"userType\":\"A\"}"))
                .andExpect(status().isOk());

            UserSecurity updated = userSecurityRepository.findByUserId("TESTUSER").orElseThrow();
            assertThat(updated.getLastName()).isEqualTo("Updated");
            assertThat(updated.getUserType()).isEqualTo("A");

            mockMvc.perform(delete("/api/users/TESTUSER")
                    .header("X-CSRF-TOKEN", csrf)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", session)))
                .andExpect(status().isOk());

            assertThat(userSecurityRepository.findByUserId("TESTUSER")).isEmpty();
        }
    }

    @Nested
    class SecurityControlsFlow {

        @Test
        void shouldLockoutAfterFailedAttempts() throws Exception {
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"USER0001\",\"password\":\"WRONG\"}"))
                    .andExpect(status().isUnauthorized());
            }

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"USER0001\",\"password\":\"PASSWORDU\"}"))
                .andExpect(status().isTooManyRequests());
        }

        @Test
        void shouldPreventRegularUserFromAccessingAdminFunctions() throws Exception {
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"USER0001\",\"password\":\"PASSWORDU\"}"))
                .andExpect(status().isOk())
                .andReturn();

            String userSession = loginResult.getResponse().getCookie("SESSION").getValue();

            mockMvc.perform(get("/api/users")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", userSession)))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldPreventAccessToOtherUsersAccounts() throws Exception {
            Account otherAccount = new Account();
            otherAccount.setAccountId("99999999999");
            otherAccount.setActiveStatus("Y");
            otherAccount.setCurrentBalance(new BigDecimal("9999.00"));
            otherAccount.setCreditLimit(new BigDecimal("20000.00"));
            accountRepository.save(otherAccount);

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"USER0001\",\"password\":\"PASSWORDU\"}"))
                .andExpect(status().isOk())
                .andReturn();

            String userSession = loginResult.getResponse().getCookie("SESSION").getValue();

            mockMvc.perform(get("/api/account/99999999999")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", userSession)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found"));
        }

        @Test
        void shouldRequireCsrfOnAllStateChangingOperations() throws Exception {
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"USER0001\",\"password\":\"PASSWORDU\"}"))
                .andExpect(status().isOk())
                .andReturn();

            String session = loginResult.getResponse().getCookie("SESSION").getValue();

            mockMvc.perform(post("/api/billing/payment")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", session))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"" + ACCOUNT_ID + "\",\"amount\":100.00}"))
                .andExpect(status().isForbidden());
        }

        @Test
        void shouldMaskPiiInResponses() throws Exception {
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"ADMIN001\",\"password\":\"PASSWORDA\"}"))
                .andExpect(status().isOk())
                .andReturn();

            String adminSession = loginResult.getResponse().getCookie("SESSION").getValue();

            MvcResult accountResult = mockMvc.perform(get("/api/account/" + ACCOUNT_ID)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSession)))
                .andExpect(status().isOk())
                .andReturn();

            String responseBody = accountResult.getResponse().getContentAsString();
            assertThat(responseBody).doesNotContain("123-45-6789");
            if (responseBody.contains("ssn")) {
                assertThat(responseBody).contains("***-**-");
            }
        }
    }

    @Nested
    class CardManagementFlow {

        @Test
        void shouldListAndViewCardDetails() throws Exception {
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"USER0001\",\"password\":\"PASSWORDU\"}"))
                .andExpect(status().isOk())
                .andReturn();

            String session = loginResult.getResponse().getCookie("SESSION").getValue();

            mockMvc.perform(get("/api/cards")
                    .param("accountId", ACCOUNT_ID)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cardNumber").value(CARD_NUMBER))
                .andExpect(jsonPath("$.content[0].accountId").value(ACCOUNT_ID));

            mockMvc.perform(get("/api/cards/" + CARD_NUMBER)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value(CARD_NUMBER))
                .andExpect(jsonPath("$.embossedName").value("JOHN M DOE"))
                .andExpect(jsonPath("$.activeStatus").value("Y"));
        }
    }
}
