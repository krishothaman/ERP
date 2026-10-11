package com.college.erp.service;

import com.college.erp.entity.Course;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradeUtilTest {

    @Test
    void grades() {
        assertEquals("O", GradeUtil.grade(95));
        assertEquals("O", GradeUtil.grade(90));
        assertEquals("A+", GradeUtil.grade(80));
        assertEquals("A", GradeUtil.grade(79));
        assertEquals("B+", GradeUtil.grade(60));
        assertEquals("B", GradeUtil.grade(50));
        assertEquals("C", GradeUtil.grade(40));
        assertEquals("F", GradeUtil.grade(39));
    }

    @Test
    void percent() {
        assertEquals(0.0, GradeUtil.percent(0, 0));
        assertEquals(75.0, GradeUtil.percent(3, 4));
        assertEquals(66.67, GradeUtil.percent(2, 3));
        assertEquals(83.64, GradeUtil.percent(46, 55));
        assertEquals(100.0, GradeUtil.percent(5, 5));
    }

    @Test
    void gradePoints() {
        assertEquals(10, GradeUtil.gradePoint("O"));
        assertEquals(9, GradeUtil.gradePoint("A+"));
        assertEquals(8, GradeUtil.gradePoint("A"));
        assertEquals(7, GradeUtil.gradePoint("B+"));
        assertEquals(6, GradeUtil.gradePoint("B"));
        assertEquals(5, GradeUtil.gradePoint("C"));
        assertEquals(0, GradeUtil.gradePoint("F"));
    }

    @Test
    void gpaIsCreditWeightedAndSkipsUngradedCourses() {
        // 4 credits of O (10) + 2 credits of B (6) = (40 + 12) / 6 = 8.67; ungraded course ignored
        List<MarkService.MarkRow> rows = List.of(
                MarkService.MarkRow.of(course(4), 95),
                MarkService.MarkRow.of(course(2), 55),
                MarkService.MarkRow.of(course(3), null));
        assertEquals(8.67, MarkService.gpa(rows));
        assertEquals(0.0, MarkService.gpa(List.of(MarkService.MarkRow.of(course(3), null))));
    }

    @Test
    void odCountsTowardTotalButNotAverage() {
        // 10 hours: 6 present, 2 absent, 2 OD
        AttendanceService.Row row = AttendanceService.Row.of(course(3), 6, 2, 2);
        assertEquals(10, row.max());
        assertEquals(60.0, row.averagePct());
        assertEquals(20.0, row.odPct());
        assertEquals(80.0, row.totalPct());
        assertFalse(row.low());

        assertTrue(AttendanceService.Row.of(course(3), 6, 4, 0).low());
        assertFalse(AttendanceService.Row.of(course(3), 0, 0, 0).low());
    }

    private Course course(int credits) {
        Course c = new Course();
        c.setCredits(credits);
        return c;
    }
}
