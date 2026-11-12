package com.ems.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import com.ems.entity.Employee;

@Service
public class DashboardAnalyticsService {

    private final EmployeeService employeeService;

    public DashboardAnalyticsService(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // Filter employees based on joining date
    private List<Employee> getFilteredEmployees(
            LocalDate fromDate, LocalDate toDate) {

        List<Employee> employees = employeeService.getAllEmployees();

        // No date filter: return all employees
        if (fromDate == null && toDate == null) {
            return employees;
        }

        List<Employee> filteredEmployees = new ArrayList<>();

        for (Employee employee : employees) {

            LocalDate joiningDate = employee.getJoiningDate();

            if (joiningDate == null) {
                continue;
            }

            boolean afterFromDate =
                    fromDate == null || !joiningDate.isBefore(fromDate);

            boolean beforeToDate =
                    toDate == null || !joiningDate.isAfter(toDate);

            if (afterFromDate && beforeToDate) {
                filteredEmployees.add(employee);
            }
        }

        return filteredEmployees;
    }

    // 1. Department-wise employee count
    public Map<String, Long> getDepartmentDistribution() {
        return getDepartmentDistribution(null, null);
    }

    public Map<String, Long> getDepartmentDistribution(
            LocalDate fromDate, LocalDate toDate) {

        List<Employee> employees =
                getFilteredEmployees(fromDate, toDate);

        Map<String, Long> departmentData = new LinkedHashMap<>();

        for (Employee employee : employees) {

            String department = employee.getDepartment();

            if (department != null && !department.isBlank()) {
                departmentData.merge(department, 1L, Long::sum);
            }
        }

        return departmentData;
    }

    // 2. Monthly employee joining count
    public Map<String, Long> getMonthlyJoiningTrends() {
        return getMonthlyJoiningTrends(null, null);
    }

    public Map<String, Long> getMonthlyJoiningTrends(
            LocalDate fromDate, LocalDate toDate) {

        List<Employee> employees =
                getFilteredEmployees(fromDate, toDate);

        Map<String, Long> monthlyData = new TreeMap<>();

        for (Employee employee : employees) {

            if (employee.getJoiningDate() != null) {

                String monthKey = employee.getJoiningDate().getYear()
                        + "-"
                        + String.format("%02d",
                                employee.getJoiningDate().getMonthValue());

                monthlyData.merge(monthKey, 1L, Long::sum);
            }
        }

        return monthlyData;
    }

    // 3. Salary distribution
    public Map<String, Long> getSalaryDistribution() {
        return getSalaryDistribution(null, null);
    }

    public Map<String, Long> getSalaryDistribution(
            LocalDate fromDate, LocalDate toDate) {

        List<Employee> employees =
                getFilteredEmployees(fromDate, toDate);

        Map<String, Long> salaryData = new LinkedHashMap<>();

        salaryData.put("Below 25,000", 0L);
        salaryData.put("25,000 - 50,000", 0L);
        salaryData.put("50,000 - 75,000", 0L);
        salaryData.put("75,000 - 1,00,000", 0L);
        salaryData.put("Above 1,00,000", 0L);

        for (Employee employee : employees) {

            Double salary = employee.getSalary();

            if (salary == null) {
                continue;
            }

            if (salary < 25000) {
                salaryData.merge("Below 25,000", 1L, Long::sum);

            } else if (salary < 50000) {
                salaryData.merge("25,000 - 50,000", 1L, Long::sum);

            } else if (salary < 75000) {
                salaryData.merge("50,000 - 75,000", 1L, Long::sum);

            } else if (salary <= 100000) {
                salaryData.merge("75,000 - 1,00,000", 1L, Long::sum);

            } else {
                salaryData.merge("Above 1,00,000", 1L, Long::sum);
            }
        }

        return salaryData;
    }
}