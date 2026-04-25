package com.lvatong.lft.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GenerateOpinionRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 300)
    private String title;

    private String domain;

    @NotBlank(message = "问题描述不能为空")
    @Size(max = 5000)
    private String question;

    @Size(max = 10000)
    private String facts;
}
