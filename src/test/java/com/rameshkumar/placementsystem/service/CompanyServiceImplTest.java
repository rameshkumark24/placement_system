package com.rameshkumar.placementsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rameshkumar.placementsystem.dto.CompanyDTO;
import com.rameshkumar.placementsystem.entity.Company;
import com.rameshkumar.placementsystem.exception.CompanyNotFoundException;
import com.rameshkumar.placementsystem.repository.CompanyRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyServiceImpl companyService;

    @Test
    void updateCompanyUpdatesExistingRecord() {
        Company existingCompany = new Company();
        existingCompany.setId(5L);
        existingCompany.setName("Old Name");
        existingCompany.setRole("Old Role");
        existingCompany.setPackageOffered(7.0);
        existingCompany.setEligibilityCgpa(6.5);
        existingCompany.setDeadline(LocalDate.now().plusDays(5));

        CompanyDTO request = new CompanyDTO(
                null,
                "New Name",
                "Backend Engineer",
                12.0,
                7.5,
                LocalDate.now().plusDays(20)
        );

        when(companyRepository.findById(5L)).thenReturn(Optional.of(existingCompany));
        when(companyRepository.save(existingCompany)).thenReturn(existingCompany);

        CompanyDTO result = companyService.updateCompany(5L, request);

        assertEquals("New Name", result.getName());
        assertEquals("Backend Engineer", result.getRole());
        assertEquals(12.0, result.getPackageOffered());
        assertEquals(7.5, result.getEligibilityCgpa());
        verify(companyRepository).save(existingCompany);
    }

    @Test
    void updateCompanyThrowsWhenMissing() {
        CompanyDTO request = new CompanyDTO(
                null,
                "New Name",
                "Backend Engineer",
                12.0,
                7.5,
                LocalDate.now().plusDays(20)
        );

        when(companyRepository.findById(50L)).thenReturn(Optional.empty());

        assertThrows(CompanyNotFoundException.class, () -> companyService.updateCompany(50L, request));
    }
}
