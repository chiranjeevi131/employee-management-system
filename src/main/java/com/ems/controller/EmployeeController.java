package com.ems.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.ems.entity.Employee;
import com.ems.service.EmployeeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {


private final EmployeeService employeeService;

public EmployeeController(EmployeeService employeeService) {
    this.employeeService = employeeService;
}

// Add employee - ADMIN and HR
@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
@PostMapping
public Employee addEmployee(@Valid @RequestBody Employee employee) {
    return employeeService.saveEmployee(employee);
}

// Get all employees - ADMIN and HR
@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
@GetMapping
public List<Employee> getAllEmployees() {
    return employeeService.getAllEmployees();
}

// Get employee by ID - ADMIN and HR
@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
@GetMapping("/{id}")
public Employee getEmployeeById(@PathVariable Long id) {
    return employeeService.getEmployeeById(id);
}

// Update employee - ADMIN and HR
@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
@PutMapping("/{id}")
public Employee updateEmployee(
        @PathVariable Long id,
        @Valid @RequestBody Employee employee) {

    return employeeService.updateEmployee(id, employee);
}

// Delete employee - ADMIN only
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public String deleteEmployee(@PathVariable Long id) {

    employeeService.deleteEmployee(id);

    return "Employee deleted successfully!";
}


}
