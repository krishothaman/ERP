package com.college.erp.service;

import com.college.erp.entity.Attendance;
import com.college.erp.entity.AttendanceStatus;
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

    public static final double MIN_PERCENT = 75.0;

    /**
     * One course line of a student's attendance, in hours.
     * Average % counts only hours present; Total % also counts OD/ML hours.
     */
    public record Row(Course course, long max, long attended, long absent, long od,
                      double averagePct, double odPct, double totalPct) {

        public static Row of(Course course, long attended, long absent, long od) {
            long max = attended + absent + od;
            return new Row(course, max, attended, absent, od,
                    GradeUtil.percent(attended, max),
                    GradeUtil.percent(od, max),
                    GradeUtil.percent(attended + od, max));
        }

        public boolean low() {
            return max > 0 && totalPct < MIN_PERCENT;
        }
    }

    /** Hours added up over all of a student's courses, for the dashboard. */
    public record Totals(long max, long attended, long absent, long od, double totalPct) {

        public static Totals of(List<Row> rows) {
            long attended = rows.stream().mapToLong(Row::attended).sum();
            long absent = rows.stream().mapToLong(Row::absent).sum();
            long od = rows.stream().mapToLong(Row::od).sum();
            long max = attended + absent + od;
            return new Totals(max, attended, absent, od, GradeUtil.percent(attended + od, max));
        }

        public boolean low() {
            return max > 0 && totalPct < MIN_PERCENT;
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

    /** studentId -> status for a course on a day. Empty if attendance was not taken yet. */
    public Map<Long, AttendanceStatus> statusMap(Course course, LocalDate date) {
        Map<Long, AttendanceStatus> map = new HashMap<>();
        for (Attendance a : attendanceRepository.findByCourseAndDate(course, date)) {
            map.put(a.getStudent().getId(), a.getStatus());
        }
        return map;
    }

    /** Session length already saved for that day, or 1 if attendance was not taken yet. */
    public int hoursOf(Course course, LocalDate date) {
        return attendanceRepository.findByCourseAndDate(course, date).stream()
                .findFirst().map(Attendance::getHours).orElse(1);
    }

    /** Saves a row for every student of the course. Students missing from the map are marked present. */
    @Transactional
    public void save(Course course, LocalDate date, int hours, Map<Long, AttendanceStatus> statuses) {
        for (Student student : studentsOf(course)) {
            AttendanceStatus status = statuses.getOrDefault(student.getId(), AttendanceStatus.PRESENT);
            Attendance a = attendanceRepository.findByStudentAndCourseAndDate(student, course, date)
                    .orElse(new Attendance(student, course, date, hours, status));
            a.setHours(hours);
            a.setStatus(status);
            attendanceRepository.save(a);
        }
    }

    /** Attendance per course for one student. */
    public List<Row> summary(Student student) {
        List<Row> rows = new ArrayList<>();
        for (Course course : courseRepository.findByDepartmentAndYearOrderByCode(student.getDepartment(), student.getYear())) {
            rows.add(row(student, course));
        }
        return rows;
    }

    /** One student's attendance in one course. */
    public Row row(Student student, Course course) {
        return Row.of(course,
                attendanceRepository.sumHours(student, course, AttendanceStatus.PRESENT),
                attendanceRepository.sumHours(student, course, AttendanceStatus.ABSENT),
                attendanceRepository.sumHours(student, course, AttendanceStatus.OD));
    }

    /** First and last class date for a student, e.g. for "During the period X to Y". Empty if none. */
    public Optional<LocalDate[]> period(Student student) {
        Optional<LocalDate> first = attendanceRepository.firstDate(student);
        Optional<LocalDate> last = attendanceRepository.lastDate(student);
        if (first.isEmpty() || last.isEmpty()) return Optional.empty();
        return Optional.of(new LocalDate[]{first.get(), last.get()});
    }
}
