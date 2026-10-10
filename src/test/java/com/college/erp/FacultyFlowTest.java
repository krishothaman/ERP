package com.college.erp;

import com.college.erp.entity.Course;
import com.college.erp.entity.Student;
import com.college.erp.repository.AttendanceRepository;
import com.college.erp.repository.CourseRepository;
import com.college.erp.repository.MarkRepository;
import com.college.erp.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FacultyFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    CourseRepository courses;
    @Autowired
    StudentRepository students;
    @Autowired
    AttendanceRepository attendance;
    @Autowired
    MarkRepository marks;

    @Test
    @WithUserDetails("ravi@college.edu")
    void dashboardShowsOnlyOwnCourses() throws Exception {
        mvc.perform(get("/faculty"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CS201")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("CS202"))));
    }

    @Test
    @WithUserDetails("ravi@college.edu")
    void savingAttendanceRecordsPresentAndAbsent() throws Exception {
        Course c = courses.findByCode("CS201").orElseThrow();
        Student first = students.findByDepartmentAndYearOrderByRollNo("CSE", 2).get(0);
        Student second = students.findByDepartmentAndYearOrderByRollNo("CSE", 2).get(1);
        LocalDate day = LocalDate.of(2030, 1, 1);

        mvc.perform(post("/faculty/attendance").with(csrf())
                        .param("courseId", c.getId().toString()).param("date", day.toString())
                        .param("present", first.getId().toString()))
                .andExpect(status().is3xxRedirection());

        assertTrue(attendance.findByStudentAndCourseAndDate(first, c, day).orElseThrow().isPresent());
        assertFalse(attendance.findByStudentAndCourseAndDate(second, c, day).orElseThrow().isPresent());

        // Saving the same day again updates instead of duplicating
        long count = attendance.count();
        mvc.perform(post("/faculty/attendance").with(csrf())
                .param("courseId", c.getId().toString()).param("date", day.toString())
                .param("present", second.getId().toString()));
        assertEquals(count, attendance.count());
        assertTrue(attendance.findByStudentAndCourseAndDate(second, c, day).orElseThrow().isPresent());
    }

    @Test
    @WithUserDetails("ravi@college.edu")
    void savingMarksUpsertsAndRejectsOutOfRange() throws Exception {
        Course c = courses.findByCode("CS203").orElseThrow();
        Student first = students.findByDepartmentAndYearOrderByRollNo("CSE", 2).get(0);

        mvc.perform(post("/faculty/marks").with(csrf())
                        .param("courseId", c.getId().toString()).param("m_" + first.getId(), "77"))
                .andExpect(status().is3xxRedirection());
        assertEquals(77, marks.findByStudentAndCourse(first, c).orElseThrow().getMarks());

        mvc.perform(post("/faculty/marks").with(csrf())
                        .param("courseId", c.getId().toString()).param("m_" + first.getId(), "150"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("between 0 and 100")));
        assertEquals(77, marks.findByStudentAndCourse(first, c).orElseThrow().getMarks());
    }

    @Test
    @WithUserDetails("meena@college.edu")
    void facultyCannotOpenAnotherFacultysCourse() throws Exception {
        Course c = courses.findByCode("CS201").orElseThrow();
        mvc.perform(get("/faculty/marks").param("courseId", c.getId().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/faculty/attendance").param("courseId", c.getId().toString()))
                .andExpect(status().isForbidden());
    }
}
