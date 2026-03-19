package com.rameshkumar.placementsystem.repository;

import com.rameshkumar.placementsystem.entity.Student;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByUserRole(String role);

    List<Student> findByUserRoleAndSkillsContainingIgnoreCase(String role, String skill);

    List<Student> findByUserRoleAndSkillsContainingIgnoreCaseAndCgpaGreaterThanEqual(String role, String skill, double cgpa);

    List<Student> findByUserRoleAndCgpaGreaterThanEqual(String role, double cgpa);

    long countByUserRole(String role);

    Page<Student> findByUserRole(String role, Pageable pageable);

    Optional<Student> findByUserId(Long userId);

    Optional<Student> findByUserEmail(String email);

    boolean existsByUserEmail(String email);
}
