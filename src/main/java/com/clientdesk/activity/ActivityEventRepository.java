package com.clientdesk.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {

    Page<ActivityEvent> findByWorkRequest_IdOrderByCreatedAtDesc(UUID workRequestId, Pageable pageable);
}
