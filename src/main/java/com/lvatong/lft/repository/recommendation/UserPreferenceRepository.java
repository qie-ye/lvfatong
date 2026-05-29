package com.lvatong.lft.repository.recommendation;

import com.lvatong.lft.model.entity.recommendation.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    Optional<UserPreference> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
