package com.lvatong.lft.controller;

import com.lvatong.lft.common.result.ApiResult;
import com.lvatong.lft.model.dto.ChangePasswordRequest;
import com.lvatong.lft.model.dto.UpdateProfileRequest;
import com.lvatong.lft.model.dto.UserProfileResponse;
import com.lvatong.lft.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "用户中心", description = "个人资料查看、编辑、密码修改")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "获取个人资料")
    public ApiResult<UserProfileResponse> getProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人资料")
    public ApiResult<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                         Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResult.success(userService.updateProfile(userId, request));
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public ApiResult<String> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                             Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        userService.changePassword(userId, request);
        return ApiResult.success("密码修改成功");
    }
}
