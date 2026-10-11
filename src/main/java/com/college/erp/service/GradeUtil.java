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

    /** IA-1 and IA-2 are out of 50 each; the semester exam is out of 100. */
    public static final int IA_MAX = 50;
    public static final int SEM_MAX = 100;

    /** IA-1 + IA-2 (out of 100) scaled to 40, rounded. */
    public static int internal(int ia1, int ia2) {
        return (4 * (ia1 + ia2) + 5) / 10;
    }

    /** Final mark out of 100: internals count 40%, the semester exam 60%. */
    public static int total(int ia1, int ia2, int sem) {
        return (4 * (ia1 + ia2) + 6 * sem + 5) / 10;
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

    /** Percentage rounded to two decimal places; 0 when there are no classes yet. */
    public static double percent(long part, long total) {
        if (total == 0) return 0.0;
        return Math.round(part * 10000.0 / total) / 100.0;
    }
}
