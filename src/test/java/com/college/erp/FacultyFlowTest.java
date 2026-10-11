package com.college.erp;

import com.college.erp.entity.Attendance;
import com.college.erp.entity.AttendanceStatus;
import com.college.erp.entity.Course;
import com.college.erp.entity.Mark;
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
import java.util.List;

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
    void savingAttendanceRecordsHoursAndStatuses() throws Exception {
        Course c = courses.findByCode("CS201").orElseThrow();
        List<Student> cls = students.findByDepartmentAndYearOrderByRollNo("CSE", 2);
        Student first = cls.get(0), second = cls.get(1), third = cls.get(2);
        LocalDate day = LocalDate.of(2030, 1, 1);

        mvc.perform(post("/faculty/attendance").with(csrf())
                        .param("courseId", c.getId().toString()).param("date", day.toString()).param("hours", "2")
                        .param("s_" + first.getId(), "PRESENT")
                        .param("s_" + second.getId(), "ABSENT")
                        .param("s_" + third.getId(), "OD"))
                .andExpect(status().is3xxRedirection());

        Attendance a1 = attendance.findByStudentAndCourseAndDate(first, c, day).orElseThrow();
        assertEquals(AttendanceStatus.PRESENT, a1.getStatus());
        assertEquals(2, a1.getHours());
        assertEquals(AttendanceStatus.ABSENT, attendance.findByStudentAndCourseAndDate(second, c, day).orElseThrow().getStatus());
        assertEquals(AttendanceStatus.OD, attendance.findByStudentAndCourseAndDate(third, c, day).orElseThrow().getStatus());
        // students not sent default to present
        assertEquals(AttendanceStatus.PRESENT, attendance.findByStudentAndCourseAndDate(cls.get(3), c, day).orElseThrow().getStatus());

        // Saving the same day again updates instead of duplicating
        long count = attendance.count();
        mvc.perform(post("/faculty/attendance").with(csrf())
                .param("courseId", c.getId().toString()).param("date", day.toString()).param("hours", "1")
                .param("s_" + second.getId(), "PRESENT"));
        assertEquals(count, attendance.count());
        Attendance updated = attendance.findByStudentAndCourseAndDate(second, c, day).orElseThrow();
        assertEquals(AttendanceStatus.PRESENT, updated.getStatus());
        assertEquals(1, updated.getHours());
    }

    @Test
    @WithUserDetails("ravi@college.edu")
    void invalidHoursOrStatusIsRejected() throws Exception {
        Course c = courses.findByCode("CS201").orElseThrow();
        Student first = students.findByDepartmentAndYearOrderByRollNo("CSE", 2).get(0);
        mvc.perform(post("/faculty/attendance").with(csrf())
                        .param("courseId", c.getId().toString()).param("date", "2030-02-01").param("hours", "5"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/faculty/attendance").with(csrf())
                        .param("courseId", c.getId().toString()).param("date", "2030-02-01")
                        .param("s_" + first.getId(), "SLEEPING"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("ravi@college.edu")
    void savingMarksUpsertsAndRejectsOutOfRange() throws Exception {
        Course c = courses.findByCode("CS203").orElseThrow();
        Student first = students.findByDepartmentAndYearOrderByRollNo("CSE", 2).get(0);
        String id = first.getId().toString();

        // IA-1 only, the rest still blank
        mvc.perform(post("/faculty/marks").with(csrf())
                        .param("courseId", c.getId().toString()).param("ia1_" + id, "40"))
                .andExpect(status().is3xxRedirection());
        Mark m = marks.findByStudentAndCourse(first, c).orElseThrow();
        assertEquals(40, m.getIa1());
        assertNull(m.getTotal());

        mvc.perform(post("/faculty/marks").with(csrf())
                        .param("courseId", c.getId().toString())
                        .param("ia1_" + id, "40").param("ia2_" + id, "45").param("sem_" + id, "80"))
                .andExpect(status().is3xxRedirection());
        assertEquals(82, marks.findByStudentAndCourse(first, c).orElseThrow().getTotal());

        mvc.perform(post("/faculty/marks").with(csrf())
                        .param("courseId", c.getId().toString())
                        .param("ia1_" + id, "60").param("ia2_" + id, "45").param("sem_" + id, "80"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("between 0 and 50")));
        mvc.perform(post("/faculty/marks").with(csrf())
                        .param("courseId", c.getId().toString())
                        .param("ia1_" + id, "40").param("ia2_" + id, "45").param("sem_" + id, "101"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("between 0 and 100")));
        assertEquals(82, marks.findByStudentAndCourse(first, c).orElseThrow().getTotal());
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
