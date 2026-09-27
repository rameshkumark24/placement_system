package com.rameshkumar.placementsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StudentDTO {

    private Long id;
    private Long userId;

    @NotBlank(message = "Name cannot be empty")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email cannot be empty")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @DecimalMin(value = "0.0", message = "CGPA must be positive")
    @DecimalMax(value = "10.0", message = "CGPA cannot exceed 10")
    private double cgpa;

    @NotBlank(message = "Skills cannot be empty")
    @Size(max = 255, message = "Skills must be at most 255 characters")
    private String skills;

    @Size(max = 255, message = "Resume link must be at most 255 characters")
    private String resumeLink;

    public StudentDTO() {
    }

    public StudentDTO(Long id, Long userId, String name, String email, String password, double cgpa, String skills, String resumeLink) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.cgpa = cgpa;
        this.skills = skills;
        this.resumeLink = resumeLink;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
