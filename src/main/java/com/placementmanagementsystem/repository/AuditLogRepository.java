package com.placementmanagementsystem.repository;

import com.placementmanagementsystem.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for persisting and querying business audit trail records.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Retrieves audit trail records for a specific business entity in reverse chronological order.
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    /**
     * Retrieves audit trail records performed by a specific actor in reverse chronological order.
     */
    List<AuditLog> findByActorIdOrderByCreatedAtDesc(Long actorId);

    /**
     * Supports paginated retrieval of audit logs for an entity.
     */
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    /**
     * Supports paginated retrieval of all audit logs ordered by creation time descending.
     */
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

