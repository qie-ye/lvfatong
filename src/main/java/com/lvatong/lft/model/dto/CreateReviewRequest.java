package com.lvatong.lft.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReviewRequest {
    @NotNull(message = "律师ID不能为空")
    private Long lawyerId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低1分")
    @Max(value = 5, message = "评分最高5分")
    private Integer rating;

    @Size(max = 1000)
    private String comment;

    private String serviceType; // ONLINE, OFFLINE, PHONE
}
