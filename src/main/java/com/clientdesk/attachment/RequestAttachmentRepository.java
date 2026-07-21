package com.clientdesk.attachment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RequestAttachmentRepository extends JpaRepository<RequestAttachment, UUID> {

    Page<RequestAttachment> findByWorkRequest_IdOrderByCreatedAtDesc(UUID workRequestId, Pageable pageable);

    long countByWorkRequest_Id(UUID workRequestId);

    @Query("""
            select attachment.storedFileName
            from RequestAttachment attachment
            where attachment.workRequest.id = :workRequestId
            """)
    List<String> findStoredFileNamesByWorkRequestId(@Param("workRequestId") UUID workRequestId);

    @Query("""
            select attachment.storedFileName
            from RequestAttachment attachment
            where attachment.workRequest.client.id = :clientId
            """)
    List<String> findStoredFileNamesByClientId(@Param("clientId") UUID clientId);

    @Query("""
            select coalesce(sum(attachment.sizeBytes), 0)
            from RequestAttachment attachment
            where attachment.workRequest.client.organization.id = :organizationId
            """)
    long sumSizeBytesByOrganizationId(@Param("organizationId") UUID organizationId);
}
