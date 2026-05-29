package com.lvatong.lft.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lvatong.lft.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardScheduler {

    private final DashboardService dashboardService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 每小时聚合一次Dashboard数据
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void aggregateDashboardData() {
        log.info("开始聚合Dashboard数据...");
        try {
            Map<String, Object> allData = dashboardService.getAllDashboardData();
            saveStats("hourly", allData);
            log.info("Dashboard数据聚合完成，数据项：{}", allData.size());
        } catch (Exception e) {
            log.error("Dashboard数据聚合失败", e);
        }
    }

    /**
     * 每天凌晨2点生成日报数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyReport() {
        log.info("开始生成每日Dashboard报告...");
        try {
            Map<String, Object> dailyReport = dashboardService.getAllDashboardData();
            saveStats("daily", dailyReport);
            log.info("每日Dashboard报告生成完成");
        } catch (Exception e) {
            log.error("每日Dashboard报告生成失败", e);
        }
    }

    /**
     * 保存统计数据到dashboard_stats表
     */
    private void saveStats(String statType, Map<String, Object> data) {
        try {
            String jsonValue = objectMapper.writeValueAsString(data);
            String sql = "INSERT INTO dashboard_stats (stat_date, stat_type, stat_value) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, LocalDate.now(), statType, jsonValue);
            log.debug("保存Dashboard统计成功：type={}", statType);
        } catch (Exception e) {
            log.warn("保存Dashboard统计失败：type={}, error={}", statType, e.getMessage());
        }
    }
}
