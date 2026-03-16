package com.rameshkumar.placementsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rameshkumar.placementsystem.dto.ApplicationDTO;
import com.rameshkumar.placementsystem.dto.ApplicationStatusUpdateRequest;
import com.rameshkumar.placementsystem.entity.Application;
import com.rameshkumar.placementsystem.entity.ApplicationStatus;
import com.rameshkumar.placementsystem.entity.Company;
import com.rameshkumar.placementsystem.entity.Student;
import com.rameshkumar.placementsystem.entity.User;
import com.rameshkumar.placementsystem.repository.ApplicationRepository;
import com.rameshkumar.placementsystem.repository.CompanyRepository;
import com.rameshkumar.placementsystem.repository.StudentRepository;
import com.rameshkumar.placementsystem.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private Student student;
    private Company company;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(11L);
        user.setEmail("student@example.com");
        user.setRole("STUDENT");

        student = new Student();
        student.setId(7L);
        student.setUser(user);
        student.setCgpa(8.5);
        student.setSkills("Java, Spring Boot");
        student.setResumeLink("https://resume.example.com");

        company = new Company();
        company.setId(3L);
        company.setName("Acme");
        company.setRole("Backend Developer");
        company.setPackageOffered(10.5);
        company.setEligibilityCgpa(7.0);
        company.setDeadline(LocalDate.now().plusDays(10));
    }

    @Test
    void applyToCompanyRejectsIncompleteStudentProfile() {
        student.setResumeLink(null);
        when(studentRepository.findByUserEmail("student@example.com")).thenReturn(Optional.of(student));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> applicationService.applyToCompany("student@example.com", 3L));

        assertEquals("Complete your profile with CGPA, skills, and resume link before applying", exception.getMessage());
        verify(companyRepository, never()).findById(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void applyToCompanyCreatesApplicationForCompleteProfile() {
        when(studentRepository.findByUserEmail("student@example.com")).thenReturn(Optional.of(student));
        when(companyRepository.findById(3L)).thenReturn(Optional.of(company));
        when(applicationRepository.existsByStudentIdAndCompanyId(7L, 3L)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> {
            Application application = invocation.getArgument(0);
            application.setId(20L);
            return application;
        });

        ApplicationDTO result = applicationService.applyToCompany("student@example.com", 3L);

        assertEquals(20L, result.getId());
        assertEquals("student@example.com", result.getStudentEmail());
        assertEquals("Acme", result.getCompanyName());
        assertEquals("APPLIED", result.getStatus());
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void updateApplicationStatusRejectsInvalidStatus() {
        Application application = new Application();
        application.setId(9L);
        application.setStudent(student);
        application.setCompany(company);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setAppliedDate(LocalDate.now());

        when(applicationRepository.findById(9L)).thenReturn(Optional.of(application));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> applicationService.updateApplicationStatus(9L, new ApplicationStatusUpdateRequest("UNKNOWN")));

        assertEquals("Invalid application status: UNKNOWN", exception.getMessage());
        verify(applicationRepository, never()).save(any(Application.class));
    }
}
