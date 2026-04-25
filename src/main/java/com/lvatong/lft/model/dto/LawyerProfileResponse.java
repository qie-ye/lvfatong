package com.lvatong.lft.model.dto;

import com.lvatong.lft.model.entity.LawyerProfile;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LawyerProfileResponse {
    private Long id;
    private Long userId;
    private String realName;
    private String lawFirm;
    private String licenseNo;
    private String bio;
    private String education;
    private List<String> specialties;
    private List<String> tags;
    private String province;
    private String city;
    private Integer yearsOfExperience;
    private Double rating;
    private Integer consultationCount;
    private Boolean verified;
    private Boolean available;
    private String consultationType;

    public static LawyerProfileResponse from(LawyerProfile lp) {
        return LawyerProfileResponse.builder()
                .id(lp.getId())
                .userId(lp.getUserId())
                .realName(lp.getRealName())
                .lawFirm(lp.getLawFirm())
                .licenseNo(lp.getLicenseNo())
                .bio(lp.getBio())
                .education(lp.getEducation())
                .specialties(lp.getSpecialtyList())
                .tags(lp.getTagList())
                .province(lp.getProvince())
                .city(lp.getCity())
                .yearsOfExperience(lp.getYearsOfExperience())
                .rating(lp.getRating())
                .consultationCount(lp.getConsultationCount())
                .verified(lp.getVerified())
                .available(lp.getAvailable())
                .consultationType(lp.getConsultationType() != null ? lp.getConsultationType().name() : null)
                .build();
    }
}
