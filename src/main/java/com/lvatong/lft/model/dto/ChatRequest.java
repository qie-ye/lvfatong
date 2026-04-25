package com.lvatong.lft.model.dto;

import lombok.Data;

@Data
public class ChatRequest {

    private Long sessionId;
    private String question;
    private String docType;
    private String lawDomain;
}
