package com.rameshkumar.placementsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CompanyDTO {

    private Long id;

    @NotBlank(message = "Company name cannot be empty")
    private String name;

    @NotBlank(message = "Role cannot be empty")
    private String role;

    @NotNull(message = "Package cannot be empty")
    @DecimalMin(value = "0.0", message = "Package must be positive")
    @JsonProperty("package")
    private Double packageOffered;

    @NotNull(message = "Eligibility CGPA cannot be empty")
    @DecimalMin(value = "0.0", message = "Eligibility CGPA must be positive")
    @DecimalMax(value = "10.0", message = "Eligibility CGPA cannot exceed 10")
    private Double eligibilityCgpa;

    @NotNull(message = "Deadline cannot be empty")
    @FutureOrPresent(message = "Deadline must be today or a future date")
    private LocalDate deadline;

    public CompanyDTO() {
    }

    public CompanyDTO(Long id, String name, String role, Double packageOffered, Double eligibilityCgpa, LocalDate deadline) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.packageOffered = packageOffered;
        this.eligibilityCgpa = eligibilityCgpa;
        this.deadline = deadline;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @JsonProperty("package")
    public Double getPackageOffered() {
        return packageOffered;
    }

    @JsonProperty("package")
    public void setPackageOffered(Double packageOffered) {
        this.packageOffered = packageOffered;
    }

    public Double getEligibilityCgpa() {
        return eligibilityCgpa;
    }

    public void setEligibilityCgpa(Double eligibilityCgpa) {
        this.eligibilityCgpa = eligibilityCgpa;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }
}
