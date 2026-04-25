package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.LawyerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LawyerProfileRepository extends JpaRepository<LawyerProfile, Long> {
    Optional<LawyerProfile> findByUserId(Long userId);
    Page<LawyerProfile> findByVerifiedTrueAndAvailableTrue(Pageable pageable);
    List<LawyerProfile> findByProvinceAndCity(String province, String city);

    @Query("SELECT lp FROM LawyerProfile lp WHERE lp.verified = true AND lp.available = true AND " +
            "(lp.specialties LIKE %:keyword% OR lp.tags LIKE %:keyword% OR lp.realName LIKE %:keyword%)")
    Page<LawyerProfile> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT lp FROM LawyerProfile lp WHERE lp.verified = true AND lp.available = true ORDER BY lp.rating DESC")
    Page<LawyerProfile> findAllOrderByRating(Pageable pageable);

    @Query("SELECT lp.id, lp.realName, COALESCE(lp.consultationCount, 0) FROM LawyerProfile lp " +
           "ORDER BY COALESCE(lp.consultationCount, 0) DESC")
    List<Object[]> findTop10ByConsultationCount(Pageable pageable);

    @Query("SELECT lp FROM LawyerProfile lp WHERE lp.verified = true AND lp.available = true AND " +
            "lp.specialties LIKE %:specialty% ORDER BY lp.rating DESC")
    Page<LawyerProfile> findBySpecialtyOrderByRating(@Param("specialty") String specialty, Pageable pageable);
}
