package com.rameshkumar.placementsystem.service;

import com.rameshkumar.placementsystem.dto.CompanyDTO;
import com.rameshkumar.placementsystem.entity.Company;
import com.rameshkumar.placementsystem.exception.CompanyNotFoundException;
import com.rameshkumar.placementsystem.repository.CompanyRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CompanyServiceImpl implements CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyServiceImpl.class);

    private final CompanyRepository companyRepository;

    public CompanyServiceImpl(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    public CompanyDTO saveCompany(CompanyDTO companyDTO) {
        Company company = mapToEntity(companyDTO);
        Company savedCompany = companyRepository.save(company);
        logger.info("Company created with id {} and name {}", savedCompany.getId(), savedCompany.getName());
        return mapToDTO(savedCompany);
    }

    @Override
    public List<CompanyDTO> getAllCompanies() {
        logger.info("Fetching all companies");
        return companyRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public List<CompanyDTO> filterCompaniesByRole(String role) {
        logger.info("Filtering companies by role {}", role);
        return companyRepository.findByRoleContainingIgnoreCase(role)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public CompanyDTO getCompanyById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Company not found with id {}", id);
                    return new CompanyNotFoundException("Company not found with id: " + id);
                });
        logger.info("Fetched company with id {}", id);
        return mapToDTO(company);
    }

    @Override
    public void deleteCompany(Long id) {
        if (!companyRepository.existsById(id)) {
            logger.warn("Company not found for delete with id {}", id);
            throw new CompanyNotFoundException("Company not found with id: " + id);
        }
        companyRepository.deleteById(id);
        logger.info("Company deleted with id {}", id);
    }

    private CompanyDTO mapToDTO(Company company) {
        return new CompanyDTO(
                company.getId(),
                company.getName(),
                company.getRole(),
                company.getPackageOffered(),
                company.getEligibilityCgpa(),
                company.getDeadline()
        );
    }

    private Company mapToEntity(CompanyDTO companyDTO) {
        Company company = new Company();
        company.setName(companyDTO.getName());
        company.setRole(companyDTO.getRole());
        company.setPackageOffered(companyDTO.getPackageOffered());
        company.setEligibilityCgpa(companyDTO.getEligibilityCgpa());
        company.setDeadline(companyDTO.getDeadline());
        return company;
    }
}
