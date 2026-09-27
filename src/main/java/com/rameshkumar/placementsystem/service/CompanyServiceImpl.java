package com.rameshkumar.placementsystem.service;

import com.rameshkumar.placementsystem.dto.CompanyDTO;
import com.rameshkumar.placementsystem.entity.Company;
import com.rameshkumar.placementsystem.exception.CompanyNotFoundException;
import com.rameshkumar.placementsystem.repository.ApplicationRepository;
import com.rameshkumar.placementsystem.repository.CompanyRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyServiceImpl implements CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyServiceImpl.class);

    // Soonest deadline first, so students see the most urgent openings at the top.
    private static final Sort BY_DEADLINE = Sort.by(Sort.Order.asc("deadline"), Sort.Order.asc("id"));

    private final CompanyRepository companyRepository;
    private final ApplicationRepository applicationRepository;

    public CompanyServiceImpl(CompanyRepository companyRepository,
                              ApplicationRepository applicationRepository) {
        this.companyRepository = companyRepository;
        this.applicationRepository = applicationRepository;
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
        return companyRepository.findAll(BY_DEADLINE)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public List<CompanyDTO> filterCompaniesByRole(String role) {
        logger.info("Filtering companies by role {}", role);
        return companyRepository.findByRoleContainingIgnoreCase(role.trim(), BY_DEADLINE)
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
    public CompanyDTO updateCompany(Long id, CompanyDTO companyDTO) {
        Company existingCompany = companyRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Company not found for update with id {}", id);
                    return new CompanyNotFoundException("Company not found with id: " + id);
                });

        existingCompany.setName(companyDTO.getName().trim());
        existingCompany.setRole(companyDTO.getRole().trim());
        existingCompany.setPackageOffered(companyDTO.getPackageOffered());
        existingCompany.setEligibilityCgpa(companyDTO.getEligibilityCgpa());
        existingCompany.setDeadline(companyDTO.getDeadline());

        Company updatedCompany = companyRepository.save(existingCompany);
        logger.info("Company updated with id {}", id);
        return mapToDTO(updatedCompany);
    }

    // Applications reference the company through a foreign key, so they are removed first.
    @Override
    @Transactional
    public void deleteCompany(Long id) {
        if (!companyRepository.existsById(id)) {
            logger.warn("Company not found for delete with id {}", id);
            throw new CompanyNotFoundException("Company not found with id: " + id);
        }
        applicationRepository.deleteByCompanyId(id);
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
        company.setName(companyDTO.getName().trim());
        company.setRole(companyDTO.getRole().trim());
        company.setPackageOffered(companyDTO.getPackageOffered());
        company.setEligibilityCgpa(companyDTO.getEligibilityCgpa());
        company.setDeadline(companyDTO.getDeadline());
        return company;
    }
}
