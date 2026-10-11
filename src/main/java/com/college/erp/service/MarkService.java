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

    /** The three marks a faculty member enters for one student; any may be null (not entered yet). */
    public record Scores(Integer ia1, Integer ia2, Integer sem) {

        public boolean isEmpty() {
            return ia1 == null && ia2 == null && sem == null;
        }
    }

    /**
     * One line of a student's marks report. internal (out of 40) needs both IAs; marks (the total out of 100),
     * grade and grade point need all three marks, and are null until then.
     */
    public record MarkRow(Course course, Integer ia1, Integer ia2, Integer sem,
                          Integer internal, Integer marks, String grade, Integer gradePoint) {

        public static MarkRow of(Course course, Integer ia1, Integer ia2, Integer sem) {
            Integer internal = ia1 != null && ia2 != null ? GradeUtil.internal(ia1, ia2) : null;
            if (internal == null || sem == null) return new MarkRow(course, ia1, ia2, sem, internal, null, null, null);
            int total = GradeUtil.total(ia1, ia2, sem);
            String grade = GradeUtil.grade(total);
            return new MarkRow(course, ia1, ia2, sem, internal, total, grade, GradeUtil.gradePoint(grade));
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

    /** studentId -> IA-1 / IA-2 / SEM marks for a course. */
    public Map<Long, Scores> marksMap(Course course) {
        Map<Long, Scores> map = new HashMap<>();
        for (Mark m : markRepository.findByCourse(course)) {
            map.put(m.getStudent().getId(), new Scores(m.getIa1(), m.getIa2(), m.getSem()));
        }
        return map;
    }

    /**
     * Saves the given marks (studentId -> scores); a blank box clears that mark, and a student with
     * every box blank has their row removed. Callers must check the ranges first.
     */
    @Transactional
    public void save(Course course, Map<Long, Scores> scoresByStudent) {
        for (Map.Entry<Long, Scores> entry : scoresByStudent.entrySet()) {
            Student student = studentRepository.findById(entry.getKey()).orElseThrow();
            Scores s = entry.getValue();
            Optional<Mark> existing = markRepository.findByStudentAndCourse(student, course);
            if (s.isEmpty()) {
                existing.ifPresent(markRepository::delete);
                continue;
            }
            Mark mark = existing.orElse(new Mark(student, course, null, null, null));
            mark.setIa1(s.ia1());
            mark.setIa2(s.ia2());
            mark.setSem(s.sem());
            markRepository.save(mark);
        }
    }

    /** Marks, grade and grade point per course for one student. */
    public List<MarkRow> summary(Student student) {
        List<MarkRow> rows = new ArrayList<>();
        for (Course course : courseRepository.findByDepartmentAndYearOrderByCode(student.getDepartment(), student.getYear())) {
            Optional<Mark> m = markRepository.findByStudentAndCourse(student, course);
            rows.add(MarkRow.of(course, m.map(Mark::getIa1).orElse(null),
                    m.map(Mark::getIa2).orElse(null), m.map(Mark::getSem).orElse(null)));
        }
        return rows;
    }
}
