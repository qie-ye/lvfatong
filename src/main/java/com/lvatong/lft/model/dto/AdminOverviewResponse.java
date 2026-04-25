package com.lvatong.lft.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminOverviewResponse {
    private long totalUsers;
    private long totalSessions;
    private long todayActive;
    private double satisfactionRate;
}
