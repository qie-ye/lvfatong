package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户注册、登录、token刷新")
public class AuthController {

    // TODO: v0.2 实现JWT认证
    // - POST /api/auth/register
    // - POST /api/auth/login
    // - POST /api/auth/refresh

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public ApiResult<Void> register() {
        return ApiResult.success();
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ApiResult<Void> login() {
        return ApiResult.success();
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新token")
    public ApiResult<Void> refresh() {
        return ApiResult.success();
    }
}
