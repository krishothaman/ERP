package com.college.erp.entity;

import com.college.erp.service.GradeUtil;
import jakarta.persistence.*;

/**
 * A student's exam marks in one course: IA-1 and IA-2 (out of 50) and the semester exam (out of 100).
 * Any of them may still be blank. The total and grade are calculated, not stored.
 */
@Entity
@Table(name = "exam_marks", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
public class Mark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    private Integer ia1;

    private Integer ia2;

    private Integer sem;

    public Mark() {
    }

    public Mark(Student student, Course course, Integer ia1, Integer ia2, Integer sem) {
        this.student = student;
        this.course = course;
        this.ia1 = ia1;
        this.ia2 = ia2;
        this.sem = sem;
    }

    /** Final mark out of 100, or null until all three marks are entered. */
    public Integer getTotal() {
        if (ia1 == null || ia2 == null || sem == null) return null;
        return GradeUtil.total(ia1, ia2, sem);
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public Course getCourse() { return course; }
    public Integer getIa1() { return ia1; }
    public void setIa1(Integer ia1) { this.ia1 = ia1; }
    public Integer getIa2() { return ia2; }
    public void setIa2(Integer ia2) { this.ia2 = ia2; }
    public Integer getSem() { return sem; }
    public void setSem(Integer sem) { this.sem = sem; }
}
