package com.lvatong.lft.service;

import com.lvatong.lft.common.audit.Auditable;
import com.lvatong.lft.common.exception.BusinessException;
import com.lvatong.lft.model.dto.ChangePasswordRequest;
import com.lvatong.lft.model.dto.UpdateProfileRequest;
import com.lvatong.lft.model.dto.UserProfileResponse;
import com.lvatong.lft.model.entity.User;
import com.lvatong.lft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取用户信息
     */
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return UserProfileResponse.from(user);
    }

    /**
     * 更新用户资料
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        user = userRepository.save(user);
        log.info("User {} profile updated", userId);
        return UserProfileResponse.from(user);
    }

    /**
     * 修改密码
     */
    @Transactional
    @Auditable(action = "CHANGE_PASSWORD", resource = "修改密码")
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("User {} password changed", userId);
    }

    /**
     * 获取用户统计信息（对话数、合同数）
     */
    public UserStats getUserStats(Long userId) {
        return new UserStats(userId);
    }

    public record UserStats(Long userId) {}
}
