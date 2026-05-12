package com.carddemo.integration;

import com.carddemo.entity.TransactionType;
import com.carddemo.repository.TransactionTypeRepository;
import com.carddemo.repository.TransactionCategoryRepository;
import com.carddemo.repository.UserSecurityRepository;
import com.carddemo.entity.UserSecurity;
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
class ReferenceDataIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionTypeRepository transactionTypeRepository;

    @Autowired
    private TransactionCategoryRepository transactionCategoryRepository;

    @Autowired
    private UserSecurityRepository userSecurityRepository;

    private String adminSessionCookie;
    private String adminCsrfToken;

    @BeforeEach
    void setUp() throws Exception {
        transactionTypeRepository.deleteAll();
        userSecurityRepository.deleteAll();

        UserSecurity admin = new UserSecurity();
        admin.setUserId("ADMIN001");
        admin.setPassword("PASSWORDA");
        admin.setFirstName("Admin");
        admin.setLastName("One");
        admin.setUserType("A");
        userSecurityRepository.save(admin);

        for (int i = 1; i <= 15; i++) {
            TransactionType type = new TransactionType();
            type.setTypeCode(String.format("%02d", i));
            type.setDescription("Transaction Type " + i);
            transactionTypeRepository.save(type);
        }

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
    class TransactionTypeList {

        @Test
        void shouldDisplayFirstPageOfTransactionTypes() throws Exception {
            mockMvc.perform(get("/api/reference/transaction-types")
                    .param("page", "0")
                    .param("size", "7")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(7));
        }

        @Test
        void shouldPaginateTransactionTypes() throws Exception {
            mockMvc.perform(get("/api/reference/transaction-types")
                    .param("page", "1")
                    .param("size", "7")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(7));

            mockMvc.perform(get("/api/reference/transaction-types")
                    .param("page", "2")
                    .param("size", "7")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        void shouldFilterByTypeCode() throws Exception {
            mockMvc.perform(get("/api/reference/transaction-types")
                    .param("typeCode", "01")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].typeCode").value("01"));
        }

        @Test
        void shouldRejectNonNumericTypeCodeFilter() throws Exception {
            mockMvc.perform(get("/api/reference/transaction-types")
                    .param("typeCode", "AB")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnEmptyForNoMatchingRecords() throws Exception {
            mockMvc.perform(get("/api/reference/transaction-types")
                    .param("typeCode", "99")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
        }

        @Test
        void shouldRequireAdminRole() throws Exception {
            UserSecurity regularUser = new UserSecurity();
            regularUser.setUserId("USER0001");
            regularUser.setPassword("PASSWORDU");
            regularUser.setFirstName("Reg");
            regularUser.setLastName("User");
            regularUser.setUserType("U");
            userSecurityRepository.save(regularUser);

            MvcResult userLogin = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"USER0001\",\"password\":\"PASSWORDU\"}"))
                .andExpect(status().isOk())
                .andReturn();

            String userSession = userLogin.getResponse().getCookie("SESSION").getValue();

            mockMvc.perform(get("/api/reference/transaction-types")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", userSession)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    class TransactionTypeCreate {

        @Test
        void shouldCreateNewTransactionType() throws Exception {
            mockMvc.perform(post("/api/reference/transaction-types")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"typeCode\":\"99\",\"description\":\"New Type\"}"))
                .andExpect(status().isCreated());

            assertThat(transactionTypeRepository.findByTypeCode("99")).isPresent();
            assertThat(transactionTypeRepository.findByTypeCode("99").get().getDescription()).isEqualTo("New Type");
        }

        @Test
        void shouldRejectNonNumericTypeCode() throws Exception {
            mockMvc.perform(post("/api/reference/transaction-types")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"typeCode\":\"AB\",\"description\":\"Invalid\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectZeroTypeCode() throws Exception {
            mockMvc.perform(post("/api/reference/transaction-types")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"typeCode\":\"00\",\"description\":\"Zero\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectEmptyDescription() throws Exception {
            mockMvc.perform(post("/api/reference/transaction-types")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"typeCode\":\"99\",\"description\":\"\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectNonAlphabeticDescription() throws Exception {
            mockMvc.perform(post("/api/reference/transaction-types")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"typeCode\":\"99\",\"description\":\"Type @#$ Invalid\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectDuplicateTypeCode() throws Exception {
            mockMvc.perform(post("/api/reference/transaction-types")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"typeCode\":\"01\",\"description\":\"Duplicate\"}"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    class TransactionTypeUpdate {

        @Test
        void shouldUpdateDescription() throws Exception {
            mockMvc.perform(put("/api/reference/transaction-types/01")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"description\":\"Updated Description\"}"))
                .andExpect(status().isOk());

            TransactionType updated = transactionTypeRepository.findByTypeCode("01").orElseThrow();
            assertThat(updated.getDescription()).isEqualTo("Updated Description");
        }

        @Test
        void shouldDetectNoChanges() throws Exception {
            mockMvc.perform(put("/api/reference/transaction-types/01")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"description\":\"Transaction Type 1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No changes detected"));
        }

        @Test
        void shouldReturnNotFoundForNonExistentType() throws Exception {
            mockMvc.perform(put("/api/reference/transaction-types/99")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"description\":\"Nonexistent\"}"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    class TransactionTypeDelete {

        @Test
        void shouldDeleteTransactionType() throws Exception {
            mockMvc.perform(delete("/api/reference/transaction-types/15")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isOk());

            assertThat(transactionTypeRepository.findByTypeCode("15")).isEmpty();
        }

        @Test
        void shouldReturnNotFoundForDeletingNonExistentType() throws Exception {
            mockMvc.perform(delete("/api/reference/transaction-types/99")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isNotFound());
        }

        @Test
        void shouldRejectDeleteWithDependentRecords() throws Exception {
            mockMvc.perform(delete("/api/reference/transaction-types/01")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete, dependent records exist"));
        }
    }

    @Nested
    class TransactionTypeMaintenance {

        @Test
        void shouldSearchByTypeCode() throws Exception {
            mockMvc.perform(get("/api/reference/transaction-types/05")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeCode").value("05"))
                .andExpect(jsonPath("$.description").value("Transaction Type 5"));
        }

        @Test
        void shouldReturnNotFoundForNonExistentSearch() throws Exception {
            mockMvc.perform(get("/api/reference/transaction-types/99")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isNotFound());
        }

        @ParameterizedTest
        @ValueSource(strings = {"Valid Description", "Purchase", "Cash Advance", "Balance Transfer"})
        void shouldAcceptValidDescriptions(String description) throws Exception {
            mockMvc.perform(put("/api/reference/transaction-types/01")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"description\":\"" + description + "\"}"))
                .andExpect(status().isOk());
        }
    }
}
