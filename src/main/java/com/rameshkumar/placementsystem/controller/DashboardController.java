package com.rameshkumar.placementsystem.controller;

import com.rameshkumar.placementsystem.dto.ApiResponse;
import com.rameshkumar.placementsystem.dto.DashboardStatsDTO;
import com.rameshkumar.placementsystem.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard APIs", description = "Operations related to dashboard analytics")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get admin dashboard stats",
            description = "Returns aggregate placement statistics for the admin dashboard.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/stats")
    public ApiResponse<DashboardStatsDTO> getAdminStats() {
        DashboardStatsDTO stats = dashboardService.getAdminStats();
        return new ApiResponse<>(true, "Dashboard statistics fetched successfully", stats);
    }
}
