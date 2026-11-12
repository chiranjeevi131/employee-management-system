package com.ems.controller;
import java.io.ByteArrayOutputStream;
import com.ems.service.AttendanceService;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.LocalDate;

import com.ems.entity.Attendance;
import com.ems.entity.AttendanceStatus;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import com.ems.entity.Employee;
import com.ems.entity.Department;
import com.ems.service.EmployeeService;
import com.ems.service.DepartmentService;
import com.ems.service.EmployeePhotoService;
import com.ems.service.EmployeeImportService;
@Controller
public class HomeController {
    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final EmployeePhotoService employeePhotoService;
    private final EmployeeImportService employeeImportService;
    private final AttendanceService attendanceService;
    public HomeController(
            EmployeeService employeeService,
            DepartmentService departmentService,
            EmployeePhotoService employeePhotoService,
            EmployeeImportService employeeImportService,
            AttendanceService attendanceService) {

        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.employeePhotoService = employeePhotoService;
        this.employeeImportService = employeeImportService;
        this.attendanceService = attendanceService;
    }
 // Dashboard - ADMIN and HR
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @GetMapping("/dashboard")
    public String showDashboard(Model model) {

        List<Employee> employees = employeeService.getAllEmployees();

        long totalEmployees = employees.size();

        long departmentCount = employees.stream()
                .map(Employee::getDepartment)
                .filter(department -> department != null && !department.isBlank())
                .distinct()
                .count();

        double averageSalary = employees.stream()
                .filter(employee -> employee.getSalary() != null)
                .mapToDouble(Employee::getSalary)
                .average()
                .orElse(0.0);

        // TODAY'S ATTENDANCE
        LocalDate today = LocalDate.now();

        List<Attendance> todayAttendance =
                attendanceService.getAttendanceByDate(today);

        long presentCount = todayAttendance.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT)
                .count();

        long absentCount = todayAttendance.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                .count();

        long halfDayCount = todayAttendance.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.HALF_DAY)
                .count();

        long leaveCount = todayAttendance.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.LEAVE)
                .count();

        // EXISTING DASHBOARD DATA
        model.addAttribute("totalEmployees", totalEmployees);
        model.addAttribute("departmentCount", departmentCount);
        model.addAttribute("averageSalary", averageSalary);
        model.addAttribute("employees", employees);

        // ATTENDANCE DASHBOARD DATA
        model.addAttribute("presentCount", presentCount);
        model.addAttribute("absentCount", absentCount);
        model.addAttribute("halfDayCount", halfDayCount);
        model.addAttribute("leaveCount", leaveCount);
        model.addAttribute("todayAttendanceCount", todayAttendance.size());

        return "dashboard";
    }
    // Display Employees with Search, Pagination, Filtering and Sorting
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @GetMapping("/employees")
    public String showEmployees(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) Long departmentId,
    @RequestParam(defaultValue = "id") String sortBy,
    Authentication authentication,
    Model model) {
        int pageSize = 5;
        Page<Employee> employeePage = employeeService.filterEmployees(
        departmentId, keyword, page, pageSize, sortBy);
        model.addAttribute("employees", employeePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", employeePage.getTotalPages());
        model.addAttribute("totalItems", employeePage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("departments", departmentService.getAllDepartments());
        boolean isAdmin = authentication.getAuthorities().stream()
        .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        return "employees";
    }
    // Export all employees matching the current search/filter/sort to Excel
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @GetMapping("/employees/export/excel")
    public ResponseEntity<byte[]> exportEmployeesToExcel(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) Long departmentId,
    @RequestParam(defaultValue = "id") String sortBy) throws IOException {
        List<Employee> employees = employeeService.getEmployeesForExport(
        departmentId, keyword, sortBy);
        try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Employees");
            // Header styling
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            String[] columns = {
                "Employee ID", "First Name", "Last Name", "Email",
                "Phone", "Department", "Salary", "Joining Date"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }
            DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
            int rowNumber = 1;
            for (Employee employee : employees) {
                Row row = sheet.createRow(rowNumber++);
                setCellValue(row, 0, employee.getId());
                setCellValue(row, 1, employee.getFirstName());
                setCellValue(row, 2, employee.getLastName());
                setCellValue(row, 3, employee.getEmail());
                setCellValue(row, 4, employee.getPhone());
                setCellValue(row, 5, employee.getDepartment());
                setCellValue(row, 6, employee.getSalary());
                setCellValue(row, 7, employee.getJoiningDate() == null
                ? "" : employee.getJoiningDate().format(dateFormatter));
            }
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
            byte[] excelFile = outputStream.toByteArray();
            return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=employees.xlsx")
            .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .contentLength(excelFile.length)
            .body(excelFile);
        }
    }
    // Export all employees matching the current search/filter/sort to PDF
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @GetMapping("/employees/export/pdf")
    public ResponseEntity<byte[]> exportEmployeesToPdf(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) Long departmentId,
    @RequestParam(defaultValue = "id") String sortBy) throws IOException {
        List<Employee> employees = employeeService.getEmployeesForExport(
        departmentId, keyword, sortBy);
        try (PDDocument document = new PDDocument();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            final float margin = 30;
            final float rowHeight = 22;
            final float[] columnWidths = {42, 75, 75, 160, 90, 90, 75, 105};
            final String[] headers = {
                "ID", "First Name", "Last Name", "Email",
                "Phone", "Department", "Salary", "Joining Date"
            };
            PDType1Font titleFont =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font headerFont =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font bodyFont =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDPage page = new PDPage(
            new PDRectangle(
            PDRectangle.A4.getHeight(),
            PDRectangle.A4.getWidth()
            )
            );
            document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            float y = page.getMediaBox().getHeight() - margin;
            // Report title and generation date
            stream.beginText();
            stream.setFont(titleFont, 18);
            stream.newLineAtOffset(margin, y);
            stream.showText("Employee Management System - Employee Report");
            stream.endText();
            y -= 22;
            stream.beginText();
            stream.setFont(bodyFont, 9);
            stream.newLineAtOffset(margin, y);
            stream.showText("Generated: " + LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
            stream.endText();
            y -= 18;
            stream.beginText();
            stream.setFont(bodyFont, 9);
            stream.newLineAtOffset(margin, y);
            stream.showText("Total employees: " + employees.size());
            stream.endText();
            y -= 18;
            y = drawPdfTableHeader(stream, headers, columnWidths, margin, y,
            rowHeight, headerFont);
            for (Employee employee : employees) {
                if (y < margin + rowHeight) {
                    stream.close();
                    page = new PDPage(
                    new PDRectangle(
                    PDRectangle.A4.getHeight(),
                    PDRectangle.A4.getWidth()
                    )
                    );
                    document.addPage(page);
                    stream = new PDPageContentStream(document, page);
                    y = page.getMediaBox().getHeight() - margin;
                    y = drawPdfTableHeader(stream, headers, columnWidths,
                    margin, y, rowHeight, headerFont);
                }
                String[] values = {
                    String.valueOf(employee.getId()),
                    employee.getFirstName(),
                    employee.getLastName(),
                    employee.getEmail(),
                    employee.getPhone(),
                    employee.getDepartment(),
                    employee.getSalary() == null ? "" :
                    String.format("%.2f", employee.getSalary()),
                    employee.getJoiningDate() == null ? "" :
                    employee.getJoiningDate().format(
                    DateTimeFormatter.ISO_LOCAL_DATE)
                };
                float x = margin;
                float cellBottom = y - rowHeight;
                stream.setStrokingColor(
                190 / 255f,
                198 / 255f,
                208 / 255f
                );
                stream.setLineWidth(0.5f);
                for (int i = 0; i < values.length; i++) {
                    stream.addRect(x, cellBottom, columnWidths[i], rowHeight);
                    stream.stroke();
                    stream.beginText();
                    stream.setFont(bodyFont, 8);
                    stream.newLineAtOffset(x + 4, y - 14);
                    stream.showText(fitPdfText(values[i], columnWidths[i] - 8));
                    stream.endText();
                    x += columnWidths[i];
                }
                y -= rowHeight;
            }
            stream.close();
            document.save(outputStream);
            byte[] pdfFile = outputStream.toByteArray();
            return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=employees.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .contentLength(pdfFile.length)
            .body(pdfFile);
        }
    }
    private float drawPdfTableHeader(
    PDPageContentStream stream,
    String[] headers,
    float[] columnWidths,
    float margin,
    float y,
    float rowHeight,
    PDType1Font headerFont) throws IOException {
        float x = margin;
        float cellBottom = y - rowHeight;
        for (int i = 0; i < headers.length; i++) {
            stream.setNonStrokingColor(
            225 / 255f,
            233 / 255f,
            244 / 255f
            );
            stream.addRect(x, cellBottom, columnWidths[i], rowHeight);
            stream.fill();
            stream.setStrokingColor(
            150 / 255f,
            160 / 255f,
            175 / 255f
            );
            stream.addRect(x, cellBottom, columnWidths[i], rowHeight);
            stream.stroke();
            stream.beginText();
            stream.setNonStrokingColor(
            25 / 255f,
            40 / 255f,
            65 / 255f
            );
            stream.setFont(headerFont, 8);
            stream.newLineAtOffset(x + 4, y - 14);
            stream.showText(fitPdfText(headers[i], columnWidths[i] - 8));
            stream.endText();
            x += columnWidths[i];
        }
        return cellBottom;
    }
    private String fitPdfText(String value, float maxWidth) throws IOException {
        if (value == null) {
            return "";
        }
        String safeValue = value.replaceAll("[^\\x20-\\xFF]", "?");
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        while (!safeValue.isEmpty() && font.getStringWidth(safeValue) / 1000 * 8 > maxWidth) {
            safeValue = safeValue.substring(0, safeValue.length() - 1);
        }
        return safeValue;
    }
    private void setCellValue(Row row, int columnIndex, Object value) {
        Cell cell = row.createCell(columnIndex);
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }
    // Display Excel Import Page - ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/employees/import")
    public String showImportPage() {
        return "employee-import";
    }

    // Process Excel Import - ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/employees/import")
    public String importEmployees(
    @RequestParam("file") MultipartFile file,
    Model model) {
        try {
            EmployeeImportService.ImportResult result =
            employeeImportService.importEmployees(file);
            model.addAttribute("result", result);
        } catch (Exception exception) {
            model.addAttribute("error", exception.getMessage());
        }
        return "employee-import";
    }

    // Display Add Employee Form
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @GetMapping("/employees/add")
    public String showAddEmployeeForm(Model model) {
        model.addAttribute("employee", new Employee());
        model.addAttribute("departments", departmentService.getAllDepartments());
        return "add-employee";
    }
    // Save Employee with Photo Upload
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @PostMapping("/employees/save")
    public String saveEmployee(
    @ModelAttribute("employee") Employee employee,
    @RequestParam(value = "photoFile", required = false) MultipartFile photoFile)
    throws IOException {
        String photoFilename = employeePhotoService.savePhoto(photoFile);
        employee.setPhoto(photoFilename);
        Department selectedDepartment = departmentService.getDepartmentById(
        employee.getDepartmentEntity().getId());
        employee.setDepartmentEntity(selectedDepartment);
        employee.setDepartment(selectedDepartment.getName());
        employeeService.saveEmployee(employee);
        return "redirect:/employees";
    }
    // Display Edit Employee Form
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @GetMapping("/employees/edit/{id}")
    public String showEditEmployeeForm(@PathVariable Long id, Model model) {
        Employee employee = employeeService.getEmployeeById(id);
        model.addAttribute("employee", employee);
        model.addAttribute("departments", departmentService.getAllDepartments());
        return "edit-employee";
    }
    // Update Employee with Photo Replacement and Old Photo Deletion
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @PostMapping("/employees/update/{id}")
    public String updateEmployee(
    @PathVariable Long id,
    @ModelAttribute("employee") Employee employee,
    @RequestParam(value = "photoFile", required = false) MultipartFile photoFile)
    throws IOException {
        Employee existingEmployee = employeeService.getEmployeeById(id);
        String oldPhoto = existingEmployee.getPhoto();
        String newPhoto = null;
        Department selectedDepartment = departmentService.getDepartmentById(
        employee.getDepartmentEntity().getId());
        employee.setDepartmentEntity(selectedDepartment);
        employee.setDepartment(selectedDepartment.getName());
        if (photoFile != null && !photoFile.isEmpty()) {
            newPhoto = employeePhotoService.savePhoto(photoFile);
            employee.setPhoto(newPhoto);
        }
        try {
            employeeService.updateEmployee(id, employee);
        } catch (RuntimeException exception) {
            if (newPhoto != null) {
                try {
                    employeePhotoService.deletePhoto(newPhoto);
                } catch (IOException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            throw exception;
        }
        if (newPhoto != null && oldPhoto != null && !oldPhoto.isBlank()
        && !oldPhoto.equals(newPhoto)) {
            try {
                employeePhotoService.deletePhoto(oldPhoto);
                System.out.println("Old employee photo deleted successfully: " + oldPhoto);
            } catch (IOException exception) {
                System.err.println("Could not delete old employee photo: " + exception.getMessage());
            }
        }
        return "redirect:/employees";
    }
    // Delete Employee and Automatically Delete Its Photo - ADMIN ONLY
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/employees/delete/{id}")
    public String deleteEmployee(@PathVariable Long id) {
        Employee employee = employeeService.getEmployeeById(id);
        String photoFilename = employee.getPhoto();
        employeeService.deleteEmployee(id);
        if (photoFilename != null && !photoFilename.isBlank()) {
            try {
                employeePhotoService.deletePhoto(photoFilename);
                System.out.println("Employee photo deleted successfully: " + photoFilename);
            } catch (IOException exception) {
                System.err.println("Could not delete employee photo: " + exception.getMessage());
            }
        }
        return "redirect:/employees";
    }
    // Home Page
    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
    // View Employee Details
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @GetMapping("/employees/view/{id}")
    public String viewEmployeeDetails(@PathVariable Long id, Model model) {
        Employee employee = employeeService.getEmployeeById(id);
        model.addAttribute("employee", employee);
        return "employee-details";
    }
}
