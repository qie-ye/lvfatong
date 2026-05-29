package com.lvatong.lft.security.audit;

import com.lvatong.lft.model.entity.AuditLog;
import com.lvatong.lft.repository.AuditLogRepository;
import com.lvatong.lft.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserService userService;

    /**
     * 记录审计日志
     */
    @Async
    public void log(String action, String resource, String resourceId, String details, 
                    Long userId, String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(action);
            auditLog.setResource(resource);
            auditLog.setDetail(details);
            auditLog.setUserId(userId);
            auditLog.setIp(ipAddress);
            auditLog.setSuccess(true);

            auditLogRepository.save(auditLog);
            log.debug("审计日志记录成功: {} - {} - {}", action, resource, resourceId);
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
        }
    }

    /**
     * 记录操作审计日志
     */
    @Async
    public void logAction(String action, String resource, String resourceId, String details) {
        try {
            log(action, resource, resourceId, details, null, null, null);
        } catch (Exception e) {
            log.error("记录操作审计日志失败", e);
        }
    }

    /**
     * 记录数据访问审计日志
     */
    @Async
    public void logDataAccess(String action, String resource, String resourceId, String details) {
        logAction("DATA_ACCESS_" + action, resource, resourceId, details);
    }

    /**
     * 记录安全事件
     */
    @Async
    public void logSecurityEvent(String event, String details, String ipAddress) {
        log("SECURITY_EVENT", "SYSTEM", event, details, null, ipAddress, null);
    }

    /**
     * 查询审计日志
     */
    public java.util.List<AuditLog> getAuditLogs(String resource, String action, 
                                                  LocalDateTime startTime, LocalDateTime endTime) {
        // 简化实现，返回所有日志
        return auditLogRepository.findAll();
    }

    /**
     * 导出审计日志
     */
    public byte[] exportAuditLogs(String resource, String action, 
                                  LocalDateTime startTime, LocalDateTime endTime) {
        java.util.List<AuditLog> logs = getAuditLogs(resource, action, startTime, endTime);
        // 简单的CSV格式导出
        StringBuilder csv = new StringBuilder();
        csv.append("ID,操作,资源,详情,用户ID,IP地址,时间\n");
        for (AuditLog logEntry : logs) {
            csv.append(String.format("%d,%s,%s,%s,%d,%s,%s\n",
                    logEntry.getId(),
                    logEntry.getAction(),
                    logEntry.getResource(),
                    logEntry.getDetail(),
                    logEntry.getUserId(),
                    logEntry.getIp(),
                    logEntry.getCreatedAt()));
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}