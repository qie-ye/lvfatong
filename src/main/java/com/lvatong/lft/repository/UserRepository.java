package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByPhone(String phone);
    boolean existsByUsername(String username);

    @Query("SELECT CAST(u.createdAt AS date) AS day, COUNT(u) AS cnt " +
           "FROM User u WHERE u.createdAt >= :since " +
           "GROUP BY CAST(u.createdAt AS date) ORDER BY CAST(u.createdAt AS date)")
    List<Object[]> countRegistrationByDay(@Param("since") LocalDateTime since);
}
