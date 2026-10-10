package com.college.erp;

import com.college.erp.repository.CourseRepository;
import com.college.erp.repository.StudentRepository;
import com.college.erp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class AdminControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    StudentRepository students;
    @Autowired
    UserRepository users;
    @Autowired
    CourseRepository courses;

    @Test
    void dashboardAndListsLoad() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk());
        mvc.perform(get("/admin/students")).andExpect(status().isOk())
                .andExpect(content().string(containsString("CSE2001")));
        mvc.perform(get("/admin/faculty")).andExpect(status().isOk())
                .andExpect(content().string(containsString("ravi@college.edu")));
        mvc.perform(get("/admin/courses")).andExpect(status().isOk())
                .andExpect(content().string(containsString("CS201")));
    }

    @Test
    void searchFiltersStudents() throws Exception {
        mvc.perform(get("/admin/students").param("q", "Sneha"))
                .andExpect(content().string(containsString("Sneha Reddy")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Aarav Patel"))));
    }

    @Test
    void addingStudentCreatesTheirLogin() throws Exception {
        mvc.perform(post("/admin/students/save").with(csrf())
                        .param("rollNo", "T999").param("name", "Test Student").param("department", "CSE")
                        .param("year", "1").param("email", "t@x.com").param("phone", "1"))
                .andExpect(redirectedUrl("/admin/students"));
        assertTrue(students.existsByRollNo("T999"));
        assertTrue(users.existsByUsername("T999"));
    }

    @Test
    void duplicateRollNoShowsFormError() throws Exception {
        mvc.perform(post("/admin/students/save").with(csrf())
                        .param("rollNo", "CSE2001").param("name", "Dup").param("department", "CSE").param("year", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("student", "rollNo"));
    }

    @Test
    void invalidYearShowsFormError() throws Exception {
        mvc.perform(post("/admin/students/save").with(csrf())
                        .param("rollNo", "T500").param("name", "Bad Year").param("department", "CSE").param("year", "7"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("student", "year"));
    }

    @Test
    void deletingStudentRemovesLoginToo() throws Exception {
        mvc.perform(post("/admin/students/save").with(csrf())
                .param("rollNo", "DEL1").param("name", "To Delete").param("department", "CSE").param("year", "2"));
        Long id = students.findAll().stream().filter(s -> s.getRollNo().equals("DEL1")).findFirst().orElseThrow().getId();

        mvc.perform(post("/admin/students/" + id + "/delete").with(csrf()))
                .andExpect(redirectedUrl("/admin/students"));
        assertFalse(students.existsByRollNo("DEL1"));
        assertFalse(users.existsByUsername("DEL1"));
    }

    @Test
    void addingCourseWithDuplicateCodeShowsError() throws Exception {
        mvc.perform(post("/admin/courses/save").with(csrf())
                        .param("code", "CS201").param("name", "Dup").param("department", "CSE")
                        .param("year", "2").param("credits", "3"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("course", "code"));
    }

    @Test
    void addingFacultyCreatesTheirLogin() throws Exception {
        mvc.perform(post("/admin/faculty/save").with(csrf())
                        .param("name", "New Prof").param("department", "ECE").param("email", "newprof@college.edu"))
                .andExpect(redirectedUrl("/admin/faculty"));
        assertTrue(users.existsByUsername("newprof@college.edu"));
    }
}
