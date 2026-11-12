package com.ems.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ems.entity.AuditLog;
import com.ems.repository.AuditLogRepository;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logActivity(String action, String entityType,
                            Long entityId, String details) {

        AuditLog auditLog = new AuditLog();

        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDetails(details);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String username = "SYSTEM";

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            username = authentication.getName();
        }

        auditLog.setPerformedBy(username);

        auditLogRepository.save(auditLog);
    }
 // Get all audit logs
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }
 // Get the latest 5 audit activities
    public List<AuditLog> getRecentActivities() {

        return auditLogRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .limit(5)
                .toList();
    }
}