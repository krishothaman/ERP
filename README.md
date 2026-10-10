# College ERP Portal

A simple full-stack college ERP web application built with **Java + Spring Boot + MySQL**.
Admins manage students, faculty and courses; faculty mark attendance and enter marks; students view their own attendance and results.

## Features

| Role | What they can do |
|---|---|
| **Admin** | Dashboard with counts · Add / edit / delete / search **students** · Manage **faculty** · Manage **courses** and assign faculty |
| **Faculty** | See assigned courses · **Mark attendance** by date (edit any past day) · **Enter marks** (0–100) |
| **Student** | View profile · **Attendance %** per course (red warning below 75%) · **Marks and grades** |
| Everyone | Secure login · Change password · Logout |

- Adding a student or faculty member automatically creates their login.
- A student automatically takes every course of their **department and year**.
- Grades: O ≥ 90, A+ ≥ 80, A ≥ 70, B+ ≥ 60, B ≥ 50, C ≥ 40, F < 40.

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | HTML pages rendered with **Thymeleaf**, styled with **Bootstrap 5** |
| Backend | **Java 17+**, **Spring Boot 3** (Spring MVC, Spring Security, Spring Data JPA / Hibernate) |
| Database | **MySQL** (tables created automatically by Hibernate) |
| Build | Maven |
| Tests | JUnit 5 + Spring MockMvc (on an in-memory H2 database) |

## How to Run

**Requirements:** Java 17 or newer, Maven, MySQL running on `localhost:3306`.

1. Make sure MySQL is running. The app connects as `root` / `root123`; change this in
   `src/main/resources/application.properties` if your password is different.
2. From the project folder run:

   ```bash
   mvn spring-boot:run
   ```

3. Open **http://localhost:8080** in your browser.

The `college_erp` database, all tables, and demo data are created automatically on first start.

## Demo Logins

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Faculty | `ravi@college.edu` | `ravi@college.edu` |
| Faculty | `meena@college.edu` | `meena@college.edu` |
| Student | `CSE2001` … `CSE2006` | same as username |

`CSE2006` (Sneha Reddy) has low attendance, to show the red warning.

## Run the Tests

```bash
mvn test
```

## Project Structure

```
src/main/java/com/college/erp/
├── ErpApplication.java        Main class (starts the app)
├── config/
│   ├── SecurityConfig.java    Login, logout, role-based URL access
│   └── DataSeeder.java        Creates admin + demo data on first run
├── entity/                    Database tables as Java classes
│   ├── User, Role             Login accounts
│   ├── Student, Faculty       People
│   ├── Course                 Subjects
│   └── Attendance, Mark       Records
├── repository/                Spring Data JPA interfaces (database queries)
├── service/
│   ├── AdminService           Add/delete logic that touches several tables
│   ├── AttendanceService      Saving attendance, attendance %
│   ├── MarkService            Saving marks, grades
│   ├── UserService            Login lookup, create user, change password
│   ├── CurrentUser            Finds the logged-in student/faculty
│   └── GradeUtil              Grade and percentage calculation
└── controller/                Handle web requests, return pages
    ├── AuthController         Login, home redirect, change password
    ├── AdminController        /admin/**
    ├── FacultyController      /faculty/**
    └── StudentController      /student/**

src/main/resources/
├── application.properties     Database settings
├── static/css/app.css         Custom styles
└── templates/                 HTML pages (Thymeleaf)
    ├── layout.html            Shared header + navbar
    ├── login.html, change-password.html
    ├── admin/  faculty/  student/
    └── error/403.html
```

## Database Tables

| Table | Main columns |
|---|---|
| `users` | username, password (BCrypt hash), role |
| `student` | roll_no, name, department, study_year, email, phone, user_id |
| `faculty` | name, department, email, user_id |
| `course` | code, name, department, study_year, credits, faculty_id |
| `attendance` | student_id, course_id, class_date, present |
| `marks` | student_id, course_id, score |

## How It Works (Request Flow)

Browser → **Spring Security** checks login and role → **Controller** → **Service** (business logic) → **Repository** (JPA) → **MySQL**, then the controller returns a **Thymeleaf** page, which is rendered to HTML and sent back to the browser.
