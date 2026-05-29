package com.lvatong.lft.collaboration;

import com.lvatong.lft.model.entity.CaseActivityLog;
import com.lvatong.lft.repository.CaseActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final CaseActivityLogRepository caseActivityLogRepository;

    /**
     * 记录案件活动日志
     */
    @Async
    public void logActivity(Long caseId, Long userId, String action, String details) {
        try {
            CaseActivityLog activityLog = new CaseActivityLog();
            activityLog.setCaseId(caseId);
            activityLog.setUserId(userId);
            activityLog.setAction(action);
            activityLog.setDetails(details);

            caseActivityLogRepository.save(activityLog);
            log.debug("案件活动日志记录成功: caseId={}, action={}", caseId, action);
        } catch (Exception e) {
            log.error("记录案件活动日志失败", e);
        }
    }

    /**
     * 获取案件活动日志
     */
    public List<CaseActivityLog> getCaseActivityLogs(Long caseId) {
        return caseActivityLogRepository.findByCaseIdOrderByCreatedAtDesc(caseId);
    }

    /**
     * 获取案件最近活动日志
     */
    public List<CaseActivityLog> getRecentActivityLogs(Long caseId) {
        return caseActivityLogRepository.findTop20ByCaseIdOrderByCreatedAtDesc(caseId);
    }
}