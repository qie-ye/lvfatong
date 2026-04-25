package com.lvatong.lft.common.audit;

import com.lvatong.lft.model.entity.AuditLog;
import com.lvatong.lft.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long startTime = System.currentTimeMillis();
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(auditable.action());
        auditLog.setResource(auditable.resource().isEmpty() ? joinPoint.getSignature().getName() : auditable.resource());

        // 获取请求信息
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                auditLog.setIp(getClientIp(request));
                auditLog.setMethod(request.getMethod());
                auditLog.setUri(request.getRequestURI());
            }
        } catch (Exception e) {
            log.debug("Failed to get request info for audit: {}", e.getMessage());
        }

        // 获取用户信息
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Long userId) {
                auditLog.setUserId(userId);
                if (auth.getCredentials() instanceof String username) {
                    auditLog.setUsername(username);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get user info for audit: {}", e.getMessage());
        }

        try {
            Object result = joinPoint.proceed();
            auditLog.setSuccess(true);
            auditLog.setDurationMs(System.currentTimeMillis() - startTime);
            saveAsync(auditLog);
            return result;
        } catch (Throwable e) {
            auditLog.setSuccess(false);
            auditLog.setDetail(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)) : "Unknown error");
            auditLog.setDurationMs(System.currentTimeMillis() - startTime);
            saveAsync(auditLog);
            throw e;
        }
    }

    private void saveAsync(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to save audit log: {}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
