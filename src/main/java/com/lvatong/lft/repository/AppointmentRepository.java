package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Appointment> findByLawyerIdOrderByCreatedAtDesc(Long lawyerId);
    List<Appointment> findByLawyerIdAndStatus(Long lawyerId, Appointment.AppointmentStatus status);
    boolean existsByLawyerIdAndAppointmentTime(Long lawyerId, java.time.LocalDateTime appointmentTime);
}
