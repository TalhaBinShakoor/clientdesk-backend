package com.clientdesk.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByWorkRequest_IdOrderByCreatedAtAsc(UUID workRequestId);

    List<Comment> findByProjectTask_IdOrderByCreatedAtAsc(UUID projectTaskId);
}
