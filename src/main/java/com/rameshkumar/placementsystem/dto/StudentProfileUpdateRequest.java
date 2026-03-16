package com.rameshkumar.placementsystem.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public class StudentProfileUpdateRequest {

    @NotBlank(message = "Name cannot be empty")
    private String name;

    @DecimalMin(value = "0.0", message = "CGPA must be positive")
    @DecimalMax(value = "10.0", message = "CGPA cannot exceed 10")
    private double cgpa;

    @NotBlank(message = "Skills cannot be empty")
    private String skills;

    private String resumeLink;

    public StudentProfileUpdateRequest() {
    }

    public StudentProfileUpdateRequest(String name, double cgpa, String skills, String resumeLink) {
        this.name = name;
        this.cgpa = cgpa;
        this.skills = skills;
        this.resumeLink = resumeLink;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCgpa() {
        return cgpa;
    }

    public void setCgpa(double cgpa) {
        this.cgpa = cgpa;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getResumeLink() {
        return resumeLink;
    }

    public void setResumeLink(String resumeLink) {
        this.resumeLink = resumeLink;
    }
}
