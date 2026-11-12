package com.ems.controller;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ems.entity.AuditLog;

import com.ems.service.AuditLogService;

@Controller
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String viewAuditLogs(Model model) {

        model.addAttribute(
                "auditLogs",
                auditLogService.getAllAuditLogs()
        );

        return "audit-logs";
    }
 // API to fetch the latest 5 employee activities
    @GetMapping("/api/audit-logs/recent")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public List<AuditLog> getRecentActivities() {

        return auditLogService.getRecentActivities();
    }
}