package com.ems.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ems.entity.Attendance;
import com.ems.entity.AttendanceStatus;
import com.ems.entity.Employee;
import com.ems.repository.AttendanceRepository;
import com.ems.repository.EmployeeRepository;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository) {

        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    // Mark employee attendance
    public Attendance markAttendance(
            Long employeeId,
            LocalDate attendanceDate,
            AttendanceStatus status,
            String remarks) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found"));

        boolean alreadyMarked = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(
                        employeeId, attendanceDate)
                .isPresent();

        if (alreadyMarked) {
            throw new RuntimeException(
                    "Attendance already marked for this employee on this date");
        }

        Attendance attendance = new Attendance();

        attendance.setEmployee(employee);
        attendance.setAttendanceDate(attendanceDate);
        attendance.setStatus(status);
        attendance.setRemarks(remarks);

        return attendanceRepository.save(attendance);
    }

    // Get all attendance records
    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }

    // Get attendance by date
    public List<Attendance> getAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByAttendanceDate(date);
    }

    // Get employee attendance history
    public List<Attendance> getEmployeeAttendance(Long employeeId) {
        return attendanceRepository.findByEmployeeId(employeeId);
    }

    // Get attendance between dates
    public List<Attendance> getAttendanceBetweenDates(
            LocalDate startDate,
            LocalDate endDate) {

        return attendanceRepository.findByAttendanceDateBetween(
                startDate, endDate);
    }

    // Employee check-in
    public Attendance checkIn(Long attendanceId) {

        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() ->
                        new RuntimeException("Attendance record not found"));

        if (attendance.getCheckIn() != null) {
            throw new RuntimeException("Employee has already checked in");
        }

        attendance.setCheckIn(LocalTime.now());

        return attendanceRepository.save(attendance);
    }

    // Employee check-out
    public Attendance checkOut(Long attendanceId) {

        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() ->
                        new RuntimeException("Attendance record not found"));

        if (attendance.getCheckIn() == null) {
            throw new RuntimeException(
                    "Employee must check in before checking out");
        }

        if (attendance.getCheckOut() != null) {
            throw new RuntimeException("Employee has already checked out");
        }

        attendance.setCheckOut(LocalTime.now());

        return attendanceRepository.save(attendance);
    }
}