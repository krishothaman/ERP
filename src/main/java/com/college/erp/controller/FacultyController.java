package com.college.erp.controller;

import com.college.erp.entity.AttendanceStatus;
import com.college.erp.entity.Course;
import com.college.erp.entity.Faculty;
import com.college.erp.entity.Student;
import com.college.erp.repository.CourseRepository;
import com.college.erp.service.AttendanceService;
import com.college.erp.service.CurrentUser;
import com.college.erp.service.DashboardService;
import com.college.erp.service.GradeUtil;
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

    /**
     * Marks arrive as ia1_{studentId}, ia2_{studentId} and sem_{studentId}. A blank box means "not entered";
     * students with none of the three fields in the request are left untouched.
     */
    @PostMapping("/marks")
    public String saveMarks(@RequestParam Long courseId, @RequestParam Map<String, String> params,
                            Principal principal, Model model, RedirectAttributes redirect) {
        Course course = myCourse(courseId, principal);
        List<Student> students = attendanceService.studentsOf(course);

        Map<Long, MarkService.Scores> toSave = new HashMap<>();
        List<String> errors = new ArrayList<>();
        for (Student s : students) {
            String id = s.getId().toString();
            if (!params.containsKey("ia1_" + id) && !params.containsKey("ia2_" + id) && !params.containsKey("sem_" + id)) {
                continue;
            }
            Integer ia1 = parseMark(params.get("ia1_" + id), GradeUtil.IA_MAX, "IA-1", s, errors);
            Integer ia2 = parseMark(params.get("ia2_" + id), GradeUtil.IA_MAX, "IA-2", s, errors);
            Integer sem = parseMark(params.get("sem_" + id), GradeUtil.SEM_MAX, "SEM", s, errors);
            toSave.put(s.getId(), new MarkService.Scores(ia1, ia2, sem));
        }

        if (!errors.isEmpty()) {
            model.addAttribute("course", course);
            model.addAttribute("students", students);
            model.addAttribute("marks", toSave);
            model.addAttribute("error", errors.get(0) + (errors.size() > 1 ? " (and " + (errors.size() - 1) + " more)" : ""));
            return "faculty/marks";
        }
        markService.save(course, toSave);
        redirect.addFlashAttribute("msg", "Marks saved.");
        return "redirect:/faculty/marks?courseId=" + courseId;
    }

    /** Blank -> null. Out-of-range or non-numeric values add an error and come back as null. */
    private Integer parseMark(String value, int max, String exam, Student s, List<String> errors) {
        if (value == null || value.isBlank()) return null;
        try {
            int mark = Integer.parseInt(value.trim());
            if (mark >= 0 && mark <= max) return mark;
            errors.add(exam + " marks for " + s.getName() + " must be between 0 and " + max + ".");
        } catch (NumberFormatException e) {
            errors.add(exam + " marks for " + s.getName() + " must be a whole number.");
        }
        return null;
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
