package com.rameshkumar.placementsystem.dto;

public class DashboardStatsDTO {

    private long totalStudents;
    private long totalCompanies;
    private long totalApplications;
    private long shortlistedApplications;
    private long selectedApplications;

    public DashboardStatsDTO() {
    }

    public DashboardStatsDTO(long totalStudents,
                             long totalCompanies,
                             long totalApplications,
                             long shortlistedApplications,
                             long selectedApplications) {
        this.totalStudents = totalStudents;
        this.totalCompanies = totalCompanies;
        this.totalApplications = totalApplications;
        this.shortlistedApplications = shortlistedApplications;
        this.selectedApplications = selectedApplications;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalCompanies() {
        return totalCompanies;
    }

    public void setTotalCompanies(long totalCompanies) {
        this.totalCompanies = totalCompanies;
    }

    public long getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(long totalApplications) {
        this.totalApplications = totalApplications;
    }

    public long getShortlistedApplications() {
        return shortlistedApplications;
    }

    public void setShortlistedApplications(long shortlistedApplications) {
        this.shortlistedApplications = shortlistedApplications;
    }

    public long getSelectedApplications() {
        return selectedApplications;
    }

    public void setSelectedApplications(long selectedApplications) {
        this.selectedApplications = selectedApplications;
    }
}
