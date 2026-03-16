package com.rameshkumar.placementsystem.service;

import com.rameshkumar.placementsystem.dto.ApplicationDTO;
import java.util.List;

public interface ApplicationService {

    ApplicationDTO applyToCompany(String studentEmail, Long companyId);

    List<ApplicationDTO> getMyApplications(String studentEmail);

    List<ApplicationDTO> getAllApplications();
}
