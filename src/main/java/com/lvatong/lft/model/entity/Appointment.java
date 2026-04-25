package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_lawyer_id", columnList = "lawyerId"),
        @Index(name = "idx_status", columnList = "status")
})
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "lawyer_id", nullable = false)
    private Long lawyerId;

    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    @Column(length = 20)
    private String consultationType;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String cancelReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = AppointmentStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AppointmentStatus {
        PENDING, CONFIRMED, CANCELLED, COMPLETED
    }
}
