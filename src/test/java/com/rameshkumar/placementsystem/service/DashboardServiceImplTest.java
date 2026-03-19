package com.rameshkumar.placementsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.rameshkumar.placementsystem.dto.DashboardStatsDTO;
import com.rameshkumar.placementsystem.entity.ApplicationStatus;
import com.rameshkumar.placementsystem.repository.ApplicationRepository;
import com.rameshkumar.placementsystem.repository.CompanyRepository;
import com.rameshkumar.placementsystem.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void getAdminStatsAggregatesCountsFromRepositories() {
        when(studentRepository.countByUserRole("STUDENT")).thenReturn(18L);
        when(companyRepository.count()).thenReturn(6L);
        when(applicationRepository.count()).thenReturn(24L);
        when(applicationRepository.countByStatus(ApplicationStatus.SHORTLISTED)).thenReturn(5L);
        when(applicationRepository.countByStatus(ApplicationStatus.SELECTED)).thenReturn(2L);

        DashboardStatsDTO result = dashboardService.getAdminStats();

        assertEquals(18L, result.getTotalStudents());
        assertEquals(6L, result.getTotalCompanies());
        assertEquals(24L, result.getTotalApplications());
        assertEquals(5L, result.getShortlistedApplications());
        assertEquals(2L, result.getSelectedApplications());
    }
}
