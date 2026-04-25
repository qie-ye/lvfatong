package com.lvatong.lft.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FeedbackStatsResponse {

    private long total;
    private long goodCount;
    private long badCount;
    private double satisfactionRate;

    public static FeedbackStatsResponse of(long good, long bad) {
        long total = good + bad;
        double rate = total == 0 ? 0.0 : Math.round((double) good / total * 1000) / 10.0;
        return new FeedbackStatsResponse(total, good, bad, rate);
    }
}
