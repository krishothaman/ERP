package com.college.erp.controller;

import com.college.erp.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** After login, send each user to their own dashboard. */
    @GetMapping("/")
    public String home(Authentication auth) {
        if (hasRole(auth, "ROLE_ADMIN")) return "redirect:/admin";
        if (hasRole(auth, "ROLE_FACULTY")) return "redirect:/faculty";
        return "redirect:/student";
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "error/403";
    }

    @GetMapping("/change-password")
    public String changePasswordForm() {
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth, Model model) {
        if (newPassword.length() < 4) {
            model.addAttribute("error", "New password must be at least 4 characters.");
        } else if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "New passwords do not match.");
        } else if (!userService.changePassword(auth.getName(), currentPassword, newPassword)) {
            model.addAttribute("error", "Current password is wrong.");
        } else {
            model.addAttribute("success", "Password changed successfully.");
        }
        return "change-password";
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
