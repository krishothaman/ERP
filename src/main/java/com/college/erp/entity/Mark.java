package com.college.erp.entity;

import jakarta.persistence.*;

/** A student's marks (out of 100) in one course. The grade is calculated, not stored. */
@Entity
@Table(name = "marks", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
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

    @Column(name = "score", nullable = false)
    private Integer marks;

    public Mark() {
    }

    public Mark(Student student, Course course, Integer marks) {
        this.student = student;
        this.course = course;
        this.marks = marks;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public Course getCourse() { return course; }
    public Integer getMarks() { return marks; }
    public void setMarks(Integer marks) { this.marks = marks; }
}
