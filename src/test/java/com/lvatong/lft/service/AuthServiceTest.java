package com.lvatong.lft.service;

import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.dto.AuthResponse;
import com.lvatong.lft.model.dto.LoginRequest;
import com.lvatong.lft.model.dto.RegisterRequest;
import com.lvatong.lft.model.entity.User;
import com.lvatong.lft.repository.UserRepository;
import com.lvatong.lft.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldThrowBusinessException_whenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("existingUser")).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("existingUser");
        request.setPassword("password123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldReturnAuthResponse_whenSuccess() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("newUser");
        savedUser.setRole(User.UserRole.USER);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        when(jwtTokenProvider.generateAccessToken(1L, "newUser", "USER")).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(86400000L);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newUser");
        request.setPassword("password123");
        request.setNickname("Nick");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUsername()).isEqualTo("newUser");
        assertThat(response.getRole()).isEqualTo("USER");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void login_shouldThrowBusinessException_whenUserNotFound() {
        when(userRepository.findByUsername("unknownUser")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setUsername("unknownUser");
        request.setPassword("anyPassword");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    @Test
    void login_shouldThrowBusinessException_whenPasswordDoesNotMatch() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setPassword("encodedPassword");
        user.setRole(User.UserRole.USER);
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setUsername("testUser");
        request.setPassword("wrongPassword");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    @Test
    void login_shouldReturnAuthResponse_whenCredentialsAreValid() {
        User user = new User();
        user.setId(2L);
        user.setUsername("validUser");
        user.setPassword("encodedPassword");
        user.setRole(User.UserRole.USER);
        when(userRepository.findByUsername("validUser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(2L, "validUser", "USER")).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(2L)).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(86400000L);

        LoginRequest request = new LoginRequest();
        request.setUsername("validUser");
        request.setPassword("correctPassword");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUsername()).isEqualTo("validUser");
    }
}
