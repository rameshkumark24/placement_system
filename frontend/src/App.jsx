import { useEffect, useMemo, useRef, useState } from "react";
import { apiRequest, getBackendState, wakeBackend } from "./api";
import BackendStatus from "./components/BackendStatus";
import { useBackendStatus } from "./useBackendStatus";

const SESSION_KEY = "placement-system-session";
const SESSION_EXPIRED = "Your session has expired. Please sign in again.";
const PROFILE_PLACEHOLDER_SKILLS = "Profile not updated";
const TOKEN_REFRESH_MARGIN_MS = 30_000;
const NOTICE_TIMEOUT_MS = 5_000;

function readSession() {
  try {
    const raw = window.localStorage.getItem(SESSION_KEY);
    const session = raw ? JSON.parse(raw) : null;
    return session?.token && session?.user?.role ? session : null;
  } catch {
    return null;
  }
}

function writeSession(session) {
  try {
    if (!session) {
      window.localStorage.removeItem(SESSION_KEY);
      return;
    }
    window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  } catch {
    // Storage can be unavailable (private mode); the session then lasts for this tab only.
  }
}

// JWTs are base64url encoded; window.atob only understands standard base64 with padding.
function parseJwt(token) {
  try {
    const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
    const payload = JSON.parse(window.atob(padded));
    return {
      email: payload.sub,
      role: payload.role,
      expiresAt: payload.exp ? payload.exp * 1000 : null
    };
  } catch {
    return null;
  }
}

function buildSession(authResponse) {
  const claims = parseJwt(authResponse.token);
  if (!claims?.email || !claims?.role) {
    throw new Error("Received invalid access token");
  }

  return {
    token: authResponse.token,
    refreshToken: authResponse.refreshToken,
    expiresAt: claims.expiresAt,
    user: { email: claims.email, role: claims.role }
  };
}

function todayIso() {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);
}

function displaySkills(skills) {
  return skills && skills !== PROFILE_PLACEHOLDER_SKILLS ? skills : "";
}

const emptyStudentForm = {
  name: "",
  email: "",
  password: "",
  cgpa: "",
  skills: "",
  resumeLink: ""
};

const emptyCompanyForm = {
  name: "",
  role: "",
  package: "",
  eligibilityCgpa: "",
  deadline: ""
};

const emptyProfileForm = {
  name: "",
  cgpa: "",
  skills: "",
  resumeLink: ""
};

const applicationStatuses = ["APPLIED", "SHORTLISTED", "REJECTED", "SELECTED"];

function isProfileComplete(profile) {
  return Boolean(profile)
    && Number(profile.cgpa) > 0
    && Boolean(displaySkills(profile.skills).trim())
    && Boolean(profile.resumeLink?.trim());
}

export default function App() {
  const backend = useBackendStatus();
  const [session, setSessionState] = useState(readSession);
  const sessionRef = useRef(session);
  const refreshPromiseRef = useRef(null);
  const [mode, setMode] = useState("login");
  const [authForm, setAuthForm] = useState({ name: "", email: "", password: "" });
  const [authLoading, setAuthLoading] = useState(false);
  const [authError, setAuthError] = useState("");
  const [students, setStudents] = useState([]);
  const [companies, setCompanies] = useState([]);
  const [applications, setApplications] = useState([]);
  const [dashboardStats, setDashboardStats] = useState(null);
  const [profile, setProfile] = useState(null);
  const [profileForm, setProfileForm] = useState(emptyProfileForm);
  const [filters, setFilters] = useState({
    studentSkill: "",
    studentCgpa: "",
    companyRole: "",
    applicationCompany: "",
    applicationStatus: "",
    applicationStudentEmail: ""
  });
  const [studentForm, setStudentForm] = useState(emptyStudentForm);
  const [companyForm, setCompanyForm] = useState(emptyCompanyForm);
  const [editingCompanyId, setEditingCompanyId] = useState(null);
  const [applyCompanyId, setApplyCompanyId] = useState("");
  const [pageState, setPageState] = useState({ loading: false, error: "", notice: "" });

  const currentUser = session?.user || null;
  const isAdmin = currentUser?.role === "ADMIN";
  const sessionIdentity = currentUser ? `${currentUser.email}|${currentUser.role}` : "";
  // Eligibility is based on the saved profile, not on unsaved edits in the form.
  const profileComplete = isProfileComplete(profile);
  const appliedCompanyIds = useMemo(
    () => new Set(applications.map((application) => application.companyId)),
    [applications]
  );

  function companyEligibility(company) {
    if (appliedCompanyIds.has(company.id)) {
      return { canApply: false, label: "Already applied", action: "Applied", tone: "done" };
    }
    if (company.deadline && company.deadline < todayIso()) {
      return { canApply: false, label: "Deadline passed", action: "Closed", tone: "closed" };
    }
    if (!profileComplete) {
      return { canApply: false, label: "Complete your profile first", action: "Complete Profile First", tone: "warn" };
    }
    if (Number(profile.cgpa) < Number(company.eligibilityCgpa)) {
      return { canApply: false, label: `Requires CGPA ${company.eligibilityCgpa}`, action: "Not Eligible", tone: "closed" };
    }
    return { canApply: true, label: "You are eligible", action: "Apply Now", tone: "ok" };
  }

  const openForYou = isAdmin ? 0 : companies.filter((company) => companyEligibility(company).canApply).length;

  const stats = isAdmin ? [
    { label: "Students", value: dashboardStats?.totalStudents ?? students.length, accent: "var(--sun)" },
    { label: "Companies", value: dashboardStats?.totalCompanies ?? companies.length, accent: "var(--mint)" },
    { label: "Applications", value: dashboardStats?.totalApplications ?? applications.length, accent: "var(--sky)" },
    { label: "Shortlisted", value: dashboardStats?.shortlistedApplications ?? 0, accent: "var(--mint)" },
    { label: "Selected", value: dashboardStats?.selectedApplications ?? 0, accent: "var(--sky)" }
  ] : [
    { label: "Companies", value: companies.length, accent: "var(--mint)" },
    { label: "Open For You", value: openForYou, accent: "var(--sun)" },
    { label: "My Applications", value: applications.length, accent: "var(--sky)" }
  ];

  useEffect(() => {
    if (sessionIdentity) {
      void refreshData();
    }
    // Reload only when the signed-in identity changes, not on every token refresh.
  }, [sessionIdentity]);

  useEffect(() => {
    if (!pageState.notice) {
      return undefined;
    }
    const timer = setTimeout(() => setPageState((current) => ({ ...current, notice: "" })), NOTICE_TIMEOUT_MS);
    return () => clearTimeout(timer);
  }, [pageState.notice]);

  function updateSession(next) {
    sessionRef.current = next;
    writeSession(next);
    setSessionState(next);
  }

  // Concurrent requests that all see an expired token share a single refresh call.
  function refreshTokens() {
    if (!refreshPromiseRef.current) {
      const current = sessionRef.current;
      refreshPromiseRef.current = (async () => {
        if (!current?.refreshToken) {
          logout(SESSION_EXPIRED);
          throw new Error(SESSION_EXPIRED);
        }
        try {
          const data = await apiRequest("/auth/refresh", "POST", { refreshToken: current.refreshToken });
          const next = buildSession(data);
          updateSession(next);
          return next;
        } catch (error) {
          // A network failure (e.g. the server is waking up) should not sign the user out.
          if (!error.status) {
            throw error;
          }
          logout(SESSION_EXPIRED);
          throw new Error(SESSION_EXPIRED);
        }
      })().finally(() => {
        refreshPromiseRef.current = null;
      });
    }
    return refreshPromiseRef.current;
  }

  async function authorizedRequest(path, method = "GET", body) {
    let active = sessionRef.current;
    if (!active?.token) {
      throw new Error(SESSION_EXPIRED);
    }
    if (active.expiresAt && active.expiresAt - Date.now() < TOKEN_REFRESH_MARGIN_MS) {
      active = await refreshTokens();
    }

    try {
      return await apiRequest(path, method, body, active.token);
    } catch (error) {
      if (error.status !== 401) {
        throw error;
      }
      if (error.message === "JWT token has expired" && active.refreshToken) {
        const refreshed = await refreshTokens();
        return apiRequest(path, method, body, refreshed.token);
      }
      logout(SESSION_EXPIRED);
      throw new Error(SESSION_EXPIRED);
    }
  }

  async function loadData() {
    const active = sessionRef.current;
    if (!active?.token) {
      return;
    }

    if (active.user.role === "ADMIN") {
      const [studentData, companyData, applicationData, statsData] = await Promise.all([
        authorizedRequest(buildStudentQuery()),
        authorizedRequest(buildCompanyQuery()),
        authorizedRequest(buildApplicationQuery()),
        authorizedRequest("/dashboard/stats")
      ]);

      setStudents(studentData || []);
      setCompanies(companyData || []);
      setApplications(applicationData || []);
      setDashboardStats(statsData);
    } else {
      const [companyData, applicationData, profileData] = await Promise.all([
        authorizedRequest(buildCompanyQuery()),
        authorizedRequest("/applications/my"),
        authorizedRequest("/students/me")
      ]);

      setCompanies(companyData || []);
      setApplications(applicationData || []);
      setDashboardStats(null);
      setProfile(profileData);
      setProfileForm({
        name: profileData?.name || "",
        cgpa: profileData?.cgpa ? String(profileData.cgpa) : "",
        skills: displaySkills(profileData?.skills),
        resumeLink: profileData?.resumeLink || ""
      });
    }
  }

  async function refreshData() {
    setPageState((current) => ({ ...current, loading: true, error: "" }));
    try {
      await loadData();
      setPageState((current) => ({ ...current, loading: false }));
    } catch (error) {
      setPageState({ loading: false, error: error.message, notice: "" });
    }
  }

  function buildStudentQuery() {
    const params = new URLSearchParams();
    if (filters.studentSkill.trim()) {
      params.set("skill", filters.studentSkill.trim());
    }
    if (filters.studentCgpa) {
      params.set("cgpa", filters.studentCgpa);
    }
    const queryString = params.toString();
    return queryString ? `/students?${queryString}` : "/students";
  }

  function buildCompanyQuery() {
    if (filters.companyRole.trim()) {
      return `/companies?role=${encodeURIComponent(filters.companyRole.trim())}`;
    }
    return "/companies";
  }

  function buildApplicationQuery() {
    const params = new URLSearchParams();
    if (filters.applicationCompany.trim()) {
      params.set("company", filters.applicationCompany.trim());
    }
    if (filters.applicationStatus) {
      params.set("status", filters.applicationStatus);
    }
    if (filters.applicationStudentEmail.trim()) {
      params.set("studentEmail", filters.applicationStudentEmail.trim());
    }
    const queryString = params.toString();
    return queryString ? `/applications?${queryString}` : "/applications";
  }

  async function handleAuthSubmit(event) {
    event.preventDefault();
    setAuthLoading(true);
    setAuthError("");

    try {
      if (getBackendState().status !== "online" && !(await wakeBackend())) {
        throw new Error("The server is not reachable right now. Please try again in a moment.");
      }

      if (mode === "register") {
        await apiRequest("/auth/register", "POST", authForm);
        setMode("login");
        setAuthForm({ name: "", email: authForm.email, password: "" });
        setPageState({ loading: false, error: "", notice: "Registration complete. Log in to continue." });
      } else {
        const data = await apiRequest("/auth/login", "POST", {
          email: authForm.email,
          password: authForm.password
        });
        updateSession(buildSession(data));
        setAuthForm({ name: "", email: "", password: "" });
        setPageState({ loading: false, error: "", notice: "Signed in successfully." });
      }
    } catch (error) {
      setAuthError(error.message);
    } finally {
      setAuthLoading(false);
    }
  }

  async function handleCreateStudent(event) {
    event.preventDefault();
    await guardedAction(async () => {
      await authorizedRequest("/students", "POST", {
        ...studentForm,
        cgpa: Number(studentForm.cgpa)
      });
      setStudentForm(emptyStudentForm);
      return "Student profile created.";
    });
  }

  async function handleCreateCompany(event) {
    event.preventDefault();
    await guardedAction(async () => {
      const payload = {
        ...companyForm,
        package: Number(companyForm.package),
        eligibilityCgpa: Number(companyForm.eligibilityCgpa)
      };

      if (editingCompanyId) {
        await authorizedRequest(`/companies/${editingCompanyId}`, "PUT", payload);
      } else {
        await authorizedRequest("/companies", "POST", payload);
      }

      setCompanyForm(emptyCompanyForm);
      setEditingCompanyId(null);
      return editingCompanyId ? "Company updated successfully." : "Company posted successfully.";
    });
  }

  async function handleDeleteStudent(student) {
    if (!window.confirm(`Delete ${student.name || student.email}? Their applications will be removed too.`)) {
      return;
    }
    await guardedAction(async () => {
      await authorizedRequest(`/students/${student.id}`, "DELETE");
      return "Student removed.";
    });
  }

  async function handleDeleteCompany(company) {
    if (!window.confirm(`Delete ${company.name}? All applications to this company will be removed too.`)) {
      return;
    }
    await guardedAction(async () => {
      await authorizedRequest(`/companies/${company.id}`, "DELETE");
      if (editingCompanyId === company.id) {
        setCompanyForm(emptyCompanyForm);
        setEditingCompanyId(null);
      }
      return "Company removed.";
    });
  }

  function handleEditCompany(company) {
    setEditingCompanyId(company.id);
    setCompanyForm({
      name: company.name || "",
      role: company.role || "",
      package: company.packageOffered ?? company.package ?? "",
      eligibilityCgpa: company.eligibilityCgpa ?? "",
      deadline: company.deadline || ""
    });
  }

  function handleCancelCompanyEdit() {
    setEditingCompanyId(null);
    setCompanyForm(emptyCompanyForm);
  }

  async function handleApply(companyId) {
    await guardedAction(async () => {
      await authorizedRequest(`/applications/apply/${companyId}`, "POST");
      setApplyCompanyId("");
      return "Application submitted.";
    });
  }

  async function handleUpdateProfile(event) {
    event.preventDefault();
    await guardedAction(async () => {
      await authorizedRequest("/students/me", "PUT", {
        name: profileForm.name,
        cgpa: Number(profileForm.cgpa),
        skills: profileForm.skills,
        resumeLink: profileForm.resumeLink
      });
      return "Profile updated successfully.";
    });
  }

  async function handleUpdateApplicationStatus(applicationId, status) {
    await guardedAction(async () => {
      await authorizedRequest(`/applications/${applicationId}/status`, "PUT", { status });
      return "Application status updated.";
    });
  }

  function handleFilterSubmit(event) {
    event.preventDefault();
    void refreshData();
  }

  async function guardedAction(action) {
    setPageState((current) => ({ ...current, loading: true, error: "", notice: "" }));
    let notice;
    try {
      notice = await action();
    } catch (error) {
      setPageState({ loading: false, error: error.message, notice: "" });
      return;
    }

    try {
      await loadData();
      setPageState({ loading: false, error: "", notice });
    } catch (error) {
      setPageState({ loading: false, error: `${notice} Reloading the latest data failed: ${error.message}`, notice: "" });
    }
  }

  function logout(notice = "You have been signed out.") {
    updateSession(null);
    setStudents([]);
    setCompanies([]);
    setApplications([]);
    setDashboardStats(null);
    setProfile(null);
    setProfileForm(emptyProfileForm);
    setEditingCompanyId(null);
    setCompanyForm(emptyCompanyForm);
    setPageState({ loading: false, error: "", notice });
  }

  if (!session?.token) {
    const waitingForServer = authLoading && backend.status !== "online";
    return (
      <main className="auth-shell">
        <section className="auth-hero">
          <span className="eyebrow">Placement Management System</span>
          <h1>Placement control room for students, companies, and admins.</h1>
          <p>
            Students build their profile, discover openings they are eligible for, and track every
            application. Admins manage the entire placement pipeline from one workspace.
          </p>
          <div className="hero-grid">
            <div>
              <strong>JWT secured</strong>
              <span>Role-aware dashboards with automatic session refresh.</span>
            </div>
            <div>
              <strong>Workflow ready</strong>
              <span>Eligibility checks, deadlines, search, filtering, and status tracking.</span>
            </div>
          </div>
        </section>

        <section className="auth-panel">
          <BackendStatus />

          <div className="tab-row">
            <button type="button" className={mode === "login" ? "active" : ""} onClick={() => { setMode("login"); setAuthError(""); }}>Login</button>
            <button type="button" className={mode === "register" ? "active" : ""} onClick={() => { setMode("register"); setAuthError(""); }}>Register</button>
          </div>

          <form className="card-form" onSubmit={handleAuthSubmit}>
            {mode === "register" ? (
              <label>
                Name
                <input
                  value={authForm.name}
                  onChange={(event) => setAuthForm({ ...authForm, name: event.target.value })}
                  placeholder="Your full name"
                  autoComplete="name"
                  maxLength={100}
                  required
                />
              </label>
            ) : null}

            <label>
              Email
              <input
                type="email"
                value={authForm.email}
                onChange={(event) => setAuthForm({ ...authForm, email: event.target.value })}
                placeholder="you@example.com"
                autoComplete="email"
                required
              />
            </label>

            <label>
              Password
              <input
                type="password"
                value={authForm.password}
                onChange={(event) => setAuthForm({ ...authForm, password: event.target.value })}
                placeholder={mode === "register" ? "At least 6 characters" : "Enter password"}
                autoComplete={mode === "register" ? "new-password" : "current-password"}
                minLength={mode === "register" ? 6 : undefined}
                maxLength={72}
                required
              />
            </label>

            {authError ? <p className="error-text">{authError}</p> : null}
            {pageState.notice ? <p className="notice-text">{pageState.notice}</p> : null}

            <button className="primary-button" type="submit" disabled={authLoading}>
              {waitingForServer
                ? "Waiting for server..."
                : authLoading
                  ? "Working..."
                  : mode === "login" ? "Sign In" : "Create Account"}
            </button>
          </form>

          <p className="helper-text">
            Registration creates a student account. Admins sign in with the account configured for the deployment.
          </p>
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <span className="eyebrow">Placement System</span>
          <h1>{isAdmin ? "Admin Placement Console" : "Student Placement Portal"}</h1>
        </div>
        <div className="user-panel">
          <div>
            <strong>{currentUser.email}</strong>
            <span>{currentUser.role}</span>
            <BackendStatus compact />
          </div>
          <button type="button" className="ghost-button" onClick={() => logout()}>Logout</button>
        </div>
      </header>

      {pageState.error ? <div className="banner error-banner" role="alert">{pageState.error}</div> : null}
      {pageState.notice ? <div className="banner notice-banner" role="status">{pageState.notice}</div> : null}
      {pageState.loading ? <div className="loading-bar" aria-hidden="true" /> : null}

      <section className="stats-grid">
        {stats.map((item) => (
          <article key={item.label} className="stat-card" style={{ "--accent": item.accent }}>
            <span>{item.label}</span>
            <strong>{item.value}</strong>
          </article>
        ))}
      </section>

      <section className="layout-grid">
        {isAdmin ? (
          <>
            <Panel title="Create Student" subtitle="Creates both the login account and the student profile.">
              <form className="card-form compact" onSubmit={handleCreateStudent}>
                <FormInput label="Name" value={studentForm.name} onChange={(value) => setStudentForm({ ...studentForm, name: value })} required maxLength={100} />
                <FormInput label="Email" type="email" value={studentForm.email} onChange={(value) => setStudentForm({ ...studentForm, email: value })} required />
                <FormInput label="Password" type="password" value={studentForm.password} onChange={(value) => setStudentForm({ ...studentForm, password: value })} required minLength={6} maxLength={72} autoComplete="new-password" />
                <FormInput label="CGPA" type="number" value={studentForm.cgpa} onChange={(value) => setStudentForm({ ...studentForm, cgpa: value })} required min={0} max={10} step="0.01" />
                <FormInput label="Skills" value={studentForm.skills} onChange={(value) => setStudentForm({ ...studentForm, skills: value })} required maxLength={255} placeholder="Java, Spring Boot, React" />
                <FormInput label="Resume Link" type="url" value={studentForm.resumeLink} onChange={(value) => setStudentForm({ ...studentForm, resumeLink: value })} maxLength={255} placeholder="https://..." />
                <button className="primary-button" type="submit" disabled={pageState.loading}>Add Student</button>
              </form>
            </Panel>

            <Panel
              title={editingCompanyId ? "Edit Company" : "Post Company"}
              subtitle="Admin-only company management with role, package, and eligibility details."
            >
              <form className="card-form compact" onSubmit={handleCreateCompany}>
                <FormInput label="Company Name" value={companyForm.name} onChange={(value) => setCompanyForm({ ...companyForm, name: value })} required maxLength={255} />
                <FormInput label="Role" value={companyForm.role} onChange={(value) => setCompanyForm({ ...companyForm, role: value })} required maxLength={255} />
                <FormInput label="Package (LPA)" type="number" value={companyForm.package} onChange={(value) => setCompanyForm({ ...companyForm, package: value })} required min={0} step="0.01" />
                <FormInput label="Eligibility CGPA" type="number" value={companyForm.eligibilityCgpa} onChange={(value) => setCompanyForm({ ...companyForm, eligibilityCgpa: value })} required min={0} max={10} step="0.01" />
                <FormInput label="Deadline" type="date" value={companyForm.deadline} onChange={(value) => setCompanyForm({ ...companyForm, deadline: value })} required min={todayIso()} />
                <button className="primary-button" type="submit" disabled={pageState.loading}>
                  {editingCompanyId ? "Update Company" : "Create Company"}
                </button>
                {editingCompanyId ? (
                  <button className="ghost-button" type="button" onClick={handleCancelCompanyEdit}>
                    Cancel Edit
                  </button>
                ) : null}
              </form>
            </Panel>

            <Panel title="Student Directory" subtitle="Search by skill or filter by minimum CGPA.">
              <form className="toolbar" onSubmit={handleFilterSubmit}>
                <FormInput label="Skill Search" value={filters.studentSkill} onChange={(value) => setFilters({ ...filters, studentSkill: value })} />
                <FormInput label="Min CGPA" type="number" value={filters.studentCgpa} onChange={(value) => setFilters({ ...filters, studentCgpa: value })} min={0} max={10} step="0.01" />
                <button className="ghost-button" type="submit" disabled={pageState.loading}>Search</button>
              </form>
              <DataTable
                columns={["Name", "Email", "CGPA", "Skills", "Resume", "Actions"]}
                rows={students.map((student) => [
                  student.name,
                  student.email,
                  student.cgpa,
                  displaySkills(student.skills) || <span className="muted">Not updated</span>,
                  student.resumeLink
                    ? <a key={`${student.id}-resume`} href={student.resumeLink} target="_blank" rel="noopener noreferrer">View</a>
                    : <span className="muted">Missing</span>,
                  <button key={student.id} type="button" className="table-action danger" onClick={() => handleDeleteStudent(student)} disabled={pageState.loading}>Delete</button>
                ])}
              />
            </Panel>

            <Panel title="Company Pipeline" subtitle="Filter companies by role or remove outdated opportunities.">
              <form className="toolbar" onSubmit={handleFilterSubmit}>
                <FormInput label="Role Filter" value={filters.companyRole} onChange={(value) => setFilters({ ...filters, companyRole: value })} />
                <button className="ghost-button" type="submit" disabled={pageState.loading}>Search</button>
              </form>
              <DataTable
                columns={["ID", "Name", "Role", "Package", "CGPA", "Deadline", "Actions"]}
                rows={companies.map((company) => [
                  company.id,
                  company.name,
                  company.role,
                  company.packageOffered ?? company.package,
                  company.eligibilityCgpa,
                  <span key={`${company.id}-deadline`} className={company.deadline < todayIso() ? "muted" : ""}>
                    {company.deadline}{company.deadline < todayIso() ? " (closed)" : ""}
                  </span>,
                  <div key={company.id} className="action-row">
                    <button type="button" className="table-action" onClick={() => handleEditCompany(company)}>Edit</button>
                    <button type="button" className="table-action danger" onClick={() => handleDeleteCompany(company)} disabled={pageState.loading}>Delete</button>
                  </div>
                ])}
              />
            </Panel>

            <Panel title="Application Feed" subtitle="Admin visibility across all student applications, newest first.">
              <form className="toolbar" onSubmit={handleFilterSubmit}>
                <FormInput label="Company Filter" value={filters.applicationCompany} onChange={(value) => setFilters({ ...filters, applicationCompany: value })} />
                <label>
                  Status Filter
                  <select value={filters.applicationStatus} onChange={(event) => setFilters({ ...filters, applicationStatus: event.target.value })}>
                    <option value="">All statuses</option>
                    {applicationStatuses.map((status) => (
                      <option key={status} value={status}>{status}</option>
                    ))}
                  </select>
                </label>
                <FormInput label="Student Email" value={filters.applicationStudentEmail} onChange={(value) => setFilters({ ...filters, applicationStudentEmail: value })} />
                <button className="ghost-button" type="submit" disabled={pageState.loading}>Search</button>
              </form>
              <DataTable
                columns={["Student", "Company", "Status", "Applied", "Update"]}
                rows={applications.map((application) => [
                  application.studentEmail,
                  application.companyName,
                  <StatusPill key={`${application.id}-status`} status={application.status} />,
                  application.appliedDate,
                  <select
                    key={`${application.id}-select`}
                    className="status-select"
                    value={application.status}
                    disabled={pageState.loading}
                    onChange={(event) => handleUpdateApplicationStatus(application.id, event.target.value)}
                  >
                    {applicationStatuses.map((status) => (
                      <option key={status} value={status}>{status}</option>
                    ))}
                  </select>
                ])}
              />
            </Panel>
          </>
        ) : (
          <>
            <Panel title="My Profile" subtitle="Complete your profile before applying to placement opportunities.">
              <form className="card-form compact" onSubmit={handleUpdateProfile}>
                <FormInput label="Name" value={profileForm.name} onChange={(value) => setProfileForm({ ...profileForm, name: value })} required maxLength={100} />
                <FormInput label="CGPA" type="number" value={profileForm.cgpa} onChange={(value) => setProfileForm({ ...profileForm, cgpa: value })} required min={0} max={10} step="0.01" placeholder="e.g. 8.25" />
                <FormInput label="Skills" value={profileForm.skills} onChange={(value) => setProfileForm({ ...profileForm, skills: value })} required maxLength={255} placeholder="Java, Spring Boot, React" />
                <FormInput label="Resume Link" type="url" value={profileForm.resumeLink} onChange={(value) => setProfileForm({ ...profileForm, resumeLink: value })} required maxLength={255} placeholder="https://drive.google.com/..." />
                <button className="primary-button" type="submit" disabled={pageState.loading}>Save Profile</button>
              </form>
              {!profileComplete ? (
                <p className="helper-text">
                  Save your CGPA, skills, and resume link to unlock job applications.
                </p>
              ) : null}
            </Panel>

            <Panel title="Open Opportunities" subtitle="Browse companies sorted by deadline and apply to the ones you are eligible for.">
              <form className="toolbar" onSubmit={handleFilterSubmit}>
                <FormInput label="Role Filter" value={filters.companyRole} onChange={(value) => setFilters({ ...filters, companyRole: value })} />
                <button className="ghost-button" type="submit" disabled={pageState.loading}>Search</button>
              </form>
              {companies.length ? (
                <div className="company-grid">
                  {companies.map((company) => {
                    const eligibility = companyEligibility(company);
                    return (
                      <article key={company.id} className="company-card">
                        <span className="eyebrow">{company.role}</span>
                        <h3>{company.name}</h3>
                        <dl>
                          <div><dt>Package</dt><dd>{company.packageOffered ?? company.package} LPA</dd></div>
                          <div><dt>Min CGPA</dt><dd>{company.eligibilityCgpa}</dd></div>
                          <div><dt>Deadline</dt><dd>{company.deadline}</dd></div>
                        </dl>
                        <span className={`eligibility eligibility-${eligibility.tone}`}>{eligibility.label}</span>
                        <button
                          type="button"
                          className="primary-button"
                          onClick={() => handleApply(company.id)}
                          disabled={!eligibility.canApply || pageState.loading}
                        >
                          {eligibility.action}
                        </button>
                      </article>
                    );
                  })}
                </div>
              ) : (
                <p className="empty-state">No openings right now. Check back soon.</p>
              )}
            </Panel>

            <Panel title="Quick Apply" subtitle="Use a company id when you already know the opening.">
              <form
                className="inline-form"
                onSubmit={(event) => {
                  event.preventDefault();
                  if (applyCompanyId) {
                    void handleApply(applyCompanyId);
                  }
                }}
              >
                <FormInput label="Company ID" type="number" value={applyCompanyId} onChange={setApplyCompanyId} min={1} step="1" required />
                <button className="primary-button" type="submit" disabled={!profileComplete || pageState.loading}>Apply</button>
              </form>
            </Panel>

            <Panel title="My Applications" subtitle="Track the status of your submitted applications.">
              <DataTable
                columns={["Company", "Status", "Applied"]}
                rows={applications.map((application) => [
                  application.companyName,
                  <StatusPill key={`${application.id}-student-status`} status={application.status} />,
                  application.appliedDate
                ])}
              />
            </Panel>
          </>
        )}
      </section>
    </main>
  );
}

function Panel({ title, subtitle, children }) {
  return (
    <section className="panel">
      <header className="panel-head">
        <div>
          <h2>{title}</h2>
          <p>{subtitle}</p>
        </div>
      </header>
      {children}
    </section>
  );
}

function FormInput({ label, type = "text", value, onChange, ...inputProps }) {
  return (
    <label>
      {label}
      <input type={type} value={value} onChange={(event) => onChange(event.target.value)} {...inputProps} />
    </label>
  );
}

function DataTable({ columns, rows }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column}>{column}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length ? (
            rows.map((row, index) => (
              <tr key={index}>
                {row.map((cell, cellIndex) => (
                  <td key={cellIndex}>{cell}</td>
                ))}
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={columns.length} className="empty-state">No records yet.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

function StatusPill({ status }) {
  const value = status || "UNKNOWN";
  return <span className={`status-pill status-${value.toLowerCase()}`}>{value}</span>;
}
