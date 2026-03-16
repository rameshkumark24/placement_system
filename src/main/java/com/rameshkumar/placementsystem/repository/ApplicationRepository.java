package com.rameshkumar.placementsystem.repository;

import com.rameshkumar.placementsystem.entity.Application;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByStudentId(Long studentId);

    boolean existsByStudentIdAndCompanyId(Long studentId, Long companyId);
}
