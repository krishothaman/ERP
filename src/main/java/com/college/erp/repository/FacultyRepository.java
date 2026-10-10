package com.college.erp.repository;

import com.college.erp.entity.Faculty;
import com.college.erp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    Optional<Faculty> findByUser(User user);

    boolean existsByEmail(String email);

    List<Faculty> findAllByOrderByName();
}
