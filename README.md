# Employee Management System (EMS)

## Project Overview
The Employee Management System (EMS) is a Java Full Stack web application designed to manage employee information, departments, attendance, and administrative activities.

## Technologies Used
- Java
- Spring Boot
- Spring MVC
- Spring Data JPA
- Spring Security
- Thymeleaf
- MySQL
- HTML, CSS, JavaScript
- Chart.js
- Apache POI
- jsPDF

## Features

### Employee Management
- Add, update, view, and delete employees.
- Search, sort, and paginate employee records.
- Upload employee profile photos.
- Import and export employee data using Excel.
- Export employee information to PDF.

### Department Management
- Create and manage departments.

### Attendance Management
- Mark employee attendance.
- Check-in and check-out.
- View attendance history.
- Generate attendance reports.
- Export attendance reports to CSV and PDF.

### Dashboard
- Employee statistics.
- Department distribution chart.
- Monthly joining analytics.
- Salary distribution chart.
- Attendance statistics.
- Recent employee activities.

### User Management and Security
- Secure login and logout.
- Role-based access for ADMIN and HR.
- HR account creation and deletion.
- User profile management.
- Change password functionality.

### Audit Logs
- Track employee-related activities.
- Search and filter audit records.
- Export audit logs.

## Installation and Setup

1. Clone or download this repository.
2. Import the project into Spring Tools for Eclipse as an existing Maven project.
3. Create a MySQL database for the application.
4. Configure the database connection in `application.properties`.
5. Run the application as a Spring Boot application.

## Running the Application

Open the following address after starting the application:

http://localhost:8081

## Project Structure

```text
src/main/java
    └── com.ems
        ├── controller
        ├── entity
        ├── repository
        ├── service
        └── configuration

src/main/resources
    ├── static
    ├── templates
    └── application.properties
```

## Future Enhancements
- Additional reporting capabilities.
- Further UI and performance improvements.

## Author
Employee Management System Project