package com.rameshkumar.placementsystem.controller;

import com.rameshkumar.placementsystem.dto.ApiResponse;
import com.rameshkumar.placementsystem.dto.ApplicationDTO;
import com.rameshkumar.placementsystem.dto.ApplicationStatusUpdateRequest;
import com.rameshkumar.placementsystem.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Application APIs", description = "Operations related to company applications")
@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
            summary = "Apply to a company",
            description = "Allows a STUDENT user to apply to a company by company id.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Application submitted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or duplicate application"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Company not found")
    })
    @PostMapping("/apply/{companyId}")
    public ApiResponse<ApplicationDTO> applyToCompany(@PathVariable Long companyId, Principal principal) {
        ApplicationDTO application = applicationService.applyToCompany(principal.getName(), companyId);
        return new ApiResponse<>(true, "Application submitted successfully", application);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @Operation(
            summary = "Get my applications",
            description = "Returns applications submitted by the currently authenticated STUDENT user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Applications fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/my")
    public ApiResponse<List<ApplicationDTO>> getMyApplications(Principal principal) {
        List<ApplicationDTO> applications = applicationService.getMyApplications(principal.getName());
        return new ApiResponse<>(true, "Applications fetched successfully", applications);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all applications",
            description = "Returns all applications. Accessible only to ADMIN users.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Applications fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ApiResponse<List<ApplicationDTO>> getAllApplications(
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String studentEmail) {
        List<ApplicationDTO> applications = applicationService.getAllApplications(company, status, studentEmail);
        return new ApiResponse<>(true, "All applications fetched successfully", applications);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update application status",
            description = "Allows an ADMIN user to update an application status to APPLIED, SHORTLISTED, REJECTED, or SELECTED.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Application status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status or application id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PutMapping("/{applicationId}/status")
    public ApiResponse<ApplicationDTO> updateApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        ApplicationDTO updatedApplication = applicationService.updateApplicationStatus(applicationId, request);
        return new ApiResponse<>(true, "Application status updated successfully", updatedApplication);
    }
}
