package com.ems.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ems.entity.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Search employees by name or department
    List<Employee> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrDepartmentContainingIgnoreCase(
            String firstName, String lastName, String department);

    // Search employees with pagination
    Page<Employee> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrDepartmentContainingIgnoreCase(
            String firstName, String lastName, String department, Pageable pageable);

    // Count employees by department name
    long countByDepartmentIgnoreCase(String department);

    // Department filtering with optional keyword search and pagination
    @Query("""
            SELECT e FROM Employee e
            WHERE (:departmentId IS NULL OR e.departmentEntity.id = :departmentId)
            AND (
                :keyword IS NULL
                OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(e.department) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Employee> filterEmployees(
            @Param("departmentId") Long departmentId,
            @Param("keyword") String keyword,
            Pageable pageable);

    // Same filters for Excel export, without pagination
    @Query("""
            SELECT e FROM Employee e
            WHERE (:departmentId IS NULL OR e.departmentEntity.id = :departmentId)
            AND (
                :keyword IS NULL
                OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(e.department) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    List<Employee> filterEmployeesForExport(
            @Param("departmentId") Long departmentId,
            @Param("keyword") String keyword,
            Sort sort);
    
    boolean existsByEmailIgnoreCase(String email);
}
