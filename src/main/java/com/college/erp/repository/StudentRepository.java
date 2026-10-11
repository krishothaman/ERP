package com.college.erp.repository;

import com.college.erp.entity.Student;
import com.college.erp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUser(User user);

    /** All students who take a course of this department and year. */
    List<Student> findByDepartmentAndYearOrderByRollNo(String department, Integer year);

    boolean existsByRollNo(String rollNo);

    boolean existsByRollNoAndIdNot(String rollNo, Long id);

    List<Student> findAllByOrderByRollNo();

    @Query("SELECT s FROM Student s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(s.rollNo) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(s.department) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY s.rollNo")
    List<Student> search(@Param("q") String q);
}
