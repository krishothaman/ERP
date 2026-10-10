package com.college.erp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StudentViewTest {

    @Autowired
    MockMvc mvc;

    @Test
    @WithUserDetails("CSE2001")
    void profileShowsOwnDetails() throws Exception {
        mvc.perform(get("/student"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aarav Patel")))
                .andExpect(content().string(not(containsString("Sneha Reddy"))));
    }

    @Test
    @WithUserDetails("CSE2001")
    void attendanceListsCoursesWithPercent() throws Exception {
        mvc.perform(get("/student/attendance"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CS201")))
                .andExpect(content().string(containsString("%")));
    }

    @Test
    @WithUserDetails("CSE2001")
    void marksShowGrade() throws Exception {
        // Seeded: CSE2001 has 92 in CS201 -> grade O
        mvc.perform(get("/student/marks"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CS201")))
                .andExpect(content().string(containsString("92")));
    }

    @Test
    @WithUserDetails("CSE2006")
    void lowAttendanceIsFlagged() throws Exception {
        // Seeded: CSE2006 attends only half the classes
        mvc.perform(get("/student/attendance"))
                .andExpect(content().string(containsString("Below 75%")));
    }
}
