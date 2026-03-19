package com.rameshkumar.placementsystem.service;

import com.rameshkumar.placementsystem.dto.DashboardStatsDTO;
import com.rameshkumar.placementsystem.entity.ApplicationStatus;
import com.rameshkumar.placementsystem.repository.ApplicationRepository;
import com.rameshkumar.placementsystem.repository.CompanyRepository;
import com.rameshkumar.placementsystem.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);
    private static final String STUDENT_ROLE = "STUDENT";

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationRepository applicationRepository;

    public DashboardServiceImpl(StudentRepository studentRepository,
                                CompanyRepository companyRepository,
                                ApplicationRepository applicationRepository) {
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    public DashboardStatsDTO getAdminStats() {
        logger.info("Fetching admin dashboard statistics");
        return new DashboardStatsDTO(
                studentRepository.countByUserRole(STUDENT_ROLE),
                companyRepository.count(),
                applicationRepository.count(),
                applicationRepository.countByStatus(ApplicationStatus.SHORTLISTED),
                applicationRepository.countByStatus(ApplicationStatus.SELECTED)
        );
    }
}
