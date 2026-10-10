package com.college.erp.service;

import com.college.erp.entity.Course;
import com.college.erp.entity.Student;
import com.college.erp.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/** Numbers shown on the admin and faculty dashboards. */
@Service
public class DashboardService {

    /** A student whose overall attendance is below the minimum. */
    public record LowAttendance(Student student, double totalPct) {
    }

    /** Class-level attendance figures for one course. */
    public record CourseStats(Course course, int students, double averageTotalPct, long lowCount) {
    }

    private final StudentRepository studentRepository;
    private final AttendanceService attendanceService;

    public DashboardService(StudentRepository studentRepository, AttendanceService attendanceService) {
        this.studentRepository = studentRepository;
        this.attendanceService = attendanceService;
    }

    /** Department -> number of students, sorted by department name. */
    public Map<String, Long> studentsPerDepartment() {
        Map<String, Long> counts = new TreeMap<>();
        for (Student s : studentRepository.findAll()) {
            counts.merge(s.getDepartment(), 1L, Long::sum);
        }
        return counts;
    }

    /** Students below 75% total attendance across their courses, lowest first. */
    public List<LowAttendance> lowAttendance() {
        List<LowAttendance> list = new ArrayList<>();
        for (Student s : studentRepository.findAllByOrderByRollNo()) {
            AttendanceService.Totals totals = AttendanceService.Totals.of(attendanceService.summary(s));
            if (totals.low()) {
                list.add(new LowAttendance(s, totals.totalPct()));
            }
        }
        list.sort(Comparator.comparingDouble(LowAttendance::totalPct));
        return list;
    }

    public CourseStats courseStats(Course course) {
        List<Student> students = attendanceService.studentsOf(course);
        List<AttendanceService.Row> rows = students.stream().map(s -> attendanceService.row(s, course)).toList();
        double average = rows.stream().filter(r -> r.max() > 0).mapToDouble(AttendanceService.Row::totalPct)
                .average().orElse(0.0);
        long low = rows.stream().filter(AttendanceService.Row::low).count();
        return new CourseStats(course, students.size(), Math.round(average * 10) / 10.0, low);
    }
}
