package com.rameshkumar.placementsystem.repository;

import com.rameshkumar.placementsystem.entity.Application;
import com.rameshkumar.placementsystem.entity.ApplicationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listing queries fetch the student, its user, and the company in one query so that mapping
 * applications to DTOs does not trigger one extra query per row.
 */
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    @Override
    @EntityGraph(attributePaths = {"student", "student.user", "company"})
    List<Application> findAll();

    @EntityGraph(attributePaths = {"student", "student.user", "company"})
    List<Application> findByStudentId(Long studentId);

    @EntityGraph(attributePaths = {"student", "student.user", "company"})
    List<Application> findByCompanyNameContainingIgnoreCase(String companyName);

    @EntityGraph(attributePaths = {"student", "student.user", "company"})
    List<Application> findByStatus(ApplicationStatus status);

    @EntityGraph(attributePaths = {"student", "student.user", "company"})
    List<Application> findByStudentUserEmailContainingIgnoreCase(String studentEmail);

    @EntityGraph(attributePaths = {"student", "student.user", "company"})
    List<Application> findByCompanyNameContainingIgnoreCaseAndStatus(String companyName, ApplicationStatus status);

    @EntityGraph(attributePaths = {"student", "student.user", "company"})
    List<Application> findByCompanyNameContainingIgnoreCaseAndStudentUserEmailContainingIgnoreCase(String companyName, String studentEmail);

    @EntityGraph(attributePaths = {"student", "student.user", "company"})
    List<Application> findByStatusAndStudentUserEmailContainingIgnoreCase(ApplicationStatus status, String studentEmail);

    @EntityGraph(attributePaths = {"student", "student.user", "company"})
    List<Application> findByCompanyNameContainingIgnoreCaseAndStatusAndStudentUserEmailContainingIgnoreCase(
            String companyName,
            ApplicationStatus status,
            String studentEmail
    );

    long countByStatus(ApplicationStatus status);

    boolean existsByStudentIdAndCompanyId(Long studentId, Long companyId);

    @Transactional
    void deleteByStudentId(Long studentId);

    @Transactional
    void deleteByCompanyId(Long companyId);
}
