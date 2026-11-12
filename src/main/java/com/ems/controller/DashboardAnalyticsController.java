package com.ems.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ems.service.DashboardAnalyticsService;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
public class DashboardAnalyticsController {

    private final DashboardAnalyticsService analyticsService;

    public DashboardAnalyticsController(
            DashboardAnalyticsService analyticsService) {

        this.analyticsService = analyticsService;
    }

    // Validate selected date range
    private void validateDates(LocalDate fromDate, LocalDate toDate) {

        if (fromDate != null && toDate != null
                && fromDate.isAfter(toDate)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "From date cannot be after To date"
            );
        }
    }

    // Department-wise employee distribution
    @GetMapping("/departments")
    public Map<String, Long> getDepartmentDistribution(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {

        validateDates(fromDate, toDate);

        return analyticsService.getDepartmentDistribution(
                fromDate, toDate);
    }

    // Monthly employee joining trends
    @GetMapping("/monthly-joining")
    public Map<String, Long> getMonthlyJoiningTrends(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {

        validateDates(fromDate, toDate);

        return analyticsService.getMonthlyJoiningTrends(
                fromDate, toDate);
    }

    // Employee salary distribution
    @GetMapping("/salaries")
    public Map<String, Long> getSalaryDistribution(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {

        validateDates(fromDate, toDate);

        return analyticsService.getSalaryDistribution(
                fromDate, toDate);
    }
}