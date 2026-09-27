package com.rameshkumar.placementsystem.service;

import com.rameshkumar.placementsystem.dto.ApplicationDTO;
import com.rameshkumar.placementsystem.dto.ApplicationStatusUpdateRequest;
import com.rameshkumar.placementsystem.entity.Application;
import com.rameshkumar.placementsystem.entity.ApplicationStatus;
import com.rameshkumar.placementsystem.entity.Company;
import com.rameshkumar.placementsystem.entity.Student;
import com.rameshkumar.placementsystem.entity.StudentProfileDefaults;
import com.rameshkumar.placementsystem.entity.User;
import com.rameshkumar.placementsystem.exception.BadRequestException;
import com.rameshkumar.placementsystem.exception.CompanyNotFoundException;
import com.rameshkumar.placementsystem.exception.ConflictException;
import com.rameshkumar.placementsystem.exception.ResourceNotFoundException;
import com.rameshkumar.placementsystem.repository.ApplicationRepository;
import com.rameshkumar.placementsystem.repository.CompanyRepository;
import com.rameshkumar.placementsystem.repository.StudentRepository;
import com.rameshkumar.placementsystem.repository.UserRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    // Newest applications first.
    private static final Comparator<Application> NEWEST_FIRST = Comparator
            .comparing(Application::getAppliedDate, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Application::getId, Comparator.nullsLast(Comparator.reverseOrder()));

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public ApplicationServiceImpl(ApplicationRepository applicationRepository,
                                  StudentRepository studentRepository,
                                  CompanyRepository companyRepository,
                                  UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ApplicationDTO applyToCompany(String studentEmail, Long companyId) {
        Student student = getOrCreateStudentProfile(studentEmail);
        validateProfileCompletion(student);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found with id: " + companyId));

        validateEligibility(student, company);

        if (applicationRepository.existsByStudentIdAndCompanyId(student.getId(), companyId)) {
            logger.warn("Duplicate application attempt by student {} for company {}", studentEmail, companyId);
            throw new ConflictException("You have already applied to this company");
        }

        Application application = new Application();
        application.setStudent(student);
        application.setCompany(company);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setAppliedDate(LocalDate.now());

        Application savedApplication = applicationRepository.save(application);
        logger.info("Student {} applied to company {}", studentEmail, company.getName());
        return mapToDTO(savedApplication);
    }

    // Not read-only: a missing profile is created on first access.
    @Override
    @Transactional
    public List<ApplicationDTO> getMyApplications(String studentEmail) {
        Student student = getOrCreateStudentProfile(studentEmail);
        logger.info("Fetching applications for student {}", studentEmail);
        return applicationRepository.findByStudentId(student.getId())
                .stream()
                .sorted(NEWEST_FIRST)
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationDTO> getAllApplications(String companyName, String status, String studentEmail) {
        logger.info("Fetching applications with filters company='{}', status='{}', studentEmail='{}'",
                companyName, status, studentEmail);
        return findApplications(companyName, status, studentEmail)
                .stream()
                .sorted(NEWEST_FIRST)
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional
    public ApplicationDTO updateApplicationStatus(Long applicationId, ApplicationStatusUpdateRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        ApplicationStatus nextStatus = parseStatus(request.getStatus());
        application.setStatus(nextStatus);

        Application updatedApplication = applicationRepository.save(application);
        logger.info("Updated application {} to status {}", applicationId, nextStatus);
        return mapToDTO(updatedApplication);
    }

    private List<Application> findApplications(String companyName, String status, String studentEmail) {
        boolean hasCompany = StringUtils.hasText(companyName);
        boolean hasStatus = StringUtils.hasText(status);
        boolean hasStudentEmail = StringUtils.hasText(studentEmail);
        ApplicationStatus parsedStatus = hasStatus ? parseStatus(status) : null;
        String company = hasCompany ? companyName.trim() : null;
        String email = hasStudentEmail ? studentEmail.trim() : null;

        if (hasCompany && hasStatus && hasStudentEmail) {
            return applicationRepository.findByCompanyNameContainingIgnoreCaseAndStatusAndStudentUserEmailContainingIgnoreCase(
                    company, parsedStatus, email);
        }
        if (hasCompany && hasStatus) {
            return applicationRepository.findByCompanyNameContainingIgnoreCaseAndStatus(company, parsedStatus);
        }
        if (hasCompany && hasStudentEmail) {
            return applicationRepository.findByCompanyNameContainingIgnoreCaseAndStudentUserEmailContainingIgnoreCase(
                    company, email);
        }
        if (hasStatus && hasStudentEmail) {
            return applicationRepository.findByStatusAndStudentUserEmailContainingIgnoreCase(parsedStatus, email);
        }
        if (hasCompany) {
            return applicationRepository.findByCompanyNameContainingIgnoreCase(company);
        }
        if (hasStatus) {
            return applicationRepository.findByStatus(parsedStatus);
        }
        if (hasStudentEmail) {
            return applicationRepository.findByStudentUserEmailContainingIgnoreCase(email);
        }
        return applicationRepository.findAll();
    }

    private ApplicationDTO mapToDTO(Application application) {
        return new ApplicationDTO(
                application.getId(),
                application.getStudent().getId(),
                application.getStudent().getUser().getEmail(),
                application.getCompany().getId(),
                application.getCompany().getName(),
                application.getStatus().name(),
                application.getAppliedDate()
        );
    }

    private ApplicationStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new BadRequestException("Application status is required");
        }
        try {
            return ApplicationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid application status: " + status);
        }
    }

    private void validateProfileCompletion(Student student) {
        if (student.getCgpa() <= 0.0
                || !StudentProfileDefaults.hasRealSkills(student.getSkills())
                || !StringUtils.hasText(student.getResumeLink())) {
            throw new BadRequestException("Complete your profile with CGPA, skills, and resume link before applying");
        }
    }

    private void validateEligibility(Student student, Company company) {
        LocalDate deadline = company.getDeadline();
        if (deadline != null && deadline.isBefore(LocalDate.now())) {
            throw new BadRequestException("The application deadline for " + company.getName() + " has passed");
        }

        Double requiredCgpa = company.getEligibilityCgpa();
        if (requiredCgpa != null && student.getCgpa() < requiredCgpa) {
            throw new BadRequestException(String.format(
                    "Your CGPA (%.2f) is below the minimum of %.2f required by %s",
                    student.getCgpa(), requiredCgpa, company.getName()));
        }
    }

    private Student getOrCreateStudentProfile(String studentEmail) {
        return studentRepository.findByUserEmail(studentEmail)
                .orElseGet(() -> {
                    User user = userRepository.findByEmail(studentEmail)
                            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

                    if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
                        throw new BadRequestException("Only students can apply to companies");
                    }

                    Student savedProfile = studentRepository.save(StudentProfileDefaults.newEmptyProfile(user));
                    logger.warn("Created missing student profile {} for user {}", savedProfile.getId(), studentEmail);
                    return savedProfile;
                });
    }
}
