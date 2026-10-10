package com.college.erp.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(66.7, GradeUtil.percent(2, 3));
        assertEquals(100.0, GradeUtil.percent(5, 5));
    }
}
