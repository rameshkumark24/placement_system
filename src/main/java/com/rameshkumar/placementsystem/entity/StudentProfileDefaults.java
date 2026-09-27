package com.rameshkumar.placementsystem.entity;

/**
 * Values used for a student profile that was created automatically and not yet filled in.
 */
public final class StudentProfileDefaults {

    /** Placeholder stored in the skills column until the student updates their profile. */
    public static final String PLACEHOLDER_SKILLS = "Profile not updated";

    private StudentProfileDefaults() {
    }

    public static Student newEmptyProfile(User user) {
        Student profile = new Student();
        profile.setUser(user);
        profile.setCgpa(0.0);
        profile.setSkills(PLACEHOLDER_SKILLS);
        profile.setResumeLink(null);
        return profile;
    }

    public static boolean hasRealSkills(String skills) {
        return skills != null && !skills.isBlank() && !PLACEHOLDER_SKILLS.equals(skills.trim());
    }
}
