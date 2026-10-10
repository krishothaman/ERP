package com.college.erp.controller;

import com.college.erp.entity.Course;
import com.college.erp.entity.Faculty;
import com.college.erp.entity.Student;
import com.college.erp.repository.CourseRepository;
import com.college.erp.repository.FacultyRepository;
import com.college.erp.repository.StudentRepository;
import com.college.erp.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final AdminService adminService;

    public AdminController(StudentRepository studentRepository, FacultyRepository facultyRepository,
                           CourseRepository courseRepository, AdminService adminService) {
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.courseRepository = courseRepository;
        this.adminService = adminService;
    }

    /** Never let a form submission set the linked login account or faculty object directly. */
    @InitBinder
    void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("user", "user.*", "faculty", "faculty.*");
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("studentCount", studentRepository.count());
        model.addAttribute("facultyCount", facultyRepository.count());
        model.addAttribute("courseCount", courseRepository.count());
        model.addAttribute("courses", courseRepository.findAllByOrderByCode());
        return "admin/dashboard";
    }

    // ---------- Students ----------

    @GetMapping("/students")
    public String students(@RequestParam(required = false) String q, Model model) {
        boolean searching = q != null && !q.isBlank();
        model.addAttribute("students", searching ? studentRepository.search(q.trim()) : studentRepository.findAllByOrderByRollNo());
        model.addAttribute("q", q);
        return "admin/students";
    }

    @GetMapping("/students/new")
    public String newStudent(Model model) {
        model.addAttribute("student", new Student());
        return "admin/student-form";
    }

    @GetMapping("/students/{id}/edit")
    public String editStudent(@PathVariable Long id, Model model) {
        model.addAttribute("student", studentRepository.findById(id).orElseThrow());
        return "admin/student-form";
    }

    @PostMapping("/students/save")
    public String saveStudent(@Valid @ModelAttribute("student") Student student, BindingResult result,
                              RedirectAttributes redirect) {
        boolean isNew = student.getId() == null;
        if (isNew && studentRepository.existsByRollNo(student.getRollNo())) {
            result.rejectValue("rollNo", "duplicate", "This roll number already exists");
        }
        if (result.hasErrors()) {
            return "admin/student-form";
        }
        if (isNew) {
            adminService.createStudent(student);
            redirect.addFlashAttribute("msg", "Student added. Login: " + student.getRollNo() + " / " + student.getRollNo());
        } else {
            adminService.updateStudent(student);
            redirect.addFlashAttribute("msg", "Student updated.");
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/students/{id}/delete")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirect) {
        adminService.deleteStudent(id);
        redirect.addFlashAttribute("msg", "Student deleted.");
        return "redirect:/admin/students";
    }

    // ---------- Faculty ----------

    @GetMapping("/faculty")
    public String faculty(Model model) {
        model.addAttribute("facultyList", facultyRepository.findAllByOrderByName());
        return "admin/faculty";
    }

    @GetMapping("/faculty/new")
    public String newFaculty(Model model) {
        model.addAttribute("faculty", new Faculty());
        return "admin/faculty-form";
    }

    @GetMapping("/faculty/{id}/edit")
    public String editFaculty(@PathVariable Long id, Model model) {
        model.addAttribute("faculty", facultyRepository.findById(id).orElseThrow());
        return "admin/faculty-form";
    }

    @PostMapping("/faculty/save")
    public String saveFaculty(@Valid @ModelAttribute("faculty") Faculty faculty, BindingResult result,
                              RedirectAttributes redirect) {
        boolean isNew = faculty.getId() == null;
        if (isNew && facultyRepository.existsByEmail(faculty.getEmail())) {
            result.rejectValue("email", "duplicate", "This email already exists");
        }
        if (result.hasErrors()) {
            return "admin/faculty-form";
        }
        if (isNew) {
            adminService.createFaculty(faculty);
            redirect.addFlashAttribute("msg", "Faculty added. Login: " + faculty.getEmail() + " / " + faculty.getEmail());
        } else {
            adminService.updateFaculty(faculty);
            redirect.addFlashAttribute("msg", "Faculty updated.");
        }
        return "redirect:/admin/faculty";
    }

    @PostMapping("/faculty/{id}/delete")
    public String deleteFaculty(@PathVariable Long id, RedirectAttributes redirect) {
        adminService.deleteFaculty(id);
        redirect.addFlashAttribute("msg", "Faculty deleted. Their courses are now unassigned.");
        return "redirect:/admin/faculty";
    }

    // ---------- Courses ----------

    @GetMapping("/courses")
    public String courses(Model model) {
        model.addAttribute("courses", courseRepository.findAllByOrderByCode());
        return "admin/courses";
    }

    @GetMapping("/courses/new")
    public String newCourse(Model model) {
        model.addAttribute("course", new Course());
        model.addAttribute("facultyList", facultyRepository.findAllByOrderByName());
        return "admin/course-form";
    }

    @GetMapping("/courses/{id}/edit")
    public String editCourse(@PathVariable Long id, Model model) {
        Course course = courseRepository.findById(id).orElseThrow();
        model.addAttribute("course", course);
        model.addAttribute("facultyList", facultyRepository.findAllByOrderByName());
        model.addAttribute("selectedFacultyId", course.getFaculty() == null ? null : course.getFaculty().getId());
        return "admin/course-form";
    }

    @PostMapping("/courses/save")
    public String saveCourse(@Valid @ModelAttribute("course") Course course, BindingResult result,
                             @RequestParam(required = false) Long facultyId,
                             Model model, RedirectAttributes redirect) {
        boolean isNew = course.getId() == null;
        if (isNew && courseRepository.existsByCode(course.getCode())) {
            result.rejectValue("code", "duplicate", "This course code already exists");
        }
        if (result.hasErrors()) {
            model.addAttribute("facultyList", facultyRepository.findAllByOrderByName());
            model.addAttribute("selectedFacultyId", facultyId);
            return "admin/course-form";
        }
        adminService.saveCourse(course, facultyId);
        redirect.addFlashAttribute("msg", isNew ? "Course added." : "Course updated.");
        return "redirect:/admin/courses";
    }

    @PostMapping("/courses/{id}/delete")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirect) {
        adminService.deleteCourse(id);
        redirect.addFlashAttribute("msg", "Course deleted.");
        return "redirect:/admin/courses";
    }
}
