package com.college.erp;

import com.college.erp.entity.Course;
import com.college.erp.entity.Faculty;
import com.college.erp.repository.CourseRepository;
import com.college.erp.repository.FacultyRepository;
import com.college.erp.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Rules found missing in the backend health check: password change, department case, reassignment. */
@SpringBootTest
@AutoConfigureMockMvc
class BackendRulesTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    StudentRepository students;
    @Autowired
    CourseRepository courses;
    @Autowired
    FacultyRepository faculty;

    @Test
    @WithUserDetails("ECE2003")
    void changePasswordValidatesAndThenWorks() throws Exception {
        mvc.perform(post("/change-password").with(csrf())
                        .param("currentPassword", "wrong").param("newPassword", "abcd").param("confirmPassword", "abcd"))
                .andExpect(content().string(containsString("Current password is wrong")));
        mvc.perform(post("/change-password").with(csrf())
                        .param("currentPassword", "ECE2003").param("newPassword", "abcd").param("confirmPassword", "abce"))
                .andExpect(content().string(containsString("do not match")));
        mvc.perform(post("/change-password").with(csrf())
                        .param("currentPassword", "ECE2003").param("newPassword", "ab").param("confirmPassword", "ab"))
                .andExpect(content().string(containsString("at least 4")));
        mvc.perform(post("/change-password").with(csrf())
                        .param("currentPassword", "ECE2003").param("newPassword", "newpass1").param("confirmPassword", "newpass1"))
                .andExpect(content().string(containsString("Password changed")));

        mvc.perform(formLogin().user("ECE2003").password("ECE2003")).andExpect(redirectedUrl("/login?error"));
        mvc.perform(formLogin().user("ECE2003").password("newpass1")).andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void lowercaseDepartmentIsNormalisedSoStudentSeesCourses() throws Exception {
        mvc.perform(post("/admin/students/save").with(csrf())
                        .param("rollNo", "LC100").param("name", "Lower Case").param("department", " cse ").param("year", "2"))
                .andExpect(redirectedUrl("/admin/students"));
        assertEquals("CSE", students.findAll().stream()
                .filter(s -> s.getRollNo().equals("LC100")).findFirst().orElseThrow().getDepartment());

        mvc.perform(get("/student/courses").with(user("LC100").roles("STUDENT")))
                .andExpect(content().string(containsString("CS201")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editingCourseReassignsFaculty() throws Exception {
        Course c = courses.findByCode("EC202").orElseThrow();
        Faculty meena = faculty.findAll().stream().filter(f -> f.getEmail().equals("meena@college.edu")).findFirst().orElseThrow();

        mvc.perform(post("/admin/courses/save").with(csrf())
                        .param("id", c.getId().toString()).param("code", "EC202").param("name", "Analog Circuits II")
                        .param("department", "ECE").param("year", "2").param("credits", "3")
                        .param("facultyId", meena.getId().toString()))
                .andExpect(redirectedUrl("/admin/courses"));

        Course updated = courses.findByCode("EC202").orElseThrow();
        assertEquals("Analog Circuits II", updated.getName());
        assertEquals(meena.getId(), updated.getFaculty().getId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletingFacultyUnassignsTheirCourses() throws Exception {
        mvc.perform(post("/admin/faculty/save").with(csrf())
                .param("name", "Temp Prof").param("department", "MECH").param("email", "temp@college.edu"));
        Faculty temp = faculty.findAll().stream().filter(f -> f.getEmail().equals("temp@college.edu")).findFirst().orElseThrow();
        mvc.perform(post("/admin/courses/save").with(csrf())
                .param("code", "ME201").param("name", "Thermodynamics").param("department", "MECH")
                .param("year", "2").param("credits", "3").param("facultyId", temp.getId().toString()));

        mvc.perform(post("/admin/faculty/" + temp.getId() + "/delete").with(csrf()))
                .andExpect(redirectedUrl("/admin/faculty"));

        assertTrue(courses.findByCode("ME201").isPresent());
        assertNull(courses.findByCode("ME201").orElseThrow().getFaculty());
        assertFalse(faculty.existsByEmail("temp@college.edu"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDashboardShowsLowAttendanceAndDepartments() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("lowAttendance", "deptLabels", "deptCounts"))
                .andExpect(content().string(containsString("CSE2006")));
    }
}
