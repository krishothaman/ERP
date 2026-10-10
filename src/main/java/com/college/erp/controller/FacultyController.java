package com.college.erp.controller;

import com.college.erp.entity.AttendanceStatus;
import com.college.erp.entity.Course;
import com.college.erp.entity.Faculty;
import com.college.erp.entity.Student;
import com.college.erp.repository.CourseRepository;
import com.college.erp.service.AttendanceService;
import com.college.erp.service.CurrentUser;
import com.college.erp.service.DashboardService;
import com.college.erp.service.MarkService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/faculty")
public class FacultyController {

    private final CourseRepository courseRepository;
    private final AttendanceService attendanceService;
    private final MarkService markService;
    private final CurrentUser currentUser;
    private final DashboardService dashboardService;

    public FacultyController(CourseRepository courseRepository, AttendanceService attendanceService,
                             MarkService markService, CurrentUser currentUser, DashboardService dashboardService) {
        this.courseRepository = courseRepository;
        this.attendanceService = attendanceService;
        this.markService = markService;
        this.currentUser = currentUser;
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public String dashboard(Principal principal, Model model) {
        Faculty me = currentUser.faculty(principal);
        List<DashboardService.CourseStats> stats = courseRepository.findByFacultyOrderByCode(me).stream()
                .map(dashboardService::courseStats).toList();
        model.addAttribute("faculty", me);
        model.addAttribute("stats", stats);
        model.addAttribute("totalStudents", stats.stream().mapToInt(DashboardService.CourseStats::students).sum());
        model.addAttribute("totalLow", stats.stream().mapToLong(DashboardService.CourseStats::lowCount).sum());
        return "faculty/dashboard";
    }

    // ---------- Attendance ----------

    @GetMapping("/attendance")
    public String attendanceForm(@RequestParam Long courseId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 Principal principal, Model model) {
        Course course = myCourse(courseId, principal);
        LocalDate day = date != null ? date : LocalDate.now();
        Map<Long, AttendanceStatus> statuses = attendanceService.statusMap(course, day);

        model.addAttribute("course", course);
        model.addAttribute("date", day);
        model.addAttribute("hours", attendanceService.hoursOf(course, day));
        model.addAttribute("students", attendanceService.studentsOf(course));
        model.addAttribute("statuses", statuses);
        model.addAttribute("alreadyTaken", !statuses.isEmpty());
        return "faculty/attendance";
    }

    /** Statuses arrive as s_{studentId}=PRESENT|ABSENT|OD; hours is the session length (1-3). */
    @PostMapping("/attendance")
    public String saveAttendance(@RequestParam Long courseId,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 @RequestParam(defaultValue = "1") int hours,
                                 @RequestParam Map<String, String> params,
                                 Principal principal, RedirectAttributes redirect) {
        Course course = myCourse(courseId, principal);
        if (hours < 1 || hours > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hours must be 1 to 3");
        }
        Map<Long, AttendanceStatus> statuses = new HashMap<>();
        for (Student s : attendanceService.studentsOf(course)) {
            String value = params.get("s_" + s.getId());
            if (value == null) continue;
            try {
                statuses.put(s.getId(), AttendanceStatus.valueOf(value));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status " + value);
            }
        }
        attendanceService.save(course, date, hours, statuses);
        redirect.addFlashAttribute("msg", "Attendance saved for " + date + " (" + hours + " hour" + (hours > 1 ? "s" : "") + ").");
        return "redirect:/faculty/attendance?courseId=" + courseId + "&date=" + date;
    }

    // ---------- Marks ----------

    @GetMapping("/marks")
    public String marksForm(@RequestParam Long courseId, Principal principal, Model model) {
        Course course = myCourse(courseId, principal);
        model.addAttribute("course", course);
        model.addAttribute("students", attendanceService.studentsOf(course));
        model.addAttribute("marks", markService.marksMap(course));
        return "faculty/marks";
    }

    /** Marks arrive as m_{studentId}=value. Blank boxes are skipped. */
    @PostMapping("/marks")
    public String saveMarks(@RequestParam Long courseId, @RequestParam Map<String, String> params,
                            Principal principal, Model model, RedirectAttributes redirect) {
        Course course = myCourse(courseId, principal);
        List<Student> students = attendanceService.studentsOf(course);

        Map<Long, Integer> toSave = new HashMap<>();
        Map<Long, Integer> entered = new HashMap<>();
        String error = null;
        for (Student s : students) {
            String value = params.get("m_" + s.getId());
            if (value == null || value.isBlank()) continue;
            try {
                int marks = Integer.parseInt(value.trim());
                entered.put(s.getId(), marks);
                if (marks < 0 || marks > 100) {
                    error = "Marks for " + s.getName() + " must be between 0 and 100.";
                } else {
                    toSave.put(s.getId(), marks);
                }
            } catch (NumberFormatException e) {
                error = "Marks for " + s.getName() + " must be a whole number.";
            }
        }

        if (error != null) {
            model.addAttribute("course", course);
            model.addAttribute("students", students);
            model.addAttribute("marks", entered);
            model.addAttribute("error", error);
            return "faculty/marks";
        }
        markService.save(course, toSave);
        redirect.addFlashAttribute("msg", "Marks saved.");
        return "redirect:/faculty/marks?courseId=" + courseId;
    }

    /** Loads a course and makes sure it belongs to the logged-in faculty member. */
    private Course myCourse(Long courseId, Principal principal) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Faculty me = currentUser.faculty(principal);
        if (course.getFaculty() == null || !course.getFaculty().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your course");
        }
        return course;
    }
}
