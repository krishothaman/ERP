package com.college.erp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired
    MockMvc mvc;

    @Test
    void anonymousIsSentToLogin() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentIsBlockedFromAdminPages() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FACULTY")
    void facultyIsBlockedFromStudentPages() throws Exception {
        mvc.perform(get("/student")).andExpect(status().isForbidden());
    }

    @Test
    void seededAdminCanLogIn() throws Exception {
        mvc.perform(formLogin().user("admin").password("admin123"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(formLogin().user("admin").password("nope"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rootSendsAdminToAdminDashboard() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/admin"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void rootSendsStudentToStudentDashboard() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/student"));
    }
}
