package com.rameshkumar.placementsystem.repository;

import com.rameshkumar.placementsystem.entity.Application;
import com.rameshkumar.placementsystem.entity.ApplicationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByStudentId(Long studentId);

    List<Application> findByCompanyNameContainingIgnoreCase(String companyName);

    List<Application> findByStatus(ApplicationStatus status);

    List<Application> findByStudentUserEmailContainingIgnoreCase(String studentEmail);

    List<Application> findByCompanyNameContainingIgnoreCaseAndStatus(String companyName, ApplicationStatus status);

    List<Application> findByCompanyNameContainingIgnoreCaseAndStudentUserEmailContainingIgnoreCase(String companyName, String studentEmail);

    List<Application> findByStatusAndStudentUserEmailContainingIgnoreCase(ApplicationStatus status, String studentEmail);

    List<Application> findByCompanyNameContainingIgnoreCaseAndStatusAndStudentUserEmailContainingIgnoreCase(
            String companyName,
            ApplicationStatus status,
            String studentEmail
    );

    long countByStatus(ApplicationStatus status);

    boolean existsByStudentIdAndCompanyId(Long studentId, Long companyId);
}
