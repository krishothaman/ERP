package com.college.erp.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

/** One student's attendance for one class session (of 1-3 hours) of a course on a day. */
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

    /** Length of the class session in hours. */
    @Column(nullable = false)
    private int hours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    public Attendance() {
    }

    public Attendance(Student student, Course course, LocalDate date, int hours, AttendanceStatus status) {
        this.student = student;
        this.course = course;
        this.date = date;
        this.hours = hours;
        this.status = status;
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public Course getCourse() { return course; }
    public LocalDate getDate() { return date; }
    public int getHours() { return hours; }
    public void setHours(int hours) { this.hours = hours; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
}
