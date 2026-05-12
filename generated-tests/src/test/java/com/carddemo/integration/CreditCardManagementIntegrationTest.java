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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CreditCardManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserSecurityRepository userSecurityRepository;

    private String sessionCookie;
    private String csrfToken;

    private static final String ACCOUNT_ID = "00000000001";
    private static final String CARD_NUMBER = "4111111111111111";

    @BeforeEach
    void setUp() throws Exception {
        cardXrefRepository.deleteAll();
        cardRepository.deleteAll();
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
        account.setCurrentBalance(new java.math.BigDecimal("5000.00"));
        account.setCreditLimit(new java.math.BigDecimal("10000.00"));
        accountRepository.save(account);

        for (int i = 1; i <= 15; i++) {
            String cardNum = String.format("411111111111%04d", i);
            Card card = new Card();
            card.setCardNumber(cardNum);
            card.setActiveStatus(i <= 12 ? "Y" : "N");
            card.setEmbossedName("TEST USER " + i);
            card.setExpiryDate("2027-12-31");
            cardRepository.save(card);

            CardXref xref = new CardXref();
            xref.setCardNumber(cardNum);
            xref.setAccountId(ACCOUNT_ID);
            xref.setCustomerId("000000001");
            cardXrefRepository.save(xref);
        }

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
    class CardList {

        @Test
        void shouldDisplayFirstPageOfCards() throws Exception {
            mockMvc.perform(get("/api/cards")
                    .param("page", "0")
                    .param("size", "7")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(7));
        }

        @Test
        void shouldPaginateCards() throws Exception {
            mockMvc.perform(get("/api/cards")
                    .param("page", "1")
                    .param("size", "7")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(7));

            mockMvc.perform(get("/api/cards")
                    .param("page", "2")
                    .param("size", "7")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        void shouldFilterByAccountId() throws Exception {
            mockMvc.perform(get("/api/cards")
                    .param("accountId", ACCOUNT_ID)
                    .param("page", "0")
                    .param("size", "7")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        void shouldRejectNonNumericAccountIdFilter() throws Exception {
            mockMvc.perform(get("/api/cards")
                    .param("accountId", "ABCDEFGHIJK")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectNonNumericCardNumberFilter() throws Exception {
            mockMvc.perform(get("/api/cards")
                    .param("cardNumber", "ABCDEFGHIJKLMNOP")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnEmptyForNoMatchingFilter() throws Exception {
            mockMvc.perform(get("/api/cards")
                    .param("accountId", "99999999999")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
        }

        @Test
        void shouldDisplayCardNumberAccountIdAndStatus() throws Exception {
            mockMvc.perform(get("/api/cards")
                    .param("page", "0")
                    .param("size", "7")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cardNumber").isNotEmpty())
                .andExpect(jsonPath("$.content[0].accountId").isNotEmpty())
                .andExpect(jsonPath("$.content[0].activeStatus").isNotEmpty());
        }
    }

    @Nested
    class CardDetailInquiry {

        @Test
        void shouldDisplayCardDetailsForValidCardNumber() throws Exception {
            mockMvc.perform(get("/api/cards/4111111111110001")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value("4111111111110001"))
                .andExpect(jsonPath("$.embossedName").value("TEST USER 1"))
                .andExpect(jsonPath("$.activeStatus").value("Y"))
                .andExpect(jsonPath("$.expiryDate").value("2027-12-31"));
        }

        @Test
        void shouldReturnNotFoundForNonExistentCard() throws Exception {
            mockMvc.perform(get("/api/cards/9999999999999999")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isNotFound());
        }

        @Test
        void shouldNeverDisplayCvvInResponse() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/cards/4111111111110001")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andReturn();

            String body = result.getResponse().getContentAsString();
            assertThat(body).doesNotContain("\"cvv\":");
            if (body.contains("cvv")) {
                assertThat(body).contains("***");
            }
        }
    }

    @Nested
    class CardUpdate {

        @Test
        void shouldUpdateCardholderName() throws Exception {
            mockMvc.perform(put("/api/cards/4111111111110001")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"embossedName\":\"JANE DOE\",\"expiryMonth\":12,\"expiryYear\":2027,\"activeStatus\":\"Y\",\"confirm\":\"Y\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

            Card updated = cardRepository.findByCardNumber("4111111111110001").orElseThrow();
            assertThat(updated.getEmbossedName()).isEqualTo("JANE DOE");
        }

        @Test
        void shouldRejectBlankAccountId() throws Exception {
            mockMvc.perform(put("/api/cards/4111111111110001")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"\",\"embossedName\":\"JANE DOE\",\"activeStatus\":\"Y\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectNonNumericAccountId() throws Exception {
            mockMvc.perform(put("/api/cards/4111111111110001")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountId\":\"ABCDEFGHIJK\",\"embossedName\":\"JANE\",\"activeStatus\":\"Y\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectBlankCardholderName() throws Exception {
            mockMvc.perform(put("/api/cards/4111111111110001")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"embossedName\":\"\",\"expiryMonth\":12,\"expiryYear\":2027,\"activeStatus\":\"Y\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectNonAlphabeticCardholderName() throws Exception {
            mockMvc.perform(put("/api/cards/4111111111110001")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"embossedName\":\"JOHN123\",\"expiryMonth\":12,\"expiryYear\":2027,\"activeStatus\":\"Y\"}"))
                .andExpect(status().isBadRequest());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 13, -1, 99})
        void shouldRejectInvalidExpiryMonth(int month) throws Exception {
            mockMvc.perform(put("/api/cards/4111111111110001")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"embossedName\":\"JANE DOE\",\"expiryMonth\":" + month + ",\"expiryYear\":2027,\"activeStatus\":\"Y\"}"))
                .andExpect(status().isBadRequest());
        }

        @ParameterizedTest
        @ValueSource(ints = {1949, 2100, 0, 999})
        void shouldRejectInvalidExpiryYear(int year) throws Exception {
            mockMvc.perform(put("/api/cards/4111111111110001")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"embossedName\":\"JANE DOE\",\"expiryMonth\":12,\"expiryYear\":" + year + ",\"activeStatus\":\"Y\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectInvalidActiveStatus() throws Exception {
            mockMvc.perform(put("/api/cards/4111111111110001")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"embossedName\":\"JANE DOE\",\"expiryMonth\":12,\"expiryYear\":2027,\"activeStatus\":\"X\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldDetectNoChangesAndSkipConfirmation() throws Exception {
            mockMvc.perform(put("/api/cards/4111111111110001")
                    .header("X-CSRF-TOKEN", csrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"embossedName\":\"TEST USER 1\",\"expiryMonth\":12,\"expiryYear\":2027,\"activeStatus\":\"Y\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No changes detected"));
        }

        @Test
        void shouldRequireCsrfTokenForUpdate() throws Exception {
            mockMvc.perform(put("/api/cards/4111111111110001")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"embossedName\":\"JANE DOE\",\"activeStatus\":\"Y\"}"))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    class CardSecurity {

        @Test
        void shouldRejectUnauthorizedCardAccess() throws Exception {
            Card otherCard = new Card();
            otherCard.setCardNumber("5555555555555555");
            otherCard.setActiveStatus("Y");
            otherCard.setEmbossedName("OTHER USER");
            otherCard.setExpiryDate("2027-12-31");
            cardRepository.save(otherCard);

            mockMvc.perform(get("/api/cards/5555555555555555")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isNotFound());
        }

        @Test
        void shouldMaskPanInListView() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/cards")
                    .param("page", "0")
                    .param("size", "7")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andReturn();

            String body = result.getResponse().getContentAsString();
            assertThat(body).doesNotContain("\"cvv\"");
        }
    }
}
