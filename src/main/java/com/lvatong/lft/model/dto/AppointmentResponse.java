package com.lvatong.lft.model.dto;

import com.lvatong.lft.model.entity.Appointment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AppointmentResponse {
    private Long id;
    private Long userId;
    private Long lawyerId;
    private String lawyerName;
    private String status;
    private LocalDateTime appointmentTime;
    private String consultationType;
    private String description;
    private String cancelReason;
    private LocalDateTime createdAt;

    public static AppointmentResponse from(Appointment apt, String lawyerName) {
        return AppointmentResponse.builder()
                .id(apt.getId())
                .userId(apt.getUserId())
                .lawyerId(apt.getLawyerId())
                .lawyerName(lawyerName)
                .status(apt.getStatus().name())
                .appointmentTime(apt.getAppointmentTime())
                .consultationType(apt.getConsultationType())
                .description(apt.getDescription())
                .cancelReason(apt.getCancelReason())
                .createdAt(apt.getCreatedAt())
                .build();
    }
}
