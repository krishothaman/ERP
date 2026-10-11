# College ERP Portal

A simple full-stack college ERP web application built with **Java + Spring Boot + MySQL**.
Admins manage students, faculty and courses; faculty mark attendance and enter marks; students view their own attendance and results.

## Features

| Role | What they can do |
|---|---|
| **Admin** | Dashboard (counts, students-per-department chart, low-attendance list) · Add / edit / delete / search **students** · Manage **faculty** · Manage **courses** and assign faculty |
| **Faculty** | Dashboard with per-course class attendance · **Mark attendance** per session (1–3 hours, Present / Absent / OD-ML, edit any past day) · **Enter marks**: IA-1, IA-2 (out of 50) and SEM (out of 100), with a live total and grade |
| **Student** | Dashboard (attendance chart, hours donut, GPA) · Personal details · Course list · **Attendance details** in hours · **Grade / Mark & Credit** with GPA |
| Everyone | Secure login · Change password · Logout |

- Adding a student or faculty member automatically creates their login.
- A student automatically takes every course of their **department and year** (department is stored upper-case, so "cse" = "CSE").
- Marks: **IA-1** and **IA-2** are out of 50, **SEM** out of 100. **Total** (out of 100) = (IA-1 + IA-2) × 0.4 + SEM × 0.6, i.e. internals 40% and semester exam 60%. A course is graded once all three are entered.
- Grades (on the total): O ≥ 90, A+ ≥ 80, A ≥ 70, B+ ≥ 60, B ≥ 50, C ≥ 40, F < 40. Grade points: O 10, A+ 9, A 8, B+ 7, B 6, C 5, F 0.
- **GPA** = Σ(credits × grade point) ÷ Σ(credits of graded courses).

### Attendance rules

Attendance is counted in **hours**. Each class session is 1–3 hours and every student is marked Present, Absent or OD/ML.

| Column | Meaning |
|---|---|
| Max. hours | All hours held |
| Att. hours | Hours present |
| Absent hours | Hours absent |
| Average % | Att. hours ÷ Max. hours |
| OD/ML % | OD/ML hours ÷ Max. hours |
| Total % | (Att. + OD/ML hours) ÷ Max. hours (**below 75% is shown in red**) |

## Design

The layout follows a classic admin-dashboard structure (left sidebar, top bar, stat cards, charts, tables).
Visual style: warm cream background, dark sidebar, a single orange accent for primary actions, pill-shaped buttons and inputs, and hairline-bordered cards.
Fonts: Bricolage Grotesque (headings), Geist (body), JetBrains Mono (roll numbers and course codes). Charts use Chart.js.
The portal name is set once in `application.properties` (`app.name`).

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | HTML pages rendered with **Thymeleaf**, **Bootstrap 5** grid + custom CSS, **Chart.js** charts |
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
| Faculty | `anil@college.edu` | `anil@college.edu` |
| Student | `CSE2001` … `CSE2006` | same as username |
| Student | `ECE2001` … `ECE2003` | same as username |

`CSE2006` (Sneha Reddy) has low attendance, to show the red warning.

> **Reset demo data:** stop the app, run `DROP DATABASE college_erp;` in MySQL, and start the app again.

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
│   ├── Attendance, Mark       Records (attendance = hours + status, mark = IA-1 / IA-2 / SEM)
│   └── AttendanceStatus       PRESENT / ABSENT / OD
├── repository/                Spring Data JPA interfaces (database queries)
├── service/
│   ├── AdminService           Add/delete logic that touches several tables
│   ├── AttendanceService      Saving attendance, hours and percentages
│   ├── DashboardService       Dashboard numbers (departments, low attendance, class stats)
│   ├── MarkService            Saving marks, grades
│   ├── UserService            Login lookup, create user, change password
│   ├── CurrentUser            Finds the logged-in student/faculty
│   └── GradeUtil              Grade, grade point, total mark and percentage calculation
└── controller/                Handle web requests, return pages
    ├── AuthController         Login, home redirect, change password
    ├── AdminController        /admin/**
    ├── FacultyController      /faculty/**
    ├── StudentController      /student/**
    └── GlobalModel            Portal name, current page, user name for every page

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
| `exam_marks` | student_id, course_id, ia1, ia2, sem |

## How It Works (Request Flow)

Browser → **Spring Security** checks login and role → **Controller** → **Service** (business logic) → **Repository** (JPA) → **MySQL**, then the controller returns a **Thymeleaf** page, which is rendered to HTML and sent back to the browser.
