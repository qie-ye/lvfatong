package com.lvatong.lft.controller;

import com.lvatong.lft.common.ratelimit.RateLimit;
import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.model.dto.*;
import com.lvatong.lft.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户注册、登录、token刷新")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @RateLimit(permitsPerSecond = 2.0, dimension = "IP", message = "注册请求过于频繁，请稍后再试")
    @Operation(summary = "用户注册")
    public ApiResult<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResult.success(authService.register(request));
    }

    @PostMapping("/login")
    @RateLimit(permitsPerSecond = 5.0, dimension = "IP", message = "登录请求过于频繁，请稍后再试")
    @Operation(summary = "用户登录")
    public ApiResult<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.success(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新token")
    public ApiResult<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResult.success(authService.refresh(request));
    }
}
