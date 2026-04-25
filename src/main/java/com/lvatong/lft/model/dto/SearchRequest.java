package com.lvatong.lft.model.dto;

import lombok.Data;

@Data
public class SearchRequest {

    private String query;
    private String docType;
    private String lawDomain;
    private Integer topK = 5;
}
