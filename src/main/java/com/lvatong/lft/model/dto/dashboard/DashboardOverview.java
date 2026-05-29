package com.lvatong.lft.model.dto.dashboard;

import lombok.Data;

@Data
public class DashboardOverview {
    private long totalUsers;
    private long dailyActiveUsers;
    private long weeklyActiveUsers;
    private long monthlyActiveUsers;
}