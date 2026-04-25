package com.lvatong.lft.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GenerateDocumentRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 300)
    private String title;

    @NotBlank(message = "文书类型不能为空")
    private String docType; // COMPLAINT, DEFENSE, ARBITRATION, PETITION, INDICTMENT, OTHER

    private String domain;

    @NotBlank(message = "案件事实不能为空")
    @Size(max = 10000)
    private String facts;

    @Size(max = 5000)
    private String claims;
}
