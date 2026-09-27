package com.rameshkumar.placementsystem.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StudentProfileUpdateRequest {

    @NotBlank(message = "Name cannot be empty")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @DecimalMin(value = "0.0", message = "CGPA must be positive")
    @DecimalMax(value = "10.0", message = "CGPA cannot exceed 10")
    private double cgpa;

    @NotBlank(message = "Skills cannot be empty")
    @Size(max = 255, message = "Skills must be at most 255 characters")
    private String skills;

    @Size(max = 255, message = "Resume link must be at most 255 characters")
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
