package com.lvatong.lft.model.dto.dashboard;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class HotQueries {
    private List<Map<String, Object>> hotKeywords;
    private List<Map<String, Object>> hotDomains;
}