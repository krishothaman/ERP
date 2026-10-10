package com.college.erp.service;

/** Grade and percentage calculations used by the attendance and marks screens. */
public final class GradeUtil {

    private GradeUtil() {
    }

    public static String grade(int marks) {
        if (marks >= 90) return "O";
        if (marks >= 80) return "A+";
        if (marks >= 70) return "A";
        if (marks >= 60) return "B+";
        if (marks >= 50) return "B";
        if (marks >= 40) return "C";
        return "F";
    }

    /** 10-point scale used for the GPA. */
    public static int gradePoint(String grade) {
        return switch (grade) {
            case "O" -> 10;
            case "A+" -> 9;
            case "A" -> 8;
            case "B+" -> 7;
            case "B" -> 6;
            case "C" -> 5;
            default -> 0;
        };
    }

    /** Percentage rounded to one decimal place; 0 when there are no classes yet. */
    public static double percent(long part, long total) {
        if (total == 0) return 0.0;
        return Math.round(part * 1000.0 / total) / 10.0;
    }
}
