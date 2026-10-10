package com.college.erp.controller;

import com.college.erp.entity.User;
import com.college.erp.repository.FacultyRepository;
import com.college.erp.repository.StudentRepository;
import com.college.erp.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.util.Optional;

/**
 * Values every page needs: the portal name, the current URL (to highlight the active
 * sidebar link) and the logged-in person's display name for the top bar.
 */
@ControllerAdvice
public class GlobalModel {

    @Value("${app.name}")
    private String appName;

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;

    public GlobalModel(UserRepository userRepository, StudentRepository studentRepository,
                       FacultyRepository facultyRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
    }

    @ModelAttribute("appName")
    public String appName() {
        return appName;
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("displayName")
    public String displayName(Principal principal) {
        if (principal == null) return null;
        Optional<User> user = userRepository.findByUsername(principal.getName());
        if (user.isEmpty()) return principal.getName();
        return switch (user.get().getRole()) {
            case STUDENT -> studentRepository.findByUser(user.get()).map(s -> s.getName()).orElse(principal.getName());
            case FACULTY -> facultyRepository.findByUser(user.get()).map(f -> f.getName()).orElse(principal.getName());
            case ADMIN -> "Administrator";
        };
    }
}
