package com.college.erp.repository;

import com.college.erp.entity.Attendance;
import com.college.erp.entity.AttendanceStatus;
import com.college.erp.entity.Course;
import com.college.erp.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByCourseAndDate(Course course, LocalDate date);

    Optional<Attendance> findByStudentAndCourseAndDate(Student student, Course course, LocalDate date);

    /** Hours a student spent in a course with the given status (PRESENT, ABSENT or OD). */
    @Query("SELECT COALESCE(SUM(a.hours), 0) FROM Attendance a "
            + "WHERE a.student = :student AND a.course = :course AND a.status = :status")
    long sumHours(@Param("student") Student student, @Param("course") Course course,
                  @Param("status") AttendanceStatus status);

    @Query("SELECT MIN(a.date) FROM Attendance a WHERE a.student = :student")
    Optional<LocalDate> firstDate(@Param("student") Student student);

    @Query("SELECT MAX(a.date) FROM Attendance a WHERE a.student = :student")
    Optional<LocalDate> lastDate(@Param("student") Student student);

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.student = :student")
    void deleteByStudent(@Param("student") Student student);

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.course = :course")
    void deleteByCourse(@Param("course") Course course);
}
