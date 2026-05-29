package com.lvatong.lft.model.dto.dashboard;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QueryStats {
    private long totalQueries;
    private List<Map<String, Object>> dailyTrend;
    private List<Map<String, Object>> peakHours;
}