package com.ems.controller;

import java.time.LocalDate;

import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import com.ems.entity.Attendance;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ems.entity.AttendanceStatus;
import com.ems.repository.EmployeeRepository;
import com.ems.service.AttendanceService;

@Controller
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeRepository employeeRepository;

    public AttendanceController(
            AttendanceService attendanceService,
            EmployeeRepository employeeRepository) {

        this.attendanceService = attendanceService;
        this.employeeRepository = employeeRepository;
    }

 // Display Attendance Page with Filters

    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String viewAttendance(

            @RequestParam(required = false) Long employeeId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            Model model) {

        List<Attendance> records = attendanceService.getAllAttendance();

        // Filter by Employee
        if (employeeId != null) {

            records = records.stream()
                    .filter(a -> a.getEmployee().getId().equals(employeeId))
                    .collect(Collectors.toList());
        }

        // Filter by Start Date
        if (startDate != null) {

            records = records.stream()
                    .filter(a -> !a.getAttendanceDate().isBefore(startDate))
                    .collect(Collectors.toList());
        }

        // Filter by End Date
        if (endDate != null) {

            records = records.stream()
                    .filter(a -> !a.getAttendanceDate().isAfter(endDate))
                    .collect(Collectors.toList());
        }

        model.addAttribute("employees", employeeRepository.findAll());

        model.addAttribute("attendanceRecords", records);

        model.addAttribute("attendanceStatuses", AttendanceStatus.values());

        model.addAttribute("today", LocalDate.now());

        model.addAttribute("selectedEmployeeId", employeeId);

        model.addAttribute("startDate", startDate);

        model.addAttribute("endDate", endDate);

        return "attendance";
    }
    
    @GetMapping("/attendance/reports")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String viewAttendanceReports(

            @RequestParam(required = false) Long employeeId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            Model model) {

        List<Attendance> records = attendanceService.getAllAttendance();

        // Filter by Employee
        if (employeeId != null) {
            records = records.stream()
                    .filter(a -> a.getEmployee().getId().equals(employeeId))
                    .collect(Collectors.toList());
        }

        // Filter by Start Date
        if (startDate != null) {
            records = records.stream()
                    .filter(a -> !a.getAttendanceDate().isBefore(startDate))
                    .collect(Collectors.toList());
        }

        // Filter by End Date
        if (endDate != null) {
            records = records.stream()
                    .filter(a -> !a.getAttendanceDate().isAfter(endDate))
                    .collect(Collectors.toList());
        }

        // Attendance Summary
        long presentCount = records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT)
                .count();

        long absentCount = records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                .count();

        long halfDayCount = records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.HALF_DAY)
                .count();

        long leaveCount = records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.LEAVE)
                .count();
        long workingDays = records.size() - leaveCount;

        double attendancePercentage = workingDays == 0
                ? 0.0
                : ((presentCount + (halfDayCount * 0.5)) / workingDays) * 100;

        model.addAttribute("employees", employeeRepository.findAll());
        model.addAttribute("attendanceRecords", records);

        model.addAttribute("totalRecords", records.size());
        model.addAttribute("presentCount", presentCount);
        model.addAttribute("absentCount", absentCount);
        model.addAttribute("halfDayCount", halfDayCount);
        model.addAttribute("leaveCount", leaveCount);
        model.addAttribute("attendancePercentage", attendancePercentage);

        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "attendance-reports";
    }

    @GetMapping("/attendance/reports/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public void exportAttendanceCSV(

            @RequestParam(required = false) Long employeeId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            HttpServletResponse response) throws IOException {

        List<Attendance> records = attendanceService.getAllAttendance();

        if (employeeId != null) {
            records = records.stream()
                    .filter(a -> a.getEmployee().getId().equals(employeeId))
                    .collect(Collectors.toList());
        }

        if (startDate != null) {
            records = records.stream()
                    .filter(a -> !a.getAttendanceDate().isBefore(startDate))
                    .collect(Collectors.toList());
        }

        if (endDate != null) {
            records = records.stream()
                    .filter(a -> !a.getAttendanceDate().isAfter(endDate))
                    .collect(Collectors.toList());
        }

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        response.setHeader(
                "Content-Disposition",
                "attachment; filename=attendance-report.csv"
        );

        var writer = response.getWriter();

        // UTF-8 BOM for Excel compatibility
        writer.write('\uFEFF');

        writer.println("Employee ID,Employee Name,Date,Status,Check In,Check Out");

        for (Attendance record : records) {

            String employeeName =
                    record.getEmployee().getFirstName() + " "
                    + record.getEmployee().getLastName();

            writer.println(
                    record.getEmployee().getId() + ","
                    + csvEscape(employeeName) + ","
                    + "\"" + record.getAttendanceDate().format(
                            java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")
                      ) + "\","
                    + record.getStatus() + ","
                    + (record.getCheckIn() != null ? record.getCheckIn() : "-") + ","
                    + (record.getCheckOut() != null ? record.getCheckOut() : "-")
            );
        }

        writer.flush();
    }

    private String csvEscape(String value) {

        if (value == null) {
            return "";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    
    // Mark Employee Attendance
    @PostMapping("/attendance/mark")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String markAttendance(
            @RequestParam Long employeeId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate attendanceDate,
            @RequestParam AttendanceStatus status,
            @RequestParam(required = false) String remarks,
            RedirectAttributes redirectAttributes) {

        try {

            attendanceService.markAttendance(
                    employeeId, attendanceDate, status, remarks);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Attendance marked successfully!");

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/attendance";
    }

    // Employee Check-In
    @PostMapping("/attendance/check-in/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String checkIn(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            attendanceService.checkIn(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Check-in recorded successfully!");

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/attendance";
    }

    // Employee Check-Out
    @PostMapping("/attendance/check-out/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public String checkOut(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            attendanceService.checkOut(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Check-out recorded successfully!");

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/attendance";
    }
}