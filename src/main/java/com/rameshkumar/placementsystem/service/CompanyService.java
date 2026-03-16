package com.rameshkumar.placementsystem.service;

import com.rameshkumar.placementsystem.dto.CompanyDTO;
import java.util.List;

public interface CompanyService {

    CompanyDTO saveCompany(CompanyDTO companyDTO);

    List<CompanyDTO> getAllCompanies();

    List<CompanyDTO> filterCompaniesByRole(String role);

    CompanyDTO getCompanyById(Long id);

    void deleteCompany(Long id);
}
