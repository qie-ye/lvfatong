package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.UserMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMemoryRepository extends JpaRepository<UserMemory, Long> {

    List<UserMemory> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<UserMemory> findByUserIdAndMemoryType(Long userId, UserMemory.MemoryType memoryType);

    Optional<UserMemory> findByUserIdAndMemoryTypeAndKey(Long userId, UserMemory.MemoryType memoryType, String key);

    long countByUserId(Long userId);

    void deleteByIdAndUserId(Long id, Long userId);

    List<UserMemory> findByUserIdAndMemoryTypeIn(Long userId, List<UserMemory.MemoryType> types);
}
