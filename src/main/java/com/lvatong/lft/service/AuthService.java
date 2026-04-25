package com.lvatong.lft.service;

import com.lvatong.lft.common.audit.Auditable;
import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.dto.*;
import com.lvatong.lft.model.entity.User;
import com.lvatong.lft.repository.UserRepository;
import com.lvatong.lft.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    @Auditable(action = "USER_REGISTER", resource = "用户注册")
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(User.UserRole.USER);
        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    @Auditable(action = "USER_LOGIN", resource = "用户登录")
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(401, "无效的refreshToken");
        }
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
}
