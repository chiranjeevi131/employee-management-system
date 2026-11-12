package com.ems.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ems.entity.Department;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;

@Service
public class DepartmentService {


private final DepartmentRepository departmentRepository;
private final EmployeeRepository employeeRepository;

public DepartmentService(
        DepartmentRepository departmentRepository,
        EmployeeRepository employeeRepository) {

    this.departmentRepository = departmentRepository;
    this.employeeRepository = employeeRepository;
}

// Add Department
public Department saveDepartment(Department department) {

    if (departmentRepository.existsByNameIgnoreCase(
            department.getName().trim())) {

        throw new RuntimeException("Department already exists!");
    }

    department.setName(department.getName().trim());

    return departmentRepository.save(department);
}

// Get All Departments
public List<Department> getAllDepartments() {

    return departmentRepository.findAll();
}

// Get Department By ID
public Department getDepartmentById(Long id) {

    return departmentRepository.findById(id)
            .orElseThrow(() ->
                new RuntimeException(
                    "Department not found with ID: " + id));
}

// Update Department
public Department updateDepartment(
        Long id, Department updatedDepartment) {

    Department existingDepartment = getDepartmentById(id);

    String newName = updatedDepartment.getName().trim();

    boolean duplicateExists = departmentRepository
            .findByNameIgnoreCase(newName)
            .filter(department -> !department.getId().equals(id))
            .isPresent();

    if (duplicateExists) {
        throw new RuntimeException(
                "Department name already exists!");
    }

    existingDepartment.setName(newName);

    existingDepartment.setDescription(
            updatedDepartment.getDescription());

    return departmentRepository.save(existingDepartment);
}

// Delete Department Safely
public void deleteDepartment(Long id) {

    Department department = getDepartmentById(id);

    long employeeCount = employeeRepository
            .countByDepartmentIgnoreCase(department.getName());

    if (employeeCount > 0) {

        throw new RuntimeException(
                "Cannot delete this department because "
                + employeeCount
                + " employee(s) are assigned to it.");
    }

    departmentRepository.delete(department);
}


}
