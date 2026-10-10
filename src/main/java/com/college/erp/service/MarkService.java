package com.college.erp.service;

import com.college.erp.entity.Course;
import com.college.erp.entity.Mark;
import com.college.erp.entity.Student;
import com.college.erp.repository.CourseRepository;
import com.college.erp.repository.MarkRepository;
import com.college.erp.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MarkService {

    /** One line of a student's marks report; marks, grade and grade point are null if not entered yet. */
    public record MarkRow(Course course, Integer marks, String grade, Integer gradePoint) {

        public static MarkRow of(Course course, Integer marks) {
            if (marks == null) return new MarkRow(course, null, null, null);
            String grade = GradeUtil.grade(marks);
            return new MarkRow(course, marks, grade, GradeUtil.gradePoint(grade));
        }
    }

    /** Credit-weighted grade point average over graded courses, to 2 decimals; 0 if none graded. */
    public static double gpa(List<MarkRow> rows) {
        int credits = 0;
        int weighted = 0;
        for (MarkRow r : rows) {
            if (r.gradePoint() == null) continue;
            credits += r.course().getCredits();
            weighted += r.course().getCredits() * r.gradePoint();
        }
        return credits == 0 ? 0.0 : Math.round(weighted * 100.0 / credits) / 100.0;
    }

    public static int totalCredits(List<MarkRow> rows) {
        return rows.stream().mapToInt(r -> r.course().getCredits()).sum();
    }

    private final MarkRepository markRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public MarkService(MarkRepository markRepository, StudentRepository studentRepository,
                       CourseRepository courseRepository) {
        this.markRepository = markRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    /** studentId -> marks for a course. */
    public Map<Long, Integer> marksMap(Course course) {
        Map<Long, Integer> map = new HashMap<>();
        for (Mark m : markRepository.findByCourse(course)) {
            map.put(m.getStudent().getId(), m.getMarks());
        }
        return map;
    }

    /** Saves the given marks (studentId -> marks). Callers must check the 0-100 range first. */
    @Transactional
    public void save(Course course, Map<Long, Integer> marksByStudent) {
        for (Map.Entry<Long, Integer> entry : marksByStudent.entrySet()) {
            Student student = studentRepository.findById(entry.getKey()).orElseThrow();
            Mark mark = markRepository.findByStudentAndCourse(student, course)
                    .orElse(new Mark(student, course, entry.getValue()));
            mark.setMarks(entry.getValue());
            markRepository.save(mark);
        }
    }

    /** Marks, grade and grade point per course for one student. */
    public List<MarkRow> summary(Student student) {
        List<MarkRow> rows = new ArrayList<>();
        for (Course course : courseRepository.findByDepartmentAndYearOrderByCode(student.getDepartment(), student.getYear())) {
            Integer marks = markRepository.findByStudentAndCourse(student, course).map(Mark::getMarks).orElse(null);
            rows.add(MarkRow.of(course, marks));
        }
        return rows;
    }
}
