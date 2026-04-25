package com.lvatong.lft.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateLawyerProfileRequest {

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 100)
    private String realName;

    @Size(max = 50)
    private String lawFirm;

    @Size(max = 50)
    private String licenseNo;

    @Size(max = 2000)
    private String bio;

    @Size(max = 500)
    private String education;

    @Size(max = 1000)
    private String specialties;

    @Size(max = 500)
    private String tags;

    @Size(max = 20)
    private String province;

    @Size(max = 20)
    private String city;

    private Integer yearsOfExperience;

    private String consultationType;
}
