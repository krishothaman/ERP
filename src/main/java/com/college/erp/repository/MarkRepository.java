package com.college.erp.repository;

import com.college.erp.entity.Course;
import com.college.erp.entity.Mark;
import com.college.erp.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarkRepository extends JpaRepository<Mark, Long> {

    List<Mark> findByCourse(Course course);

    Optional<Mark> findByStudentAndCourse(Student student, Course course);

    void deleteByStudent(Student student);

    void deleteByCourse(Course course);
}
