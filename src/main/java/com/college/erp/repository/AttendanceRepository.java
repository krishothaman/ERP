package com.college.erp.repository;

import com.college.erp.entity.Attendance;
import com.college.erp.entity.Course;
import com.college.erp.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByCourseAndDate(Course course, LocalDate date);

    Optional<Attendance> findByStudentAndCourseAndDate(Student student, Course course, LocalDate date);

    long countByStudentAndCourse(Student student, Course course);

    long countByStudentAndCourseAndPresentTrue(Student student, Course course);

    void deleteByStudent(Student student);

    void deleteByCourse(Course course);
}
