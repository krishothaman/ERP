package com.college.erp.repository;

import com.college.erp.entity.Course;
import com.college.erp.entity.Mark;
import com.college.erp.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarkRepository extends JpaRepository<Mark, Long> {

    List<Mark> findByCourse(Course course);

    Optional<Mark> findByStudentAndCourse(Student student, Course course);

    @Modifying
    @Query("DELETE FROM Mark m WHERE m.student = :student")
    void deleteByStudent(@Param("student") Student student);

    @Modifying
    @Query("DELETE FROM Mark m WHERE m.course = :course")
    void deleteByCourse(@Param("course") Course course);
}
