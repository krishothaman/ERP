package com.college.erp.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

/** One student's attendance for one course on one day. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id", "class_date"}))
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "class_date", nullable = false)
    private LocalDate date;

    private boolean present;

    public Attendance() {
    }

    public Attendance(Student student, Course course, LocalDate date, boolean present) {
        this.student = student;
        this.course = course;
        this.date = date;
        this.present = present;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public Course getCourse() { return course; }
    public LocalDate getDate() { return date; }
    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }
}
