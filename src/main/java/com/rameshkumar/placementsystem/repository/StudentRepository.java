package com.rameshkumar.placementsystem.repository;

import com.rameshkumar.placementsystem.entity.Student;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findBySkillsContainingIgnoreCase(String skill);

    List<Student> findBySkillsContainingIgnoreCaseAndCgpaGreaterThanEqual(String skill, double cgpa);

    List<Student> findByCgpaGreaterThanEqual(double cgpa);

    Optional<Student> findByUserId(Long userId);

    Optional<Student> findByUserEmail(String email);

    boolean existsByUserEmail(String email);
}
