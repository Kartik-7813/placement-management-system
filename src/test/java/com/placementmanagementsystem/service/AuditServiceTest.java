package com.placementmanagementsystem.service;

import com.placementmanagementsystem.entity.AuditLog;
import com.placementmanagementsystem.enums.AuditAction;
import com.placementmanagementsystem.repository.AuditLogRepository;
import com.placementmanagementsystem.security.SecurityHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Unit Tests")
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private AuditService auditService;

    @Test
    @DisplayName("recordAudit with authenticated user extracts actor ID from SecurityHelper")
    void recordAudit_WithAuthenticatedUser_ExtractsActorIdAndPersists() {
        when(securityHelper.getCurrentUserId()).thenReturn(Optional.of(42L));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog saved = auditService.recordAudit(
                AuditAction.JOB_CREATED,
                "JOB",
                101L,
                "Campus recruitment job created"
        );

        assertNotNull(saved);
        assertEquals(42L, saved.getActorId());
        assertEquals(AuditAction.JOB_CREATED, saved.getAction());
        assertEquals("JOB", saved.getEntityType());
        assertEquals(101L, saved.getEntityId());
        assertEquals("Campus recruitment job created", saved.getDescription());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(42L, captor.getValue().getActorId());
    }

    @Test
    @DisplayName("recordAudit with explicit actor ID stores provided actor ID directly")
    void recordAudit_WithExplicitActorId_StoresProvidedActorId() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog saved = auditService.recordAudit(
                99L,
                AuditAction.STUDENT_REGISTERED,
                "STUDENT",
                5L,
                "Student self-registration completed"
        );

        assertNotNull(saved);
        assertEquals(99L, saved.getActorId());
        assertEquals(AuditAction.STUDENT_REGISTERED, saved.getAction());
        assertEquals("STUDENT", saved.getEntityType());
        assertEquals(5L, saved.getEntityId());
    }

    @Test
    @DisplayName("recordAudit when unauthenticated or system-triggered sets actor ID to null")
    void recordAudit_UnauthenticatedOrSystem_SetsActorIdToNull() {
        when(securityHelper.getCurrentUserId()).thenReturn(Optional.empty());
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog saved = auditService.recordAudit(
                AuditAction.JOB_CLOSED,
                "JOB",
                101L,
                "Job posting closed for applications"
        );

        assertNotNull(saved);
        assertNull(saved.getActorId(), "Actor ID should be null for unauthenticated/system actions");
        assertEquals(AuditAction.JOB_CLOSED, saved.getAction());
        assertEquals("JOB", saved.getEntityType());
        assertEquals(101L, saved.getEntityId());
    }

    @Test
    @DisplayName("getAuditLogsForEntity delegates to repository with correct parameters")
    void getAuditLogsForEntity_DelegatesToRepository() {
        when(auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("APPLICATION", 10L))
                .thenReturn(Collections.emptyList());

        List<AuditLog> results = auditService.getAuditLogsForEntity("APPLICATION", 10L);

        assertNotNull(results);
        verify(auditLogRepository).findByEntityTypeAndEntityIdOrderByCreatedAtDesc("APPLICATION", 10L);
    }

    @Test
    @DisplayName("getAuditLogsForActor delegates to repository with correct actor ID")
    void getAuditLogsForActor_DelegatesToRepository() {
        when(auditLogRepository.findByActorIdOrderByCreatedAtDesc(42L))
                .thenReturn(Collections.emptyList());

        List<AuditLog> results = auditService.getAuditLogsForActor(42L);

        assertNotNull(results);
        verify(auditLogRepository).findByActorIdOrderByCreatedAtDesc(42L);
    }
}

