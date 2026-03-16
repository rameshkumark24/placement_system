package com.rameshkumar.placementsystem.service;

import com.rameshkumar.placementsystem.dto.ApplicationDTO;
import com.rameshkumar.placementsystem.entity.Application;
import com.rameshkumar.placementsystem.entity.Company;
import com.rameshkumar.placementsystem.entity.Student;
import com.rameshkumar.placementsystem.exception.CompanyNotFoundException;
import com.rameshkumar.placementsystem.repository.ApplicationRepository;
import com.rameshkumar.placementsystem.repository.CompanyRepository;
import com.rameshkumar.placementsystem.repository.StudentRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);
    private static final String APPLIED_STATUS = "APPLIED";

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;

    public ApplicationServiceImpl(ApplicationRepository applicationRepository,
                                  StudentRepository studentRepository,
                                  CompanyRepository companyRepository) {
        this.applicationRepository = applicationRepository;
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public ApplicationDTO applyToCompany(String studentEmail, Long companyId) {
        Student student = studentRepository.findByUserEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Authenticated student profile not found"));

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found with id: " + companyId));

        if (applicationRepository.existsByStudentIdAndCompanyId(student.getId(), companyId)) {
            logger.warn("Duplicate application attempt by student {} for company {}", studentEmail, companyId);
            throw new RuntimeException("You have already applied to this company");
        }

        Application application = new Application();
        application.setStudent(student);
        application.setCompany(company);
        application.setStatus(APPLIED_STATUS);
        application.setAppliedDate(LocalDate.now());

        Application savedApplication = applicationRepository.save(application);
        logger.info("Student {} applied to company {}", studentEmail, company.getName());
        return mapToDTO(savedApplication);
    }

    @Override
    public List<ApplicationDTO> getMyApplications(String studentEmail) {
        Student student = studentRepository.findByUserEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Authenticated student profile not found"));
        logger.info("Fetching applications for student {}", studentEmail);
        return applicationRepository.findByStudentId(student.getId())
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public List<ApplicationDTO> getAllApplications() {
        logger.info("Fetching all applications");
        return applicationRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    private ApplicationDTO mapToDTO(Application application) {
        return new ApplicationDTO(
                application.getId(),
                application.getStudent().getId(),
                application.getStudent().getUser().getEmail(),
                application.getCompany().getId(),
                application.getCompany().getName(),
                application.getStatus(),
                application.getAppliedDate()
        );
    }
}
