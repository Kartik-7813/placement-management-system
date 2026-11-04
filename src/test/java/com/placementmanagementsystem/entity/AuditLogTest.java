package com.placementmanagementsystem.entity;

import com.placementmanagementsystem.enums.AuditAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditLog Entity Unit Tests")
class AuditLogTest {

    @Test
    @DisplayName("PrePersist onCreate should populate createdAt when null")
    void prePersist_ShouldPopulateCreatedAt_WhenNull() {
        AuditLog auditLog = new AuditLog(1L, AuditAction.JOB_CREATED, "JOB", 10L, "Job created");
        assertNull(auditLog.getCreatedAt());

        auditLog.onCreate();

        assertNotNull(auditLog.getCreatedAt());
        assertTrue(auditLog.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    @DisplayName("PrePersist onCreate should preserve existing createdAt if already set")
    void prePersist_ShouldPreserveExistingCreatedAt() {
        LocalDateTime past = LocalDateTime.of(2025, 1, 1, 10, 0);
        AuditLog auditLog = new AuditLog();
        auditLog.setCreatedAt(past);

        auditLog.onCreate();

        assertEquals(past, auditLog.getCreatedAt());
    }

    @Test
    @DisplayName("AuditLog equality is based on auditId")
    void auditLog_Equality_ShouldBeBasedOnAuditId() {
        AuditLog log1 = new AuditLog();
        log1.setAuditId(100L);

        AuditLog log2 = new AuditLog();
        log2.setAuditId(100L);

        AuditLog log3 = new AuditLog();
        log3.setAuditId(200L);

        assertEquals(log1, log2);
        assertNotEquals(log1, log3);
        assertEquals(log1.hashCode(), log2.hashCode());
    }

    @Test
    @DisplayName("AuditLog getters, setters and toString work correctly")
    void auditLog_GettersAndSetters() {
        AuditLog log = new AuditLog(2L, AuditAction.APPLICATION_SUBMITTED, "APPLICATION", 50L, "App submitted");
        log.setAuditId(1L);

        assertEquals(1L, log.getAuditId());
        assertEquals(2L, log.getActorId());
        assertEquals(AuditAction.APPLICATION_SUBMITTED, log.getAction());
        assertEquals("APPLICATION", log.getEntityType());
        assertEquals(50L, log.getEntityId());
        assertEquals("App submitted", log.getDescription());
        assertTrue(log.toString().contains("auditId=1"));
    }
}

