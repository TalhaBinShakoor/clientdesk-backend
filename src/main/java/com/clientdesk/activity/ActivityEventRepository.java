package com.clientdesk.activity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {

    List<ActivityEvent> findByWorkRequest_IdOrderByCreatedAtDesc(UUID workRequestId);
}
