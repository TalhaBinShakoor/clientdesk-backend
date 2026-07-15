package com.clientdesk.attachment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RequestAttachmentRepository extends JpaRepository<RequestAttachment, UUID> {

    List<RequestAttachment> findByWorkRequest_IdOrderByCreatedAtDesc(UUID workRequestId);
}
