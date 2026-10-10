package com.college.erp.service;

import com.college.erp.entity.Attendance;
import com.college.erp.entity.Course;
import com.college.erp.entity.Student;
import com.college.erp.repository.AttendanceRepository;
import com.college.erp.repository.CourseRepository;
import com.college.erp.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class AttendanceService {

    /** One line of a student's attendance report. */
    public record Row(Course course, long present, long total, double percent) {
        public boolean low() {
            return total > 0 && percent < 75;
        }
    }

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public AttendanceService(AttendanceRepository attendanceRepository, StudentRepository studentRepository,
                             CourseRepository courseRepository) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    /** Students who take this course (same department and year). */
    public List<Student> studentsOf(Course course) {
        return studentRepository.findByDepartmentAndYearOrderByRollNo(course.getDepartment(), course.getYear());
    }

    /** studentId -> present, for a course on a day. Empty if attendance was not taken yet. */
    public Map<Long, Boolean> presentMap(Course course, LocalDate date) {
        Map<Long, Boolean> map = new HashMap<>();
        for (Attendance a : attendanceRepository.findByCourseAndDate(course, date)) {
            map.put(a.getStudent().getId(), a.isPresent());
        }
        return map;
    }

    /** Saves a row for every student of the course; ticked students are present, the rest absent. */
    @Transactional
    public void save(Course course, LocalDate date, Set<Long> presentIds) {
        for (Student student : studentsOf(course)) {
            boolean present = presentIds.contains(student.getId());
            Attendance a = attendanceRepository.findByStudentAndCourseAndDate(student, course, date)
                    .orElse(new Attendance(student, course, date, present));
            a.setPresent(present);
            attendanceRepository.save(a);
        }
    }

    /** Attendance per course for one student. */
    public List<Row> summary(Student student) {
        List<Row> rows = new ArrayList<>();
        for (Course course : courseRepository.findByDepartmentAndYearOrderByCode(student.getDepartment(), student.getYear())) {
            long total = attendanceRepository.countByStudentAndCourse(student, course);
            long present = attendanceRepository.countByStudentAndCourseAndPresentTrue(student, course);
            rows.add(new Row(course, present, total, GradeUtil.percent(present, total)));
        }
        return rows;
    }
}
