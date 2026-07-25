package com.clientdesk.attachment;

import com.clientdesk.activity.ActivityEventService;
import com.clientdesk.api.ApiPage;
import com.clientdesk.identity.OrganizationRepository;
import com.clientdesk.security.AccessService;
import com.clientdesk.workrequest.WorkRequest;
import com.clientdesk.workrequest.WorkRequestRepository;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;

@Service
@Transactional
public class RequestAttachmentService {

    private final RequestAttachmentRepository requestAttachmentRepository;
    private final WorkRequestRepository workRequestRepository;
    private final OrganizationRepository organizationRepository;
    private final ActivityEventService activityEventService;
    private final AccessService accessService;
    private final AttachmentProperties properties;
    private final AttachmentFileValidator fileValidator;
    private final AttachmentStorage attachmentStorage;

    public RequestAttachmentService(
            RequestAttachmentRepository requestAttachmentRepository,
            WorkRequestRepository workRequestRepository,
            OrganizationRepository organizationRepository,
            ActivityEventService activityEventService,
            AccessService accessService,
            AttachmentProperties properties,
            AttachmentFileValidator fileValidator,
            AttachmentStorage attachmentStorage
    ) {
        this.requestAttachmentRepository = requestAttachmentRepository;
        this.workRequestRepository = workRequestRepository;
        this.organizationRepository = organizationRepository;
        this.activityEventService = activityEventService;
        this.accessService = accessService;
        this.properties = properties;
        this.fileValidator = fileValidator;
        this.attachmentStorage = attachmentStorage;
    }

    @Transactional(readOnly = true)
    public ApiPage<RequestAttachmentResponse> findAll(UUID workRequestId, int page, int size) {
        findWorkRequest(workRequestId);

        return ApiPage.from(
                requestAttachmentRepository.findByWorkRequest_IdOrderByCreatedAtDesc(
                        workRequestId,
                        PageRequest.of(page, size)
                ),
                RequestAttachmentResponse::from
        );
    }

    public RequestAttachmentResponse upload(UUID workRequestId, MultipartFile file) {
        WorkRequest workRequest = findWorkRequest(workRequestId);
        ValidatedAttachmentFile validatedFile = fileValidator.validate(file);
        UUID organizationId = workRequest.getClient().getOrganization().getId();
        organizationRepository.lockById(organizationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organization not found"));

        long attachmentCount = requestAttachmentRepository.countByWorkRequest_Id(workRequestId);
        if (attachmentCount >= properties.getMaxFilesPerWorkRequest()) {
            throw new ResponseStatusException(CONFLICT, "Work request attachment limit reached");
        }
        long organizationBytes = requestAttachmentRepository.sumSizeBytesByOrganizationId(organizationId);
        if (validatedFile.content().length > properties.getMaxBytesPerOrganization() - organizationBytes) {
            throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "Organization attachment storage limit reached");
        }

        String storedFileName = organizationId + "/" + workRequestId + "/"
                + UUID.randomUUID() + validatedFile.extension();
        try {
            attachmentStorage.store(storedFileName, validatedFile.content());
            deleteFileAfterRollback(storedFileName);
        } catch (IOException exception) {
            deleteQuietly(storedFileName);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "File could not be stored", exception);
        }

        RequestAttachment attachment = new RequestAttachment(
                workRequest,
                accessService.currentAppUser(),
                validatedFile.originalFileName(),
                storedFileName,
                validatedFile.contentType(),
                validatedFile.content().length,
                accessService.displayName()
        );

        RequestAttachment savedAttachment = requestAttachmentRepository.save(attachment);
        activityEventService.recordFileUploaded(savedAttachment);

        return RequestAttachmentResponse.from(savedAttachment);
    }

    @Transactional(readOnly = true)
    public RequestAttachmentDownload loadDownload(UUID id) {
        RequestAttachment attachment = findAttachment(id);

        try {
            Resource resource = attachmentStorage.load(attachment.getStoredFileName());
            return new RequestAttachmentDownload(attachment, resource);
        } catch (FileNotFoundException exception) {
            throw new ResponseStatusException(NOT_FOUND, "Attachment file not found");
        } catch (IOException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Attachment file could not be loaded", exception);
        }
    }

    public String contentDispositionValue(RequestAttachment attachment) {
        return ContentDisposition.attachment()
                .filename(attachment.getOriginalFileName(), StandardCharsets.UTF_8)
                .build()
                .toString();
    }

    public void deleteFilesForWorkRequestAfterCommit(UUID workRequestId) {
        deleteFilesAfterCommit(
                requestAttachmentRepository.findStoredFileNamesByWorkRequestId(workRequestId)
        );
    }

    public void deleteFilesForClientAfterCommit(UUID clientId) {
        deleteFilesAfterCommit(
                requestAttachmentRepository.findStoredFileNamesByClientId(clientId)
        );
    }

    private void deleteFileAfterRollback(String storedFileName) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteQuietly(storedFileName);
                }
            }
        });
    }

    private void deleteFilesAfterCommit(List<String> storedFileNames) {
        if (storedFileNames.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Attachment deletion requires an active transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storedFileNames.forEach(RequestAttachmentService.this::deleteQuietly);
            }
        });
    }

    private void deleteQuietly(String storedFileName) {
        try {
            attachmentStorage.delete(storedFileName);
        } catch (IOException ignored) {
            // The original storage or transaction failure remains the actionable error.
        }
    }

    private RequestAttachment findAttachment(UUID id) {
        RequestAttachment attachment = requestAttachmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Attachment not found"));
        accessService.requireClientAccess(attachment.getWorkRequest().getClient(), "Attachment");
        return attachment;
    }

    private WorkRequest findWorkRequest(UUID id) {
        WorkRequest workRequest = workRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Work request not found"));
        accessService.requireClientAccess(workRequest.getClient(), "Work request");
        return workRequest;
    }
}
