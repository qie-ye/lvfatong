package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.MessageReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, Long> {

    Optional<MessageReadStatus> findByMessageIdAndUserId(Long messageId, Long userId);

    boolean existsByMessageIdAndUserId(Long messageId, Long userId);
}