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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StudentViewTest {

    @Autowired
    MockMvc mvc;

    @Test
    @WithUserDetails("CSE2001")
    void dashboardShowsOwnSummaryAndCharts() throws Exception {
        mvc.perform(get("/student"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aarav Patel")))
                .andExpect(content().string(not(containsString("Sneha Reddy"))))
                .andExpect(model().attributeExists("totals", "gpa", "credits"))
                .andExpect(content().string(containsString("attendanceChart")));
    }

    @Test
    @WithUserDetails("CSE2001")
    void profileShowsOwnDetails() throws Exception {
        mvc.perform(get("/student/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CSE2001")))
                .andExpect(content().string(containsString("aarav@student.college.edu")));
    }

    @Test
    @WithUserDetails("CSE2001")
    void courseListShowsCoursesFacultyAndCredits() throws Exception {
        mvc.perform(get("/student/courses"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CS201")))
                .andExpect(content().string(containsString("Dr. Ravi Kumar")))
                .andExpect(content().string(not(containsString("EC201"))));
    }

    @Test
    @WithUserDetails("CSE2001")
    void attendanceShowsHoursColumnsAndPeriod() throws Exception {
        mvc.perform(get("/student/attendance"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CS201")))
                .andExpect(content().string(containsString("Max. hours")))
                .andExpect(content().string(containsString("OD/ML")))
                .andExpect(content().string(containsString("During the period")));
    }

    @Test
    @WithUserDetails("CSE2001")
    void marksShowGradePointsAndGpa() throws Exception {
        mvc.perform(get("/student/marks"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CS201")))
                .andExpect(content().string(containsString("GPA")))
                .andExpect(content().string(containsString("IA-1")))
                .andExpect(content().string(containsString("SEM")))
                .andExpect(content().string(containsString("Not graded yet")));
    }

    @Test
    @WithUserDetails("CSE2006")
    void lowAttendanceIsFlagged() throws Exception {
        mvc.perform(get("/student/attendance"))
                .andExpect(content().string(containsString("Below 75%")));
    }
}
