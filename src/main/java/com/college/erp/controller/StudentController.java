package com.college.erp.controller;

import com.college.erp.entity.Student;
import com.college.erp.service.AttendanceService;
import com.college.erp.service.CurrentUser;
import com.college.erp.service.MarkService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

/** Read-only pages for the logged-in student. A student only ever sees their own records. */
@Controller
@RequestMapping("/student")
public class StudentController {

    private final CurrentUser currentUser;
    private final AttendanceService attendanceService;
    private final MarkService markService;

    public StudentController(CurrentUser currentUser, AttendanceService attendanceService, MarkService markService) {
        this.currentUser = currentUser;
        this.attendanceService = attendanceService;
        this.markService = markService;
    }

    @GetMapping
    public String dashboard(Principal principal, Model model) {
        // The dashboard is just the student's ID card; totals are only for the low-attendance warning
        Student me = currentUser.student(principal);
        model.addAttribute("student", me);
        model.addAttribute("totals", AttendanceService.Totals.of(attendanceService.summary(me)));
        return "student/dashboard";
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        model.addAttribute("student", currentUser.student(principal));
        return "student/profile";
    }

    @GetMapping("/courses")
    public String courses(Principal principal, Model model) {
        List<MarkService.MarkRow> rows = markService.summary(currentUser.student(principal));
        model.addAttribute("rows", rows);
        model.addAttribute("credits", MarkService.totalCredits(rows));
        return "student/courses";
    }

    @GetMapping("/attendance")
    public String attendance(Principal principal, Model model) {
        Student me = currentUser.student(principal);
        List<AttendanceService.Row> rows = attendanceService.summary(me);
        model.addAttribute("rows", rows);
        model.addAttribute("totals", AttendanceService.Totals.of(rows));
        model.addAttribute("period", attendanceService.period(me).orElse(null));
        return "student/attendance";
    }

    @GetMapping("/marks")
    public String marks(Principal principal, Model model) {
        List<MarkService.MarkRow> rows = markService.summary(currentUser.student(principal));
        model.addAttribute("rows", rows);
        model.addAttribute("gpa", MarkService.gpa(rows));
        model.addAttribute("credits", MarkService.totalCredits(rows));
        return "student/marks";
    }
}
