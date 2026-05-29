package com.lvatong.lft.model.dto.dashboard;

import lombok.Data;

@Data
public class AIPerformance {
    private double satisfactionRate;
    private long totalFeedback;
    private long goodCount;
    private long badCount;
    private double estimatedAccuracy;
}