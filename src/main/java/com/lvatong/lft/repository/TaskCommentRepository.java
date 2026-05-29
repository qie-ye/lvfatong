package com.lvatong.lft.repository;

import com.lvatong.lft.model.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    List<TaskComment> findByTaskIdOrderByCreatedAtDesc(Long taskId);

    List<TaskComment> findByTaskIdAndParentIdIsNullOrderByCreatedAtDesc(Long taskId);

    List<TaskComment> findByParentIdOrderByCreatedAtAsc(Long parentId);
}