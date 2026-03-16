package com.rameshkumar.placementsystem.controller;

import com.rameshkumar.placementsystem.dto.ApiResponse;
import com.rameshkumar.placementsystem.dto.CompanyDTO;
import com.rameshkumar.placementsystem.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Company APIs", description = "Operations related to company management")
@RestController
@RequestMapping("/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create a new company",
            description = "Creates a company record. Accessible only to ADMIN users.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Company created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping
    public ApiResponse<CompanyDTO> createCompany(@Valid @RequestBody CompanyDTO companyDTO) {
        CompanyDTO savedCompany = companyService.saveCompany(companyDTO);
        return new ApiResponse<>(true, "Company created successfully", savedCompany);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all companies",
            description = "Returns all company records. Accessible only to ADMIN users.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Companies fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ApiResponse<List<CompanyDTO>> getAllCompanies(@RequestParam(required = false) String role) {
        List<CompanyDTO> companies = role != null && !role.isBlank()
                ? companyService.filterCompaniesByRole(role)
                : companyService.getAllCompanies();
        return new ApiResponse<>(true, "Companies fetched successfully", companies);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get company by ID",
            description = "Returns a single company by id. Accessible only to ADMIN users.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Company fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Company not found")
    })
    @GetMapping("/{id}")
    public ApiResponse<CompanyDTO> getCompanyById(@PathVariable Long id) {
        CompanyDTO company = companyService.getCompanyById(id);
        return new ApiResponse<>(true, "Company fetched successfully", company);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete company by ID",
            description = "Deletes a company record by id. Accessible only to ADMIN users.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Company deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Company not found")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteCompany(@PathVariable Long id) {
        companyService.deleteCompany(id);
        return new ApiResponse<>(true, "Company deleted successfully", null);
    }
}
