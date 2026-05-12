package com.carddemo.integration;

import com.carddemo.dto.LoginRequest;
import com.carddemo.dto.LoginResponse;
import com.carddemo.entity.UserSecurity;
import com.carddemo.repository.UserSecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserSecurityRepository userSecurityRepository;

    @BeforeEach
    void setUp() {
        userSecurityRepository.deleteAll();
        userSecurityRepository.save(createUser("ADMIN001", "PASSWORDA", "Admin", "One", "A"));
        userSecurityRepository.save(createUser("ADMIN002", "PASSWORDA", "Admin", "Two", "A"));
        userSecurityRepository.save(createUser("USER0001", "PASSWORDU", "User", "One", "U"));
        userSecurityRepository.save(createUser("USER0002", "PASSWORDU", "User", "Two", "U"));
    }

    private UserSecurity createUser(String userId, String password, String firstName, String lastName, String userType) {
        UserSecurity user = new UserSecurity();
        user.setUserId(userId);
        user.setPassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUserType(userType);
        return user;
    }

    @Nested
    class SuccessfulAuthentication {

        @Test
        void shouldAuthenticateAdminWithValidCredentials() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("ADMIN001", "PASSWORDA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.userType").value("A"))
                .andExpect(jsonPath("$.userId").value("ADMIN001"))
                .andReturn();

            assertThat(result.getResponse().getCookie("SESSION")).isNotNull();
        }

        @Test
        void shouldAuthenticateRegularUserWithValidCredentials() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("USER0001", "PASSWORDU")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.userType").value("U"))
                .andExpect(jsonPath("$.userId").value("USER0001"));
        }

        @Test
        void shouldAuthenticateCaseInsensitiveUserId() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("admin001", "PASSWORDA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.userId").value("ADMIN001"));
        }

        @Test
        void shouldAuthenticateCaseInsensitivePassword() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("ADMIN001", "passworda")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void shouldAuthenticateMixedCaseCredentials() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("AdMin001", "PaSSworDA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.userType").value("A"));
        }
    }

    @Nested
    class FailedAuthentication {

        @Test
        void shouldRejectEmptyUserId() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("", "PASSWORDA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Please enter User ID"));
        }

        @Test
        void shouldRejectNullUserId() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson(null, "PASSWORDA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Please enter User ID"));
        }

        @Test
        void shouldRejectEmptyPassword() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("ADMIN001", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Please enter Password"));
        }

        @Test
        void shouldRejectNullPassword() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("ADMIN001", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Please enter Password"));
        }

        @Test
        void shouldRejectNonExistentUser() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("UNKNOWN1", "WHATEVER")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found. Try again"));
        }

        @Test
        void shouldRejectWrongPassword() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("ADMIN001", "WRONGPASS")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Wrong Password. Try again"));
        }

        @Test
        void shouldNotCreateSessionOnFailedLogin() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("ADMIN001", "WRONGPASS")))
                .andExpect(status().isUnauthorized())
                .andReturn();

            Cookie sessionCookie = result.getResponse().getCookie("SESSION");
            assertThat(sessionCookie == null || sessionCookie.getValue().isEmpty()).isTrue();
        }
    }

    @Nested
    class SessionManagement {

        @Test
        void shouldCreateSessionContextOnSuccessfulLogin() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("ADMIN001", "PASSWORDA")))
                .andExpect(status().isOk())
                .andReturn();

            assertThat(result.getResponse().getContentAsString())
                .contains("ADMIN001")
                .contains("A");
        }

        @Test
        void shouldIncludeCsrfTokenInResponse() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("ADMIN001", "PASSWORDA")))
                .andExpect(status().isOk())
                .andReturn();

            String response = result.getResponse().getContentAsString();
            LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
            assertThat(loginResponse.getCsrfToken()).isNotNull();
            assertThat(loginResponse.getCsrfToken()).isNotEmpty();
        }

        @Test
        void shouldRegenerateSessionIdOnLogin() throws Exception {
            MvcResult firstLogin = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("ADMIN001", "PASSWORDA")))
                .andExpect(status().isOk())
                .andReturn();

            String firstSessionId = firstLogin.getResponse().getCookie("SESSION") != null ?
                firstLogin.getResponse().getCookie("SESSION").getValue() : "";

            MvcResult secondLogin = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("ADMIN001", "PASSWORDA")))
                .andExpect(status().isOk())
                .andReturn();

            String secondSessionId = secondLogin.getResponse().getCookie("SESSION") != null ?
                secondLogin.getResponse().getCookie("SESSION").getValue() : "";

            assertThat(firstSessionId).isNotEqualTo(secondSessionId);
        }

        @Test
        void shouldInvalidateSessionOnLogout() throws Exception {
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("ADMIN001", "PASSWORDA")))
                .andExpect(status().isOk())
                .andReturn();

            Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION");

            mockMvc.perform(post("/api/auth/logout")
                    .cookie(sessionCookie))
                .andExpect(status().isOk());

            mockMvc.perform(post("/api/account/update")
                    .cookie(sessionCookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class BruteForceProtection {

        @Test
        void shouldLockAccountAfter5FailedAttempts() throws Exception {
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ADMIN001", "WRONG")))
                    .andExpect(status().isUnauthorized());
            }

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("ADMIN001", "PASSWORDA")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many attempts. Try again in 15 minutes"));
        }

        @Test
        void shouldNotLockAfter4FailedAttempts() throws Exception {
            for (int i = 0; i < 4; i++) {
                mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("USER0001", "WRONG")))
                    .andExpect(status().isUnauthorized());
            }

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("USER0001", "PASSWORDU")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void shouldNotAffectOtherUsersWhenOneLocked() throws Exception {
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ADMIN001", "WRONG")))
                    .andExpect(status().isUnauthorized());
            }

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("ADMIN002", "PASSWORDA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    class SecurityHeaders {

        @Test
        void shouldSetSecureCookieFlags() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("ADMIN001", "PASSWORDA")))
                .andExpect(status().isOk())
                .andReturn();

            Cookie sessionCookie = result.getResponse().getCookie("SESSION");
            if (sessionCookie != null) {
                assertThat(sessionCookie.isHttpOnly()).isTrue();
                assertThat(sessionCookie.getSecure()).isTrue();
            }
        }

        @Test
        void shouldNeverStorePasswordInPlaintext() {
            UserSecurity user = userSecurityRepository.findByUserId("ADMIN001").orElseThrow();
            assertThat(user.getPasswordHash()).isNotEqualTo("PASSWORDA");
            assertThat(user.getPasswordHash()).startsWith("$2");
        }
    }

    @Nested
    class RoleBasedRouting {

        @Test
        void shouldReturnAdminStatusForAdminUser() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("ADMIN001", "PASSWORDA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS_ADMIN"));
        }

        @Test
        void shouldReturnUserStatusForRegularUser() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("USER0001", "PASSWORDU")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS_USER"));
        }
    }

    private String loginJson(String userId, String password) {
        try {
            LoginRequest request = new LoginRequest();
            request.setUserId(userId);
            request.setPassword(password);
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
