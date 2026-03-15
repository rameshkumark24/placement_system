package com.rameshkumar.placementsystem.dto;

import jakarta.validation.constraints.*;

public class StudentDTO {

    private Long id;

    @NotBlank(message = "Name cannot be empty")
    private String name;

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email cannot be empty")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    private String password;

    @DecimalMin(value = "0.0", message = "CGPA must be positive")
    @DecimalMax(value = "10.0", message = "CGPA cannot exceed 10")
    private double cgpa;

    @NotBlank(message = "Skills cannot be empty")
    private String skills;

    public StudentDTO() {}

    public StudentDTO(Long id, String name, String email, String password, double cgpa, String skills) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.cgpa = cgpa;
        this.skills = skills;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {   // FIXED
        this.id = id;
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
        this.email = email;
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
}