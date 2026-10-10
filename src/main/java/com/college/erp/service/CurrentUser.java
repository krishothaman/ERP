package com.college.erp.service;

import com.college.erp.entity.Faculty;
import com.college.erp.entity.Student;
import com.college.erp.entity.User;
import com.college.erp.repository.FacultyRepository;
import com.college.erp.repository.StudentRepository;
import com.college.erp.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.security.Principal;

/** Finds the Student or Faculty record belonging to whoever is logged in. */
@Component
public class CurrentUser {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;

    public CurrentUser(UserRepository userRepository, StudentRepository studentRepository,
                       FacultyRepository facultyRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
    }

    public Student student(Principal principal) {
        return studentRepository.findByUser(user(principal)).orElseThrow();
    }

    public Faculty faculty(Principal principal) {
        return facultyRepository.findByUser(user(principal)).orElseThrow();
    }

    private User user(Principal principal) {
        return userRepository.findByUsername(principal.getName()).orElseThrow();
    }
}
