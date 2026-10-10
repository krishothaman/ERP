package com.college.erp.repository;

import com.college.erp.entity.Course;
import com.college.erp.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByFacultyOrderByCode(Faculty faculty);

    List<Course> findByDepartmentAndYearOrderByCode(String department, Integer year);

    Optional<Course> findByCode(String code);

    boolean existsByCode(String code);

    List<Course> findAllByOrderByCode();
}
