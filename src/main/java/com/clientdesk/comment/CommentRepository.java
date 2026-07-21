package com.clientdesk.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByWorkRequest_IdOrderByCreatedAtAsc(UUID workRequestId);

    Page<Comment> findByWorkRequest_IdOrderByCreatedAtAsc(UUID workRequestId, Pageable pageable);

    Page<Comment> findByWorkRequest_IdOrderByCreatedAtDesc(UUID workRequestId, Pageable pageable);

    List<Comment> findByProjectTask_IdOrderByCreatedAtAsc(UUID projectTaskId);

    Page<Comment> findByProjectTask_IdOrderByCreatedAtAsc(UUID projectTaskId, Pageable pageable);
}
