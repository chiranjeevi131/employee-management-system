
package com.ems.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ems.entity.Department;
import com.ems.entity.Employee;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Service
public class EmployeeImportService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final Validator validator;

    public EmployeeImportService(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            Validator validator) {

        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.validator = validator;
    }

    public ImportResult importEmployees(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select an Excel file.");
        }

        String filename = file.getOriginalFilename();

        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only .xlsx files are supported.");
        }

        ImportResult result = new ImportResult();

        Set<String> importedEmails = new HashSet<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() <= 1) {
                throw new IllegalArgumentException(
                        "The Excel file contains no employee records.");
            }

            for (int rowIndex = 1;
                    rowIndex <= sheet.getLastRowNum();
                    rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (row == null || isEmptyRow(row, formatter)) {
                    continue;
                }

                result.setTotalRecords(result.getTotalRecords() + 1);

                try {

                    Employee employee = mapEmployee(row, formatter);

                    Set<ConstraintViolation<Employee>> violations =
                            validator.validate(employee);

                    if (!violations.isEmpty()) {
                        String message =
                                violations.iterator().next().getMessage();

                        throw new IllegalArgumentException(message);
                    }

                    String email = employee.getEmail()
                            .trim()
                            .toLowerCase();

                    if (!importedEmails.add(email)
                            || employeeRepository
                                    .existsByEmailIgnoreCase(email)) {

                        throw new IllegalArgumentException(
                                "Duplicate email address: " + email);
                    }

                    Department department = departmentRepository
                            .findByNameIgnoreCase(
                                    employee.getDepartment().trim())
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Department not found: "
                                                    + employee.getDepartment()));

                    employee.setDepartment(department.getName());
                    employee.setDepartmentEntity(department);
                    employee.setPhoto(null);

                    employeeRepository.save(employee);

                    result.setSuccessfulRecords(
                            result.getSuccessfulRecords() + 1);

                } catch (Exception exception) {

                    result.getErrors().add(
                            "Row " + (rowIndex + 1) + ": "
                                    + exception.getMessage());
                }
            }
        }

        return result;
    }

    private Employee mapEmployee(Row row, DataFormatter formatter) {

        Employee employee = new Employee();

        employee.setFirstName(getCellValue(row, 0, formatter));
        employee.setLastName(getCellValue(row, 1, formatter));
        employee.setEmail(getCellValue(row, 2, formatter));
        employee.setPhone(getCellValue(row, 3, formatter));
        employee.setDepartment(getCellValue(row, 4, formatter));

        String salaryValue = getCellValue(row, 5, formatter);

        if (salaryValue.isBlank()) {
            throw new IllegalArgumentException("Salary is required.");
        }

        employee.setSalary(
                Double.parseDouble(salaryValue.replace(",", "")));

        employee.setJoiningDate(getDateValue(row.getCell(6)));

        return employee;
    }

    private String getCellValue(
            Row row,
            int columnIndex,
            DataFormatter formatter) {

        Cell cell = row.getCell(columnIndex);

        if (cell == null) {
            return "";
        }

        return formatter.formatCellValue(cell).trim();
    }

    private LocalDate getDateValue(Cell cell) {

        if (cell == null) {
            throw new IllegalArgumentException(
                    "Joining date is required.");
        }

        if (cell.getCellType() == CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {

            Date date = cell.getDateCellValue();

            return date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }

        String dateValue = cell.toString().trim();

        if (dateValue.isBlank()) {
            throw new IllegalArgumentException(
                    "Joining date is required.");
        }

        return LocalDate.parse(dateValue);
    }

    private boolean isEmptyRow(
            Row row,
            DataFormatter formatter) {

        for (int i = 0; i < 7; i++) {

            if (!getCellValue(row, i, formatter).isBlank()) {
                return false;
            }
        }

        return true;
    }

    public static class ImportResult {

        private int totalRecords;
        private int successfulRecords;

        private final List<String> errors = new ArrayList<>();

        public int getTotalRecords() {
            return totalRecords;
        }

        public void setTotalRecords(int totalRecords) {
            this.totalRecords = totalRecords;
        }

        public int getSuccessfulRecords() {
            return successfulRecords;
        }

        public void setSuccessfulRecords(int successfulRecords) {
            this.successfulRecords = successfulRecords;
        }

        public int getFailedRecords() {
            return totalRecords - successfulRecords;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
