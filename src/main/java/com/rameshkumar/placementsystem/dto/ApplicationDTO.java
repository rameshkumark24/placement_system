package com.rameshkumar.placementsystem.dto;

import java.time.LocalDate;

public class ApplicationDTO {

    private Long id;
    private Long studentId;
    private String studentEmail;
    private Long companyId;
    private String companyName;
    private String status;
    private LocalDate appliedDate;

    public ApplicationDTO() {
    }

    public ApplicationDTO(Long id, Long studentId, String studentEmail, Long companyId, String companyName, String status, LocalDate appliedDate) {
        this.id = id;
        this.studentId = studentId;
        this.studentEmail = studentEmail;
        this.companyId = companyId;
        this.companyName = companyName;
        this.status = status;
        this.appliedDate = appliedDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(LocalDate appliedDate) {
        this.appliedDate = appliedDate;
    }
}
