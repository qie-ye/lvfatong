package com.lvatong.lft.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DailyStatDto {
    private String date;
    private long count;
}
