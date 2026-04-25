package com.lvatong.lft.model.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateAppointmentRequest {

    @NotNull(message = "律师ID不能为空")
    private Long lawyerId;

    @NotNull(message = "预约时间不能为空")
    @Future(message = "预约时间必须在未来")
    private LocalDateTime appointmentTime;

    private String consultationType;

    private String description;
}
