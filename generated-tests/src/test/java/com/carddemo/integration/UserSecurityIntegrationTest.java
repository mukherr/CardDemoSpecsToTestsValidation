package com.carddemo.integration;

import com.carddemo.entity.UserSecurity;
import com.carddemo.repository.UserSecurityRepository;
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
class UserSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserSecurityRepository userSecurityRepository;

    private String adminSessionCookie;
    private String adminCsrfToken;

    @BeforeEach
    void setUp() throws Exception {
        userSecurityRepository.deleteAll();

        for (int i = 1; i <= 5; i++) {
            UserSecurity admin = new UserSecurity();
            admin.setUserId(String.format("ADMIN%03d", i));
            admin.setPassword("PASSWORDA");
            admin.setFirstName("Admin");
            admin.setLastName("User" + i);
            admin.setUserType("A");
            userSecurityRepository.save(admin);
        }
        for (int i = 1; i <= 5; i++) {
            UserSecurity user = new UserSecurity();
            user.setUserId(String.format("USER%04d", i));
            user.setPassword("PASSWORDU");
            user.setFirstName("Regular");
            user.setLastName("User" + i);
            user.setUserType("U");
            userSecurityRepository.save(user);
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
    class UserListBrowse {

        @Test
        void shouldDisplayFirstPageOfUsers() throws Exception {
            mockMvc.perform(get("/api/users")
                    .param("page", "0")
                    .param("size", "10")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(10));
        }

        @Test
        void shouldPaginateUserList() throws Exception {
            mockMvc.perform(get("/api/users")
                    .param("page", "1")
                    .param("size", "10")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isOk());
        }

        @Test
        void shouldDisplayUserIdFirstNameLastNameAndType() throws Exception {
            mockMvc.perform(get("/api/users")
                    .param("page", "0")
                    .param("size", "10")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").isNotEmpty())
                .andExpect(jsonPath("$.content[0].firstName").isNotEmpty())
                .andExpect(jsonPath("$.content[0].lastName").isNotEmpty())
                .andExpect(jsonPath("$.content[0].userType").isNotEmpty());
        }

        @Test
        void shouldRequireAdminAccess() throws Exception {
            MvcResult userLogin = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"USER0001\",\"password\":\"PASSWORDU\"}"))
                .andExpect(status().isOk())
                .andReturn();

            String userSession = userLogin.getResponse().getCookie("SESSION") != null ?
                userLogin.getResponse().getCookie("SESSION").getValue() : "";

            mockMvc.perform(get("/api/users")
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", userSession)))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    class AddUser {

        @Test
        void shouldCreateUserWithValidData() throws Exception {
            mockMvc.perform(post("/api/users")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"NEWUSER1\",\"firstName\":\"New\",\"lastName\":\"User\",\"password\":\"Pass1234!\",\"userType\":\"U\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

            assertThat(userSecurityRepository.findByUserId("NEWUSER1")).isPresent();
        }

        @Test
        void shouldRejectEmptyFirstName() throws Exception {
            mockMvc.perform(post("/api/users")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"NEWUSER1\",\"firstName\":\"\",\"lastName\":\"User\",\"password\":\"Pass1234!\",\"userType\":\"U\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("First Name can NOT be empty"));
        }

        @Test
        void shouldRejectEmptyLastName() throws Exception {
            mockMvc.perform(post("/api/users")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"NEWUSER1\",\"firstName\":\"New\",\"lastName\":\"\",\"password\":\"Pass1234!\",\"userType\":\"U\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Last Name can NOT be empty"));
        }

        @Test
        void shouldRejectEmptyUserId() throws Exception {
            mockMvc.perform(post("/api/users")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"\",\"firstName\":\"New\",\"lastName\":\"User\",\"password\":\"Pass1234!\",\"userType\":\"U\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User ID can NOT be empty"));
        }

        @Test
        void shouldRejectEmptyPassword() throws Exception {
            mockMvc.perform(post("/api/users")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"NEWUSER1\",\"firstName\":\"New\",\"lastName\":\"User\",\"password\":\"\",\"userType\":\"U\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password can NOT be empty"));
        }

        @Test
        void shouldRejectEmptyUserType() throws Exception {
            mockMvc.perform(post("/api/users")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"NEWUSER1\",\"firstName\":\"New\",\"lastName\":\"User\",\"password\":\"Pass1234!\",\"userType\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User Type can NOT be empty"));
        }

        @Test
        void shouldRejectDuplicateUserId() throws Exception {
            mockMvc.perform(post("/api/users")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"ADMIN001\",\"firstName\":\"Dup\",\"lastName\":\"User\",\"password\":\"Pass1234!\",\"userType\":\"A\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User ID already exist"));
        }

        @Test
        void shouldRejectInvalidUserType() throws Exception {
            mockMvc.perform(post("/api/users")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"NEWUSER1\",\"firstName\":\"New\",\"lastName\":\"User\",\"password\":\"Pass1234!\",\"userType\":\"X\"}"))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class UpdateUser {

        @Test
        void shouldUpdateUserFirstName() throws Exception {
            mockMvc.perform(put("/api/users/USER0001")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"firstName\":\"Updated\",\"lastName\":\"User1\",\"password\":\"PASSWORDU\",\"userType\":\"U\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User USER0001 has been updated"));

            UserSecurity updated = userSecurityRepository.findByUserId("USER0001").orElseThrow();
            assertThat(updated.getFirstName()).isEqualTo("Updated");
        }

        @Test
        void shouldDetectNoChanges() throws Exception {
            mockMvc.perform(put("/api/users/USER0001")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"firstName\":\"Regular\",\"lastName\":\"User1\",\"password\":\"PASSWORDU\",\"userType\":\"U\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Please modify to update"));
        }

        @Test
        void shouldReturnNotFoundForNonExistentUser() throws Exception {
            mockMvc.perform(put("/api/users/NOEXIST1")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"firstName\":\"X\",\"lastName\":\"Y\",\"password\":\"Pass1234!\",\"userType\":\"U\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User ID NOT found"));
        }

        @Test
        void shouldRejectEmptyUserIdOnUpdate() throws Exception {
            mockMvc.perform(put("/api/users/ ")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"firstName\":\"X\",\"lastName\":\"Y\",\"password\":\"Pass1234!\",\"userType\":\"U\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User ID can NOT be empty"));
        }
    }

    @Nested
    class DeleteUser {

        @Test
        void shouldDeleteExistingUser() throws Exception {
            mockMvc.perform(delete("/api/users/USER0005")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

            assertThat(userSecurityRepository.findByUserId("USER0005")).isEmpty();
        }

        @Test
        void shouldReturnNotFoundForNonExistentUserDeletion() throws Exception {
            mockMvc.perform(delete("/api/users/NOEXIST1")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isNotFound());
        }

        @Test
        void shouldPreventSelfDeletion() throws Exception {
            mockMvc.perform(delete("/api/users/ADMIN001")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class PasswordSecurity {

        @Test
        void shouldRejectPasswordShorterThan8Characters() throws Exception {
            mockMvc.perform(post("/api/users")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"NEWUSER1\",\"firstName\":\"New\",\"lastName\":\"User\",\"password\":\"Ab1!\",\"userType\":\"U\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectPasswordWithoutUppercase() throws Exception {
            mockMvc.perform(post("/api/users")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"NEWUSER1\",\"firstName\":\"New\",\"lastName\":\"User\",\"password\":\"abcdefg1!\",\"userType\":\"U\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectPasswordWithoutLowercase() throws Exception {
            mockMvc.perform(post("/api/users")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"NEWUSER1\",\"firstName\":\"New\",\"lastName\":\"User\",\"password\":\"ABCDEFG1!\",\"userType\":\"U\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectPasswordWithoutDigit() throws Exception {
            mockMvc.perform(post("/api/users")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"NEWUSER1\",\"firstName\":\"New\",\"lastName\":\"User\",\"password\":\"Abcdefgh!\",\"userType\":\"U\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectPasswordWithoutSpecialChar() throws Exception {
            mockMvc.perform(post("/api/users")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"NEWUSER1\",\"firstName\":\"New\",\"lastName\":\"User\",\"password\":\"Abcdefg12\",\"userType\":\"U\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldStorePasswordAsHash() throws Exception {
            mockMvc.perform(post("/api/users")
                    .header("X-CSRF-TOKEN", adminCsrfToken)
                    .cookie(new jakarta.servlet.http.Cookie("SESSION", adminSessionCookie))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"NEWUSER1\",\"firstName\":\"New\",\"lastName\":\"User\",\"password\":\"SecureP@ss1\",\"userType\":\"U\"}"))
                .andExpect(status().isCreated());

            UserSecurity created = userSecurityRepository.findByUserId("NEWUSER1").orElseThrow();
            assertThat(created.getPasswordHash()).isNotEqualTo("SecureP@ss1");
            assertThat(created.getPasswordHash()).startsWith("$2");
        }
    }

    @Nested
    class SeedData {

        @Test
        void shouldHaveFiveAdminUsersAfterInit() {
            long adminCount = userSecurityRepository.findAll().stream()
                .filter(u -> "A".equals(u.getUserType()))
                .count();
            assertThat(adminCount).isEqualTo(5);
        }

        @Test
        void shouldHaveFiveRegularUsersAfterInit() {
            long userCount = userSecurityRepository.findAll().stream()
                .filter(u -> "U".equals(u.getUserType()))
                .count();
            assertThat(userCount).isEqualTo(5);
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMIN001", "ADMIN002", "ADMIN003", "ADMIN004", "ADMIN005"})
        void shouldHaveAdminUsers(String userId) {
            assertThat(userSecurityRepository.findByUserId(userId)).isPresent();
            assertThat(userSecurityRepository.findByUserId(userId).get().getUserType()).isEqualTo("A");
        }

        @ParameterizedTest
        @ValueSource(strings = {"USER0001", "USER0002", "USER0003", "USER0004", "USER0005"})
        void shouldHaveRegularUsers(String userId) {
            assertThat(userSecurityRepository.findByUserId(userId)).isPresent();
            assertThat(userSecurityRepository.findByUserId(userId).get().getUserType()).isEqualTo("U");
        }
    }
}
