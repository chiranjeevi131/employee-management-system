package com.ems.service;

import com.ems.service.AuditLogService;
import java.util.List;
import java.util.ArrayList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.ems.entity.Employee;
import com.ems.repository.EmployeeRepository;

@Service
public class EmployeeService {

	private final EmployeeRepository employeeRepository;
	private final AuditLogService auditLogService;

	public EmployeeService(EmployeeRepository employeeRepository,
            AuditLogService auditLogService) {
this.employeeRepository = employeeRepository;
this.auditLogService = auditLogService;
}

	// Add employee
	public Employee saveEmployee(Employee employee) {

	    Employee savedEmployee = employeeRepository.save(employee);

	    auditLogService.logActivity(
	            "CREATE",
	            "Employee",
	            savedEmployee.getId(),
	            "Created employee: " + savedEmployee.getFirstName()
	                    + " " + savedEmployee.getLastName()
	    );

	    return savedEmployee;
	}

    // Get all employees
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    // Get employee by ID
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() ->
                    new RuntimeException("Employee not found with ID: " + id));
    }

    // Search employees
    public List<Employee> searchEmployees(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllEmployees();
        }

        return employeeRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrDepartmentContainingIgnoreCase(
                        keyword, keyword, keyword);
    }

    // Pagination and Search
    public Page<Employee> getEmployeesWithPagination(
            String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        if (keyword == null || keyword.isBlank()) {
            return employeeRepository.findAll(pageable);
        }

        return employeeRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrDepartmentContainingIgnoreCase(
                        keyword, keyword, keyword, pageable);
    }

    // Update employee
    public Employee updateEmployee(Long id, Employee employee) {
        Employee existingEmployee = getEmployeeById(id);

        existingEmployee.setFirstName(employee.getFirstName());
        existingEmployee.setLastName(employee.getLastName());
        existingEmployee.setEmail(employee.getEmail());
        existingEmployee.setPhone(employee.getPhone());
        existingEmployee.setDepartment(employee.getDepartment());

        if (employee.getDepartmentEntity() != null) {
            existingEmployee.setDepartmentEntity(employee.getDepartmentEntity());
        }

        existingEmployee.setSalary(employee.getSalary());

        if (employee.getPhoto() != null && !employee.getPhoto().isBlank()) {
            existingEmployee.setPhoto(employee.getPhoto());
        }

        if (employee.getJoiningDate() != null) {
            existingEmployee.setJoiningDate(employee.getJoiningDate());
        }

        Employee updatedEmployee = employeeRepository.save(existingEmployee);

        auditLogService.logActivity(
                "UPDATE",
                "Employee",
                updatedEmployee.getId(),
                "Updated employee: " + updatedEmployee.getFirstName()
                        + " " + updatedEmployee.getLastName()
        );

        return updatedEmployee;
    }

 // Delete employee
    public void deleteEmployee(Long id) {

        Employee employee = getEmployeeById(id);

        auditLogService.logActivity(
                "DELETE",
                "Employee",
                employee.getId(),
                "Deleted employee: " + employee.getFirstName()
                        + " " + employee.getLastName()
        );

        employeeRepository.deleteById(id);
    }

    // Department-wise filtering, search, pagination and sorting
    public Page<Employee> filterEmployees(
            Long departmentId, String keyword,
            int page, int size, String sortBy) {

        String normalizedKeyword = normalizeKeyword(keyword);
        Pageable pageable = PageRequest.of(page, size, getEmployeeSort(sortBy));

        return employeeRepository.filterEmployees(
                departmentId, normalizedKeyword, pageable);
    }

    // Fetch every matching employee for Excel export (no pagination)
    public List<Employee> getEmployeesForExport(
            Long departmentId, String keyword, String sortBy) {

        String normalizedKeyword = normalizeKeyword(keyword);

        return employeeRepository.filterEmployeesForExport(
                departmentId, normalizedKeyword, getEmployeeSort(sortBy));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    // Whitelist supported sort fields to avoid accepting arbitrary property names
    private Sort getEmployeeSort(String sortBy) {
        if (sortBy == null) {
            sortBy = "id";
        }

        switch (sortBy) {
            case "firstName":
                return Sort.by(Sort.Direction.ASC, "firstName");
            case "salary,asc":
                return Sort.by(Sort.Direction.ASC, "salary");
            case "salary,desc":
                return Sort.by(Sort.Direction.DESC, "salary");
            case "joiningDate,desc":
                return Sort.by(Sort.Direction.DESC, "joiningDate");
            default:
                return Sort.by(Sort.Direction.ASC, "id");
        }
    }
}
