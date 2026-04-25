package com.lvatong.lft.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.dto.AuthResponse;
import com.lvatong.lft.model.dto.LoginRequest;
import com.lvatong.lft.model.dto.RegisterRequest;
import com.lvatong.lft.security.JwtAccessDeniedHandler;
import com.lvatong.lft.security.JwtAuthEntryPoint;
import com.lvatong.lft.security.JwtTokenProvider;
import com.lvatong.lft.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthEntryPoint jwtAuthEntryPoint;

    @MockBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Test
    void register_shouldReturn400_whenUsernameIsBlank() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn400_whenPasswordIsTooShort() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("validUser");
        request.setPassword("abc");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn200_withValidRequest() throws Exception {
        AuthResponse mockResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(86400L)
                .username("newUser")
                .role("USER")
                .build();
        when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newUser");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.username").value("newUser"));
    }

    @Test
    void login_shouldReturn200_withValidCredentials() throws Exception {
        AuthResponse mockResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(86400L)
                .username("testUser")
                .role("USER")
                .build();
        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        LoginRequest request = new LoginRequest();
        request.setUsername("testUser");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("testUser"));
    }

    @Test
    void login_shouldReturn400_whenUsernameIsBlank() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn400_whenServiceThrowsBusinessException() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new BusinessException("用户名已存在"));

        RegisterRequest request = new RegisterRequest();
        request.setUsername("duplicateUser");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }
}
