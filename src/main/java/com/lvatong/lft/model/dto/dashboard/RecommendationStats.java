package com.lvatong.lft.model.dto.dashboard;

import lombok.Data;

@Data
public class RecommendationStats {
    private long totalRecommendations;
    private long clickedRecommendations;
    private double clickRate;
    private double conversionRate;
}