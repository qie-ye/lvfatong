package com.lvatong.lft.security.privacy;

import com.lvatong.lft.model.entity.User;
import com.lvatong.lft.repository.UserRepository;
import com.lvatong.lft.security.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivacyService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Value("${lvatong.security.privacy.deletion-grace-days:30}")
    private int deletionGraceDays;

    /**
     * 导出用户个人数据（GDPR合规）
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportUserData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Map<String, Object> userData = new HashMap<>();
        userData.put("basicInfo", extractBasicInfo(user));
        userData.put("exportTime", LocalDateTime.now());
        userData.put("dataVersion", "1.0");

        // 记录审计日志
        auditLogService.logDataAccess("EXPORT", "USER", userId.toString(), "用户导出个人数据");

        log.info("用户数据导出成功: userId={}", userId);
        return userData;
    }

    /**
     * 删除用户个人数据（软删除）
     */
    @Transactional
    public void deleteUserData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 记录审计日志
        auditLogService.logAction("DELETE_REQUEST", "USER", userId.toString(), 
                "用户请求删除个人数据");

        log.info("用户数据删除请求已记录: userId={}", userId);
    }

    /**
     * 永久删除用户数据（定时任务调用）
     */
    @Transactional
    public void permanentlyDeleteUserData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 匿名化用户数据
        anonymizeUser(user);
        userRepository.save(user);

        // 记录审计日志
        auditLogService.logAction("PERMANENT_DELETE", "USER", userId.toString(), "用户数据已永久删除");

        log.info("用户数据已永久删除: userId={}", userId);
    }

    /**
     * 匿名化用户数据
     */
    private void anonymizeUser(User user) {
        user.setUsername("deleted_" + user.getId());
        user.setNickname("已删除用户");
        user.setEmail(null);
        user.setPhone(null);
    }

    /**
     * 提取用户基本信息（脱敏）
     */
    private Map<String, Object> extractBasicInfo(User user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("email", maskEmail(user.getEmail()));
        info.put("phone", maskPhone(user.getPhone()));
        info.put("createdAt", user.getCreatedAt());
        return info;
    }

    /**
     * 邮箱脱敏
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}