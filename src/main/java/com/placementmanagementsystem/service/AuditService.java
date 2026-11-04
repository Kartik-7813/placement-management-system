package com.placementmanagementsystem.service;

import com.placementmanagementsystem.entity.AuditLog;
import com.placementmanagementsystem.enums.AuditAction;
import com.placementmanagementsystem.repository.AuditLogRepository;
import com.placementmanagementsystem.security.SecurityHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for recording business audit trail events.
 * Executes within the caller's active database transaction (Propagation.REQUIRED),
 * ensuring audit records are atomically persisted with the business operation
 * and automatically rolled back if the operation fails.
 */
@Service
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityHelper securityHelper;

    public AuditService(AuditLogRepository auditLogRepository, SecurityHelper securityHelper) {
        this.auditLogRepository = auditLogRepository;
        this.securityHelper = securityHelper;
    }

    /**
     * Records a business audit event, automatically resolving the actor ID
     * from the active SecurityContext. If unauthenticated or system-triggered, actor ID is set to null.
     *
     * @param action      the categorized business action
     * @param entityType  the name of the affected business entity type (e.g., "JOB", "APPLICATION")
     * @param entityId    the primary key of the affected entity
     * @param description a concise, safe summary of the event outcome
     * @return the saved AuditLog record
     */
    public AuditLog recordAudit(AuditAction action, String entityType, Long entityId, String description) {
        Long actorId = securityHelper.getCurrentUserId().orElse(null);
        return recordAudit(actorId, action, entityType, entityId, description);
    }

    /**
     * Records a business audit event with an explicitly specified actor ID.
     * Used for operations such as self-registration where the user account was just created.
     *
     * @param actorId     the internal user ID of the actor (may be null for system actions)
     * @param action      the categorized business action
     * @param entityType  the name of the affected business entity type
     * @param entityId    the primary key of the affected entity
     * @param description a concise, safe summary of the event outcome
     * @return the saved AuditLog record
     */
    public AuditLog recordAudit(Long actorId, AuditAction action, String entityType, Long entityId, String description) {
        AuditLog auditLog = new AuditLog(actorId, action, entityType, entityId, description);
        return auditLogRepository.save(auditLog);
    }

    /**
     * Internal retrieval of audit logs for an entity in reverse chronological order.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsForEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    /**
     * Internal retrieval of audit logs for an actor in reverse chronological order.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsForActor(Long actorId) {
        return auditLogRepository.findByActorIdOrderByCreatedAtDesc(actorId);
    }
}

