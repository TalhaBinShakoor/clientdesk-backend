package com.clientdesk.workrequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface WorkRequestRepository extends JpaRepository<WorkRequest, UUID>, JpaSpecificationExecutor<WorkRequest> {
}
