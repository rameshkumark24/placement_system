package com.rameshkumar.placementsystem.repository;

import com.rameshkumar.placementsystem.entity.Company;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findByRoleContainingIgnoreCase(String role);
}
