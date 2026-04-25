package com.lvatong.lft.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "昵称最长100字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最长100字符")
    private String email;

    @Size(max = 20, message = "手机号最长20字符")
    private String phone;
}
