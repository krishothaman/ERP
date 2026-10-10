package com.college.erp.controller;

import com.college.erp.entity.Student;
import com.college.erp.service.AttendanceService;
import com.college.erp.service.CurrentUser;
import com.college.erp.service.GradeUtil;
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
    public String profile(Principal principal, Model model) {
        Student me = currentUser.student(principal);
        List<AttendanceService.Row> attendance = attendanceService.summary(me);
        long present = attendance.stream().mapToLong(AttendanceService.Row::present).sum();
        long total = attendance.stream().mapToLong(AttendanceService.Row::total).sum();

        model.addAttribute("student", me);
        model.addAttribute("courseCount", attendance.size());
        model.addAttribute("overallAttendance", GradeUtil.percent(present, total));
        model.addAttribute("hasClasses", total > 0);
        return "student/dashboard";
    }

    @GetMapping("/attendance")
    public String attendance(Principal principal, Model model) {
        model.addAttribute("rows", attendanceService.summary(currentUser.student(principal)));
        return "student/attendance";
    }

    @GetMapping("/marks")
    public String marks(Principal principal, Model model) {
        model.addAttribute("rows", markService.summary(currentUser.student(principal)));
        return "student/marks";
    }
}
