export const PROFILE_PLACEHOLDER_SKILLS = "Profile not updated";

export const APPLICATION_STATUSES = ["APPLIED", "SHORTLISTED", "SELECTED", "REJECTED"];

export function todayIso() {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);
}

function isoToUtc(iso) {
  const [year, month, day] = iso.split("-").map(Number);
  return Date.UTC(year, month - 1, day);
}

/** Whole days from today until the ISO date (negative once it has passed). */
export function daysUntil(iso) {
  if (!iso) {
    return null;
  }
  return Math.round((isoToUtc(iso) - isoToUtc(todayIso())) / 86_400_000);
}

export function formatDate(iso) {
  if (!iso) {
    return "-";
  }
  const [year, month, day] = iso.split("-").map(Number);
  return new Date(year, month - 1, day).toLocaleDateString(undefined, {
    day: "numeric",
    month: "short",
    year: "numeric"
  });
}

export function deadlineLabel(iso) {
  const days = daysUntil(iso);
  if (days === null) return "";
  if (days < 0) return "Closed";
  if (days === 0) return "Closes today";
  if (days === 1) return "Closes tomorrow";
  return `Closes in ${days} days`;
}

export function isClosed(company) {
  const days = daysUntil(company.deadline);
  return days !== null && days < 0;
}

export function displaySkills(skills) {
  return skills && skills !== PROFILE_PLACEHOLDER_SKILLS ? skills : "";
}

export function skillList(skills) {
  return displaySkills(skills)
    .split(/[,;\n]/)
    .map((skill) => skill.trim())
    .filter(Boolean);
}

export function initials(nameOrEmail = "") {
  const words = nameOrEmail.split("@")[0].split(/[\s._-]+/).filter(Boolean);
  return (words.slice(0, 2).map((word) => word[0]).join("") || "?").toUpperCase();
}

export function packageOf(company) {
  return company.packageOffered ?? company.package;
}

/** Which profile fields are filled in; eligibility is based on the saved profile. */
export function profileChecklist(profile) {
  return [
    { key: "cgpa", label: "CGPA", done: Number(profile?.cgpa) > 0 },
    { key: "skills", label: "Skills", done: Boolean(displaySkills(profile?.skills).trim()) },
    { key: "resumeLink", label: "Resume link", done: Boolean(profile?.resumeLink?.trim()) }
  ];
}

export function isProfileComplete(profile) {
  return profileChecklist(profile).every((item) => item.done);
}

export function companyEligibility(company, { profile, appliedCompanyIds }) {
  if (appliedCompanyIds.has(company.id)) {
    return { canApply: false, label: "Already applied", action: "Applied", tone: "info", group: "applied" };
  }
  if (isClosed(company)) {
    return { canApply: false, label: "Deadline passed", action: "Closed", tone: "danger", group: "closed" };
  }
  if (!isProfileComplete(profile)) {
    return { canApply: false, label: "Complete your profile first", action: "Complete profile", tone: "warn", group: "blocked" };
  }
  if (Number(profile.cgpa) < Number(company.eligibilityCgpa)) {
    return { canApply: false, label: `Requires CGPA ${company.eligibilityCgpa}`, action: "Not eligible", tone: "danger", group: "blocked" };
  }
  return { canApply: true, label: "You are eligible", action: "Apply now", tone: "ok", group: "eligible" };
}

export function matchesQuery(query, ...values) {
  const needle = query.trim().toLowerCase();
  if (!needle) {
    return true;
  }
  return values.some((value) => String(value ?? "").toLowerCase().includes(needle));
}
