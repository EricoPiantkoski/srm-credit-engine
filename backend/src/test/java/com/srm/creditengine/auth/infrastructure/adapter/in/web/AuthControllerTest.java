package com.srm.creditengine.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.creditengine.auth.application.Login;
import com.srm.creditengine.auth.application.Logout;
import com.srm.creditengine.auth.application.Refresh;
import com.srm.creditengine.auth.domain.AccessToken;
import com.srm.creditengine.auth.domain.TokenPair;
import com.srm.creditengine.auth.domain.exception.InvalidCredentialsException;
import com.srm.creditengine.auth.domain.exception.InvalidRefreshTokenException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "ADMIN")
class AuthControllerTest {

    private static final Instant NOW = Instant.parse("2026-08-16T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Login login;

    @MockitoBean
    private Refresh refresh;

    @MockitoBean
    private Logout logout;

    @Test
    void loginReturns200WithTokens() throws Exception {
        when(login.login(eq("admin"), eq("admin123"), any()))
            .thenReturn(new TokenPair(new AccessToken("jwt", NOW.plusSeconds(900)), "refresh", NOW.plusSeconds(604800)));

        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("jwt"))
            .andExpect(jsonPath("$.refreshToken").value("refresh"))
            .andExpect(jsonPath("$.accessTokenExpiresAt").isNotEmpty());
    }

    @Test
    void loginReturns401OnInvalidCredentials() throws Exception {
        when(login.login(any(), any(), any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginReturns400OnBlankFields() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content("{\"username\":\"\",\"password\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void refreshReturns200WithNewTokens() throws Exception {
        when(refresh.refresh(eq("refresh-old"), any()))
            .thenReturn(new TokenPair(new AccessToken("jwt-new", NOW.plusSeconds(900)), "refresh-new", NOW.plusSeconds(604800)));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(APPLICATION_JSON)
                .content("{\"refreshToken\":\"refresh-old\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("jwt-new"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-new"));
    }

    @Test
    void refreshReturns401OnInvalidToken() throws Exception {
        when(refresh.refresh(any(), any())).thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(APPLICATION_JSON)
                .content("{\"refreshToken\":\"invalid\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutReturns204() throws Exception {
        doNothing().when(logout).logout("refresh-token");

        mockMvc.perform(post("/api/auth/logout")
                .contentType(APPLICATION_JSON)
                .content("{\"refreshToken\":\"refresh-token\"}"))
            .andExpect(status().isNoContent());
    }

    @Test
    void logoutReturns401OnInvalidToken() throws Exception {
        org.mockito.Mockito.doThrow(new InvalidRefreshTokenException()).when(logout).logout("invalid");

        mockMvc.perform(post("/api/auth/logout")
                .contentType(APPLICATION_JSON)
                .content("{\"refreshToken\":\"invalid\"}"))
            .andExpect(status().isUnauthorized());
    }
}