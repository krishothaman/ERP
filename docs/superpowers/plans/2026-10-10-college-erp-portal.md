# College ERP Portal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A simple Spring Boot college ERP portal with Admin, Faculty and Student roles covering students, courses, attendance and marks.

**Architecture:** Single Spring Boot 3 app, layered entity → repository → service → controller → Thymeleaf template. Spring Security form login with role-based URL rules. JPA auto-creates MySQL tables; a seeder loads demo data on an empty DB.

**Tech Stack:** Java 22, Maven, Spring Boot 3.4.x (web, thymeleaf, data-jpa, security, validation), thymeleaf-extras-springsecurity6, MySQL Connector/J, H2 (test scope), Bootstrap 5.3.3 via jsDelivr CDN.

**Spec:** `docs/superpowers/specs/2026-10-10-college-erp-portal-design.md`

## Global Constraints

- Base package: `com.college.erp`
- DB URL: `jdbc:mysql://localhost:3306/college_erp?createDatabaseIfNotExist=true`, user `root`, password `root123`
- Default admin `admin` / `admin123`; student login = rollNo / rollNo; faculty login = email / email
- Grades: O ≥ 90, A+ ≥ 80, A ≥ 70, B+ ≥ 60, B ≥ 50, C ≥ 40, F < 40
- Attendance below 75% shown red
- A student's courses = courses with same department and year
- Tests run on H2 in-memory (`src/test/resources/application.properties`), never touch MySQL
- Git: run as `git -c safe.directory=D:/projects/app ...` (drive does not record ownership)

---

### Task 1: Project skeleton + grade/attendance logic

**Files:**
- Create: `pom.xml`, `src/main/java/com/college/erp/ErpApplication.java`, `src/main/resources/application.properties`, `src/test/resources/application.properties`
- Create: `src/main/java/com/college/erp/service/GradeUtil.java`
- Test: `src/test/java/com/college/erp/service/GradeUtilTest.java`

**Interfaces:**
- Produces: `GradeUtil.grade(int marks): String`, `GradeUtil.percent(long present, long total): double` (rounded to 1 decimal, 0 when total = 0)

- [ ] **Step 1: Write failing test**

```java
class GradeUtilTest {
    @Test void grades() {
        assertEquals("O", GradeUtil.grade(95));
        assertEquals("A+", GradeUtil.grade(80));
        assertEquals("A", GradeUtil.grade(79));
        assertEquals("B+", GradeUtil.grade(60));
        assertEquals("B", GradeUtil.grade(50));
        assertEquals("C", GradeUtil.grade(40));
        assertEquals("F", GradeUtil.grade(39));
    }
    @Test void percent() {
        assertEquals(0.0, GradeUtil.percent(0, 0));
        assertEquals(75.0, GradeUtil.percent(3, 4));
        assertEquals(66.7, GradeUtil.percent(2, 3));
    }
}
```

- [ ] **Step 2:** `mvn -q test -Dtest=GradeUtilTest` → FAIL (class missing)
- [ ] **Step 3:** Implement `GradeUtil` with the thresholds above; `percent` = `Math.round(present * 1000.0 / total) / 10.0`
- [ ] **Step 4:** `mvn -q test -Dtest=GradeUtilTest` → PASS
- [ ] **Step 5:** Commit `feat: project skeleton and grade logic`

### Task 2: Entities and repositories

**Files:**
- Create: `entity/Role.java` (enum ADMIN, FACULTY, STUDENT), `entity/User.java`, `entity/Student.java`, `entity/Faculty.java`, `entity/Course.java`, `entity/Attendance.java`, `entity/Mark.java`
- Create: `repository/UserRepository`, `StudentRepository`, `FacultyRepository`, `CourseRepository`, `AttendanceRepository`, `MarkRepository`

**Interfaces (repository methods later tasks rely on):**
- `UserRepository.findByUsername(String): Optional<User>`, `existsByUsername(String)`
- `StudentRepository.findByUser(User): Optional<Student>`, `findByDepartmentAndYearOrderByRollNo(String, int): List<Student>`, `existsByRollNo(String)`, `search(String q): List<Student>` (JPQL LIKE on name/rollNo/department)
- `FacultyRepository.findByUser(User): Optional<Faculty>`, `existsByEmail(String)`
- `CourseRepository.findByFaculty(Faculty): List<Course>`, `findByDepartmentAndYear(String, int): List<Course>`, `existsByCode(String)`
- `AttendanceRepository.findByCourseAndDate(Course, LocalDate)`, `countByStudentAndCourse(Student, Course)`, `countByStudentAndCourseAndPresentTrue(Student, Course)`, `deleteByStudent(Student)`, `deleteByCourse(Course)`
- `MarkRepository.findByCourse(Course)`, `findByStudentAndCourse(Student, Course): Optional<Mark>`, `deleteByStudent(Student)`, `deleteByCourse(Course)`

Entities use Jakarta validation: `@NotBlank` on names/codes/rollNo/department, `@Min(1) @Max(4)` year, `@Min(0) @Max(100)` marks, `@Email` email. Unique constraints per spec.

- [ ] **Step 1:** Write entities + repositories
- [ ] **Step 2:** `mvn -q test` → context loads in Task 3's test; for now `mvn -q compile` succeeds
- [ ] **Step 3:** Commit `feat: entities and repositories`

### Task 3: Security, login, seeder, role redirects

**Files:**
- Create: `config/SecurityConfig.java`, `config/DataSeeder.java`, `service/UserService.java`, `service/CurrentUser.java`, `controller/AuthController.java`
- Create templates: `layout.html` (navbar fragment), `login.html`, `change-password.html`, `error/403.html`
- Test: `src/test/java/com/college/erp/SecurityTest.java`

**Interfaces:**
- `UserService implements UserDetailsService`; `createUser(String username, String rawPassword, Role role): User`; `changePassword(String username, String oldPw, String newPw): boolean`
- `CurrentUser.student(Principal): Student`, `CurrentUser.faculty(Principal): Faculty`
- URL rules: `/admin/**` ADMIN, `/faculty/**` FACULTY, `/student/**` STUDENT; `/login`, `/css/**` public; `/` redirects by role

- [ ] **Step 1: Write failing test**

```java
@SpringBootTest @AutoConfigureMockMvc
class SecurityTest {
    @Autowired MockMvc mvc;
    @Test void anonymousRedirectedToLogin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrlPattern("**/login"));
    }
    @Test @WithMockUser(roles = "STUDENT") void studentBlockedFromAdmin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }
    @Test void adminLoginRedirectsToAdmin() throws Exception {
        mvc.perform(formLogin().user("admin").password("admin123"))
           .andExpect(redirectedUrl("/"));
    }
    @Test @WithMockUser(username = "admin", roles = "ADMIN") void rootRedirectsAdmin() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/admin"));
    }
}
```

- [ ] **Step 2:** Run → FAIL
- [ ] **Step 3:** Implement SecurityConfig (BCrypt, formLogin `/login`, defaultSuccessUrl `/`, accessDeniedPage `/403`), UserService, AuthController (`/`, `/login`, `/403`, `/change-password` GET/POST), DataSeeder (CommandLineRunner, only if `userRepository.count()==0`)
- [ ] **Step 4:** Run → PASS
- [ ] **Step 5:** Commit `feat: security, login and demo data`

### Task 4: Admin module

**Files:**
- Create: `controller/AdminController.java`
- Create templates: `admin/dashboard.html`, `admin/students.html`, `admin/student-form.html`, `admin/faculty.html`, `admin/faculty-form.html`, `admin/courses.html`, `admin/course-form.html`
- Test: add to `src/test/java/com/college/erp/AdminControllerTest.java`

**Behaviour:** CRUD per spec. Creating student/faculty calls `UserService.createUser`. Duplicate rollNo/code/email → `bindingResult.rejectValue(...)`. Delete student → delete attendance, marks, student, user. Delete course → attendance, marks, course. Delete faculty → set courses' faculty null, delete faculty, user.

- [ ] **Step 1: Write failing test**

```java
@SpringBootTest @AutoConfigureMockMvc @WithMockUser(roles = "ADMIN")
class AdminControllerTest {
    @Autowired MockMvc mvc; @Autowired StudentRepository students; @Autowired UserRepository users;
    @Test void addStudentCreatesLogin() throws Exception {
        mvc.perform(post("/admin/students/save").with(csrf())
            .param("rollNo", "T999").param("name", "Test").param("department", "CSE")
            .param("year", "1").param("email", "t@x.com").param("phone", "1"))
           .andExpect(redirectedUrl("/admin/students"));
        assertTrue(students.existsByRollNo("T999"));
        assertTrue(users.existsByUsername("T999"));
    }
    @Test void duplicateRollNoShowsError() throws Exception {
        mvc.perform(post("/admin/students/save").with(csrf())
            .param("rollNo", "CSE2001").param("name", "Dup").param("department", "CSE").param("year", "2"))
           .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("student", "rollNo"));
    }
}
```

- [ ] **Step 2:** Run → FAIL
- [ ] **Step 3:** Implement controller + templates
- [ ] **Step 4:** Run → PASS
- [ ] **Step 5:** Commit `feat: admin module`

### Task 5: Faculty module (attendance + marks entry)

**Files:**
- Create: `service/AttendanceService.java`, `service/MarkService.java`, `controller/FacultyController.java`
- Create templates: `faculty/dashboard.html`, `faculty/attendance.html`, `faculty/marks.html`
- Test: `src/test/java/com/college/erp/FacultyFlowTest.java`

**Interfaces:**
- `AttendanceService.presentMap(Course, LocalDate): Map<Long,Boolean>` (studentId → present; empty if none); `save(Course, LocalDate, Set<Long> presentIds)` upserts a row for every student of the course
- `AttendanceService.summary(Student): List<Row>` where `record Row(Course course, long present, long total, double percent)`
- `MarkService.marksMap(Course): Map<Long,Integer>`; `save(Course, Map<Long,Integer>)` upserts, ignores blanks, rejects outside 0–100
- `MarkService.summary(Student): List<MarkRow>` where `record MarkRow(Course course, Integer marks, String grade)` (marks/grade null → "-" if not entered)
- Controller checks `course.getFaculty()` equals current faculty, else 403

- [ ] **Step 1: Write failing test**

```java
@SpringBootTest @AutoConfigureMockMvc
class FacultyFlowTest {
    @Autowired MockMvc mvc; @Autowired CourseRepository courses; @Autowired AttendanceRepository att;
    @Test @WithUserDetails("ravi@college.edu") void saveAttendance() throws Exception {
        Course c = courses.findByCode("CS201").orElseThrow();
        long before = att.count();
        mvc.perform(post("/faculty/attendance").with(csrf())
            .param("courseId", c.getId().toString()).param("date", "2030-01-01"))
           .andExpect(status().is3xxRedirection());
        assertTrue(att.count() > before);
    }
    @Test @WithUserDetails("meena@college.edu") void otherFacultyForbidden() throws Exception {
        Course c = courses.findByCode("CS201").orElseThrow();
        mvc.perform(get("/faculty/marks").param("courseId", c.getId().toString()))
           .andExpect(status().isForbidden());
    }
}
```

(Requires `CourseRepository.findByCode(String): Optional<Course>`; seed CS201 assigned to ravi@college.edu.)

- [ ] **Step 2:** Run → FAIL
- [ ] **Step 3:** Implement services, controller, templates
- [ ] **Step 4:** Run → PASS
- [ ] **Step 5:** Commit `feat: faculty attendance and marks`

### Task 6: Student module

**Files:**
- Create: `controller/StudentController.java`, templates `student/dashboard.html`, `student/attendance.html`, `student/marks.html`
- Test: `src/test/java/com/college/erp/StudentViewTest.java`

- [ ] **Step 1: Write failing test**

```java
@SpringBootTest @AutoConfigureMockMvc
class StudentViewTest {
    @Autowired MockMvc mvc;
    @Test @WithUserDetails("CSE2001") void seesOwnAttendanceAndMarks() throws Exception {
        mvc.perform(get("/student/attendance")).andExpect(status().isOk())
           .andExpect(content().string(containsString("CS201")));
        mvc.perform(get("/student/marks")).andExpect(status().isOk())
           .andExpect(content().string(containsString("CS201")));
    }
}
```

- [ ] **Step 2:** Run → FAIL
- [ ] **Step 3:** Implement
- [ ] **Step 4:** Run → PASS
- [ ] **Step 5:** Commit `feat: student views`

### Task 7: README + manual verification on MySQL

- [ ] Write `README.md` (run steps, logins, features, stack, project structure)
- [ ] `mvn spring-boot:run` against MySQL; click through as admin, faculty, student in the browser; fix issues
- [ ] Commit `docs: README`
