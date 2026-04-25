package com.lvatong.lft.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "lawyer_profiles")
public class LawyerProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(length = 100)
    private String realName;

    @Column(length = 50)
    private String lawFirm;

    @Column(length = 50)
    private String licenseNo;

    @Column(length = 2000)
    private String bio;

    @Column(length = 500)
    private String education;

    @Column(length = 1000)
    private String specialties;

    @Column(length = 500)
    private String tags;

    @Column(length = 20)
    private String province;

    @Column(length = 20)
    private String city;

    @Column
    private Integer yearsOfExperience;

    @Column
    private Double rating;

    @Column
    private Integer consultationCount;

    @Column
    private Boolean verified;

    @Column
    private Boolean available;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ConsultationType consultationType;

    public enum ConsultationType {
        ONLINE, OFFLINE, BOTH
    }

    @Transient
    public List<String> getSpecialtyList() {
        if (specialties == null || specialties.isBlank()) return List.of();
        return List.of(specialties.split(","));
    }

    @Transient
    public List<String> getTagList() {
        if (tags == null || tags.isBlank()) return List.of();
        return List.of(tags.split(","));
    }
}
