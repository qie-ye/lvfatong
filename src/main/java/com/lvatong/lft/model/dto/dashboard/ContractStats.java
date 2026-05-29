package com.lvatong.lft.model.dto.dashboard;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ContractStats {
    private long totalContracts;
    private List<Map<String, Object>> riskDistribution;
    private List<Map<String, Object>> dailyTrend;
}