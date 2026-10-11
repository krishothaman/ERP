package com.college.erp.service;

import com.college.erp.entity.*;
import com.college.erp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin operations that touch more than one table, e.g. adding a student also creates
 * their login, and deleting a course also deletes its attendance and marks.
 */
@Service
@Transactional
public class AdminService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final AttendanceRepository attendanceRepository;
    private final MarkRepository markRepository;

    public AdminService(UserService userService, UserRepository userRepository,
                        StudentRepository studentRepository, FacultyRepository facultyRepository,
                        CourseRepository courseRepository, AttendanceRepository attendanceRepository,
                        MarkRepository markRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.courseRepository = courseRepository;
        this.attendanceRepository = attendanceRepository;
        this.markRepository = markRepository;
    }

    // ---------- Students ----------

    /** New student; login = roll number, password = roll number. */
    public void createStudent(Student student) {
        student.setUser(userService.createUser(student.getRollNo(), student.getRollNo(), Role.STUDENT));
        studentRepository.save(student);
    }

    /** Updates student profile and syncs login username if roll number changed. */
    public void updateStudent(Student form) {
        Student existing = studentRepository.findById(form.getId()).orElseThrow();
        String oldRollNo = existing.getRollNo();
        String newRollNo = form.getRollNo();

        existing.setRollNo(newRollNo);
        existing.setName(form.getName());
        existing.setDepartment(form.getDepartment());
        existing.setYear(form.getYear());
        existing.setEmail(form.getEmail());
        existing.setPhone(form.getPhone());

        if (newRollNo != null && !newRollNo.equals(oldRollNo) && existing.getUser() != null) {
            User user = existing.getUser();
            user.setUsername(newRollNo);
            userRepository.save(user);
        }
    }

    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id).orElseThrow();
        User user = student.getUser();
        // Bulk-delete child rows immediately (JPQL @Modifying queries hit the DB right away).
        attendanceRepository.deleteByStudent(student);
        markRepository.deleteByStudent(student);
        // Null the user FK before deleting student, then delete user last.
        student.setUser(null);
        studentRepository.saveAndFlush(student);
        studentRepository.delete(student);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    // ---------- Faculty ----------

    /** New faculty member; login = email, password = email. */
    public void createFaculty(Faculty faculty) {
        faculty.setUser(userService.createUser(faculty.getEmail(), faculty.getEmail(), Role.FACULTY));
        facultyRepository.save(faculty);
    }

    /** Email is not changed on edit because it is also the login username. */
    public void updateFaculty(Faculty form) {
        Faculty existing = facultyRepository.findById(form.getId()).orElseThrow();
        existing.setName(form.getName());
        existing.setDepartment(form.getDepartment());
    }

    /** Their courses become unassigned rather than deleted. */
    public void deleteFaculty(Long id) {
        Faculty faculty = facultyRepository.findById(id).orElseThrow();
        for (Course course : courseRepository.findByFacultyOrderByCode(faculty)) {
            course.setFaculty(null);
        }
        facultyRepository.delete(faculty);
        if (faculty.getUser() != null) {
            userRepository.delete(faculty.getUser());
        }
    }

    // ---------- Courses ----------

    public void saveCourse(Course form, Long facultyId) {
        Faculty faculty = facultyId == null ? null : facultyRepository.findById(facultyId).orElse(null);
        if (form.getId() == null) {
            form.setFaculty(faculty);
            courseRepository.save(form);
            return;
        }
        Course existing = courseRepository.findById(form.getId()).orElseThrow();
        existing.setName(form.getName());
        existing.setDepartment(form.getDepartment());
        existing.setYear(form.getYear());
        existing.setCredits(form.getCredits());
        existing.setFaculty(faculty);
    }

    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id).orElseThrow();
        attendanceRepository.deleteByCourse(course);
        markRepository.deleteByCourse(course);
        courseRepository.delete(course);
    }
}
