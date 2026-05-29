package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByTeamIdAndStatus(Long teamId, Task.TaskStatus status);

    List<Task> findByTeamIdOrderByCreatedAtDesc(Long teamId);

    List<Task> findByAssigneeIdAndStatus(Long assigneeId, Task.TaskStatus status);

    List<Task> findByCaseId(Long caseId);

    @Query("SELECT t FROM Task t WHERE t.teamId = :teamId AND t.status <> 'DONE' ORDER BY " +
           "CASE t.priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, " +
           "t.dueDate ASC")
    List<Task> findActiveTasksByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.teamId = :teamId AND t.status = :status")
    long countByTeamIdAndStatus(@Param("teamId") Long teamId, @Param("status") Task.TaskStatus status);
}