package com.college.erp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/** A subject. Every student of the same department and year takes it. */
@Entity
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Course code is required")
    @Column(nullable = false, unique = true)
    private String code;

    @NotBlank(message = "Course name is required")
    private String name;

    @NotBlank(message = "Department is required")
    private String department;

    @NotNull(message = "Year is required")
    @Min(value = 1, message = "Year must be 1 to 4")
    @Max(value = 4, message = "Year must be 1 to 4")
    @Column(name = "study_year")
    private Integer year;

    @NotNull(message = "Credits are required")
    @Min(value = 1, message = "Credits must be 1 to 10")
    @Max(value = 10, message = "Credits must be 1 to 10")
    private Integer credits;

    /** The faculty member who teaches this course (may be unassigned). */
    @ManyToOne
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department == null ? null : department.trim().toUpperCase(); }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }
    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
}
