# College ERP Portal — Design

**Date:** 2026-10-10
**Goal:** A very simple college/student ERP web portal in Java for a college project, buildable in under 2 days and easy to explain in a viva.

## Stack

- Java 22 (runs on 17+), Maven 3.9
- Spring Boot 3 (embedded Tomcat, runs with `mvn spring-boot:run`)
- Spring Web MVC + Thymeleaf (server-rendered HTML) + Bootstrap 5 (CDN)
- Spring Data JPA (Hibernate), `ddl-auto=update` creates tables automatically
- Spring Security (form login, BCrypt passwords, role-based access)
- MySQL (local, root / root123, database `college_erp` auto-created via `createDatabaseIfNotExist=true`)

## Roles

| Role | Can do |
|---|---|
| ADMIN | Manage students, faculty, courses; see dashboard counts |
| FACULTY | See own courses; mark attendance; enter marks |
| STUDENT | See own profile, attendance %, marks and grades |

All roles can change their own password.

## Data Model

| Entity | Fields | Notes |
|---|---|---|
| `User` (`users`) | id, username (unique), password (BCrypt), role | role ∈ ADMIN, FACULTY, STUDENT |
| `Student` | id, rollNo (unique), name, department, year (1–4), email, phone, user (1:1) | |
| `Faculty` | id, name, department, email (unique), user (1:1) | |
| `Course` | id, code (unique), name, department, year (1–4), credits, faculty (many:1, nullable) | |
| `Attendance` | id, student, course, date, present (boolean) | unique (student, course, date) |
| `Mark` | id, student, course, marks (0–100) | unique (student, course); grade computed |

**No enrollment table:** a student takes every course whose department and year match the student's.

**Login creation:** adding a student creates a `User` with username = rollNo, password = rollNo. Adding faculty creates a `User` with username = email, password = email. Default admin: `admin` / `admin123`.

**Seed data:** on first run (empty DB), create admin, 2 faculty, 3 courses, 6 students, and some attendance and marks so the demo looks real.

## Grade Rules

O ≥ 90, A+ ≥ 80, A ≥ 70, B+ ≥ 60, B ≥ 50, C ≥ 40, F < 40.

Attendance % = present / total × 100 (0 when no classes). Below 75% is highlighted red.

## Screens

**Common:** `/login`, `/logout`, `/change-password`. After login, redirect to `/admin`, `/faculty` or `/student` by role.

**Admin (`/admin/**`)**
- `/admin` — counts of students, faculty, courses
- `/admin/students` — list + search (name / roll no / department); add, edit, delete
- `/admin/faculty` — list; add, edit, delete
- `/admin/courses` — list; add, edit, delete; assign faculty

**Faculty (`/faculty/**`)**
- `/faculty` — my courses
- `/faculty/attendance?courseId&date` — students of the course with Present checkboxes (default ticked; existing records pre-filled); save upserts
- `/faculty/marks?courseId` — students of the course with marks inputs (pre-filled); save upserts

Faculty may only access courses assigned to them.

**Student (`/student/**`)**
- `/student` — profile
- `/student/attendance` — per course: attended / total, %, red if < 75%
- `/student/marks` — per course: marks, grade

## Error Handling

- Validation on forms (required fields, year 1–4, marks 0–100); errors shown inline in red.
- Duplicate roll no / course code / faculty email → form error, not a crash.
- Deleting a student or course deletes its attendance and marks. Deleting faculty unassigns their courses and removes their login.
- Wrong role on a URL → 403 "Access denied" page.

## Project Layout

```
src/main/java/com/college/erp/
  ErpApplication.java
  config/      SecurityConfig, DataSeeder
  entity/      User, Role, Student, Faculty, Course, Attendance, Mark
  repository/  one Spring Data repository per entity
  service/     GradeUtil, AttendanceService, MarkService, UserService, CurrentUser
  controller/  AuthController, AdminController, FacultyController, StudentController
src/main/resources/
  application.properties
  templates/   layout fragment + pages per role
```

## Testing

- Unit tests: grade calculation, attendance percentage.
- Integration tests (H2 in-memory, test scope only): role access (student blocked from `/admin`, admin allowed), login redirect.
- Manual: run against MySQL and click through every screen per role in a browser.

## Out of Scope

Timetable, fees, exam scheduling, file uploads, email, REST API, enrollment management, multi-semester history.
