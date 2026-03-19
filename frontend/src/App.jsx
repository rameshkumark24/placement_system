import { useEffect, useMemo, useState } from "react";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const SESSION_KEY = "placement-system-session";

function readSession() {
  const raw = window.localStorage.getItem(SESSION_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

function writeSession(session) {
  if (!session) {
    window.localStorage.removeItem(SESSION_KEY);
    return;
  }
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

function parseJwt(token) {
  try {
    const payload = JSON.parse(window.atob(token.split(".")[1]));
    return {
      email: payload.sub,
      role: payload.role
    };
  } catch {
    return null;
  }
}

async function apiRequest(path, method = "GET", body, token) {
  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: body ? JSON.stringify(body) : undefined
  });

  const payload = await response.json().catch(() => ({}));

  if (!response.ok || payload.success === false) {
    throw new Error(payload.message || "Request failed");
  }

  return payload.data;
}

function buildSession(authResponse) {
  const user = parseJwt(authResponse.token);
  if (!user) {
    throw new Error("Received invalid access token");
  }

  return {
    token: authResponse.token,
    refreshToken: authResponse.refreshToken,
    expiresInMs: authResponse.expiresInMs,
    user
  };
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
  return Number(profile.cgpa) > 0 && Boolean(profile.skills?.trim()) && Boolean(profile.resumeLink?.trim());
}

export default function App() {
  const [session, setSession] = useState(readSession);
  const [mode, setMode] = useState("login");
  const [authForm, setAuthForm] = useState({ name: "", email: "", password: "" });
  const [authLoading, setAuthLoading] = useState(false);
  const [authError, setAuthError] = useState("");
  const [students, setStudents] = useState([]);
  const [companies, setCompanies] = useState([]);
  const [applications, setApplications] = useState([]);
  const [dashboardStats, setDashboardStats] = useState(null);
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
  const isStudent = currentUser?.role === "STUDENT";
  const profileComplete = isProfileComplete(profileForm);

  const stats = useMemo(
    () => isAdmin ? [
      { label: "Students", value: dashboardStats?.totalStudents ?? students.length, accent: "var(--sun)" },
      { label: "Companies", value: dashboardStats?.totalCompanies ?? companies.length, accent: "var(--mint)" },
      { label: "Applications", value: dashboardStats?.totalApplications ?? applications.length, accent: "var(--sky)" },
      { label: "Shortlisted", value: dashboardStats?.shortlistedApplications ?? 0, accent: "var(--mint)" },
      { label: "Selected", value: dashboardStats?.selectedApplications ?? 0, accent: "var(--sky)" }
    ] : [
      { label: "Companies", value: companies.length, accent: "var(--mint)" },
      { label: "Applications", value: applications.length, accent: "var(--sky)" }
    ],
    [applications.length, companies.length, dashboardStats, isAdmin, students.length]
  );

  useEffect(() => {
    writeSession(session);
  }, [session]);

  useEffect(() => {
    if (!session?.token) {
      return;
    }
    void refreshData();
  }, [session?.token]);

  async function refreshSessionTokens(currentSession) {
    if (!currentSession?.refreshToken) {
      throw new Error("Your session has expired. Please sign in again.");
    }

    try {
      const data = await apiRequest("/auth/refresh", "POST", {
        refreshToken: currentSession.refreshToken
      });
      const updatedSession = buildSession(data);
      setSession(updatedSession);
      return updatedSession;
    } catch {
      throw new Error("Your session has expired. Please sign in again.");
    }
  }

  async function authorizedRequest(path, method = "GET", body, attemptRefresh = true) {
    const activeSession = session || readSession();
    const accessToken = activeSession?.token;

    try {
      return await apiRequest(path, method, body, accessToken);
    } catch (error) {
      const shouldRefresh = attemptRefresh
        && activeSession?.refreshToken
        && error.message === "JWT token has expired";

      if (!shouldRefresh) {
        if (error.message === "JWT token has expired" || error.message === "Invalid JWT token") {
          logout("Your session has expired. Please sign in again.");
        }
        throw error;
      }

      const updatedSession = await refreshSessionTokens(activeSession);
      return apiRequest(path, method, body, updatedSession.token);
    }
  }

  async function refreshData() {
    if (!session?.token) {
      return;
    }

    setPageState((current) => ({ ...current, loading: true, error: "" }));
    try {
      if (isAdmin) {
        const [studentData, companyData, applicationData] = await Promise.all([
          authorizedRequest(buildStudentQuery()),
          authorizedRequest(buildCompanyQuery()),
          authorizedRequest(buildApplicationQuery())
        ]);

        const statsData = await authorizedRequest("/dashboard/stats");

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
        setProfileForm({
          name: profileData?.name || "",
          cgpa: profileData?.cgpa ?? "",
          skills: profileData?.skills || "",
          resumeLink: profileData?.resumeLink || ""
        });
      }
      setPageState((current) => ({ ...current, loading: false }));
    } catch (error) {
      setPageState({ loading: false, error: error.message, notice: "" });
    }
  }

  function buildStudentQuery() {
    const params = new URLSearchParams();
    if (filters.studentSkill) {
      params.set("skill", filters.studentSkill);
    }
    if (filters.studentCgpa) {
      params.set("cgpa", filters.studentCgpa);
    }
    const queryString = params.toString();
    return queryString ? `/students?${queryString}` : "/students";
  }

  function buildCompanyQuery() {
    if (filters.companyRole) {
      return `/companies?role=${encodeURIComponent(filters.companyRole)}`;
    }
    return "/companies";
  }

  function buildApplicationQuery() {
    const params = new URLSearchParams();
    if (filters.applicationCompany) {
      params.set("company", filters.applicationCompany);
    }
    if (filters.applicationStatus) {
      params.set("status", filters.applicationStatus);
    }
    if (filters.applicationStudentEmail) {
      params.set("studentEmail", filters.applicationStudentEmail);
    }
    const queryString = params.toString();
    return queryString ? `/applications?${queryString}` : "/applications";
  }

  async function handleAuthSubmit(event) {
    event.preventDefault();
    setAuthLoading(true);
    setAuthError("");

    try {
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
        setSession(buildSession(data));
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

  async function handleDeleteStudent(id) {
    await guardedAction(async () => {
      await authorizedRequest(`/students/${id}`, "DELETE");
      return "Student removed.";
    });
  }

  async function handleDeleteCompany(id) {
    await guardedAction(async () => {
      await authorizedRequest(`/companies/${id}`, "DELETE");
      if (editingCompanyId === id) {
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

  async function guardedAction(action) {
    setPageState((current) => ({ ...current, loading: true, error: "", notice: "" }));
    try {
      const notice = await action();
      await refreshData();
      setPageState({ loading: false, error: "", notice });
    } catch (error) {
      setPageState({ loading: false, error: error.message, notice: "" });
    }
  }

  function logout(notice = "You have been signed out.") {
    setSession(null);
    setStudents([]);
    setCompanies([]);
    setApplications([]);
    setDashboardStats(null);
    setProfileForm(emptyProfileForm);
    setPageState({ loading: false, error: "", notice });
  }

  if (!session?.token) {
    return (
      <main className="auth-shell">
        <section className="auth-hero">
          <span className="eyebrow">Placement Frontend</span>
          <h1>Placement control room for students, companies, and admins.</h1>
          <p>
            React client for the Spring Boot backend. Students can browse companies and apply.
            Admins can manage the entire placement pipeline from one workspace.
          </p>
          <div className="hero-grid">
            <div>
              <strong>JWT secured</strong>
              <span>Role-aware dashboard with API wrapper support.</span>
            </div>
            <div>
              <strong>Workflow ready</strong>
              <span>Companies, applications, search, filtering, and rate-limited requests.</span>
            </div>
          </div>
        </section>

        <section className="auth-panel">
          <div className="tab-row">
            <button className={mode === "login" ? "active" : ""} onClick={() => setMode("login")}>Login</button>
            <button className={mode === "register" ? "active" : ""} onClick={() => setMode("register")}>Register</button>
          </div>

          <form className="card-form" onSubmit={handleAuthSubmit}>
            {mode === "register" ? (
              <label>
                Name
                <input
                  value={authForm.name}
                  onChange={(event) => setAuthForm({ ...authForm, name: event.target.value })}
                  placeholder="Ramesh Kumar"
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
                required
              />
            </label>

            <label>
              Password
              <input
                type="password"
                value={authForm.password}
                onChange={(event) => setAuthForm({ ...authForm, password: event.target.value })}
                placeholder="Enter password"
                required
              />
            </label>

            {authError ? <p className="error-text">{authError}</p> : null}
            {pageState.notice ? <p className="notice-text">{pageState.notice}</p> : null}

            <button className="primary-button" type="submit" disabled={authLoading}>
              {authLoading ? "Working..." : mode === "login" ? "Sign In" : "Create Account"}
            </button>
          </form>

          <p className="helper-text">
            Registration creates a `STUDENT` account. Use an existing admin account for company and student management.
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
          </div>
          <button className="ghost-button" onClick={logout}>Logout</button>
        </div>
      </header>

      {pageState.error ? <div className="banner error-banner">{pageState.error}</div> : null}
      {pageState.notice ? <div className="banner notice-banner">{pageState.notice}</div> : null}

      <section className="stats-grid">
        {stats
          .filter((item) => (isStudent ? item.label !== "Students" : true))
          .map((item) => (
            <article key={item.label} className="stat-card" style={{ "--accent": item.accent }}>
              <span>{item.label}</span>
              <strong>{item.value}</strong>
            </article>
          ))}
      </section>

      <section className="layout-grid">
        {isAdmin ? (
          <>
            <Panel title="Create Student" subtitle="Creates both the auth user and the student profile.">
              <form className="card-form compact" onSubmit={handleCreateStudent}>
                <FormInput label="Name" value={studentForm.name} onChange={(value) => setStudentForm({ ...studentForm, name: value })} />
                <FormInput label="Email" type="email" value={studentForm.email} onChange={(value) => setStudentForm({ ...studentForm, email: value })} />
                <FormInput label="Password" type="password" value={studentForm.password} onChange={(value) => setStudentForm({ ...studentForm, password: value })} />
                <FormInput label="CGPA" type="number" value={studentForm.cgpa} onChange={(value) => setStudentForm({ ...studentForm, cgpa: value })} />
                <FormInput label="Skills" value={studentForm.skills} onChange={(value) => setStudentForm({ ...studentForm, skills: value })} />
                <FormInput label="Resume Link" value={studentForm.resumeLink} onChange={(value) => setStudentForm({ ...studentForm, resumeLink: value })} />
                <button className="primary-button" type="submit" disabled={pageState.loading}>Add Student</button>
              </form>
            </Panel>

            <Panel
              title={editingCompanyId ? "Edit Company" : "Post Company"}
              subtitle="Admin-only company management with role and package details."
            >
              <form className="card-form compact" onSubmit={handleCreateCompany}>
                <FormInput label="Company Name" value={companyForm.name} onChange={(value) => setCompanyForm({ ...companyForm, name: value })} />
                <FormInput label="Role" value={companyForm.role} onChange={(value) => setCompanyForm({ ...companyForm, role: value })} />
                <FormInput label="Package" type="number" value={companyForm.package} onChange={(value) => setCompanyForm({ ...companyForm, package: value })} />
                <FormInput label="Eligibility CGPA" type="number" value={companyForm.eligibilityCgpa} onChange={(value) => setCompanyForm({ ...companyForm, eligibilityCgpa: value })} />
                <FormInput label="Deadline" type="date" value={companyForm.deadline} onChange={(value) => setCompanyForm({ ...companyForm, deadline: value })} />
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
                <div className="toolbar">
                <FormInput label="Skill Search" value={filters.studentSkill} onChange={(value) => setFilters({ ...filters, studentSkill: value })} />
                <FormInput label="Min CGPA" type="number" value={filters.studentCgpa} onChange={(value) => setFilters({ ...filters, studentCgpa: value })} />
                <button className="ghost-button" onClick={() => refreshData()}>Refresh</button>
                </div>
              <DataTable
                columns={["Name", "Email", "CGPA", "Skills", "Actions"]}
                rows={students.map((student) => [
                  student.name,
                  student.email,
                  student.cgpa,
                  student.skills,
                  <button key={student.id} className="table-action" onClick={() => handleDeleteStudent(student.id)}>Delete</button>
                ])}
              />
            </Panel>

            <Panel title="Company Pipeline" subtitle="Filter companies by role or remove outdated opportunities.">
              <div className="toolbar">
                <FormInput label="Role Filter" value={filters.companyRole} onChange={(value) => setFilters({ ...filters, companyRole: value })} />
                <button className="ghost-button" onClick={() => refreshData()}>Refresh</button>
              </div>
              <DataTable
                columns={["Name", "Role", "Package", "CGPA", "Deadline", "Actions"]}
                rows={companies.map((company) => [
                  company.name,
                  company.role,
                  company.packageOffered ?? company.package,
                  company.eligibilityCgpa,
                  company.deadline,
                  <div key={company.id} className="action-row">
                    <button className="table-action" onClick={() => handleEditCompany(company)}>Edit</button>
                    <button className="table-action" onClick={() => handleDeleteCompany(company.id)}>Delete</button>
                  </div>
                ])}
              />
            </Panel>

            <Panel title="Application Feed" subtitle="Admin visibility across all student applications.">
              <div className="toolbar">
                <FormInput label="Company Filter" value={filters.applicationCompany} onChange={(value) => setFilters({ ...filters, applicationCompany: value })} />
                <FormInput label="Status Filter" value={filters.applicationStatus} onChange={(value) => setFilters({ ...filters, applicationStatus: value })} />
                <FormInput label="Student Email" type="email" value={filters.applicationStudentEmail} onChange={(value) => setFilters({ ...filters, applicationStudentEmail: value })} />
                <button className="ghost-button" onClick={() => refreshData()}>Refresh</button>
              </div>
              <DataTable
                columns={["Student", "Company", "Status", "Applied", "Actions"]}
                rows={applications.map((application) => [
                  application.studentEmail,
                  application.companyName,
                  <StatusPill key={`${application.id}-status`} status={application.status} />,
                  application.appliedDate,
                  <select
                    key={`${application.id}-select`}
                    className="status-select"
                    value={application.status}
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
                <FormInput label="Name" value={profileForm.name} onChange={(value) => setProfileForm({ ...profileForm, name: value })} />
                <FormInput label="CGPA" type="number" value={profileForm.cgpa} onChange={(value) => setProfileForm({ ...profileForm, cgpa: value })} />
                <FormInput label="Skills" value={profileForm.skills} onChange={(value) => setProfileForm({ ...profileForm, skills: value })} />
                <FormInput label="Resume Link" value={profileForm.resumeLink} onChange={(value) => setProfileForm({ ...profileForm, resumeLink: value })} />
                <button className="primary-button" type="submit" disabled={pageState.loading}>Save Profile</button>
              </form>
              {!profileComplete ? (
                <p className="helper-text">
                  Complete CGPA, skills, and resume link to unlock job applications.
                </p>
              ) : null}
            </Panel>

            <Panel title="Open Opportunities" subtitle="Browse companies and apply directly.">
              <div className="toolbar">
                <FormInput label="Role Filter" value={filters.companyRole} onChange={(value) => setFilters({ ...filters, companyRole: value })} />
                <button className="ghost-button" onClick={() => refreshData()}>Refresh</button>
              </div>
              <div className="company-grid">
                {companies.map((company) => (
                  <article key={company.id} className="company-card">
                    <span className="eyebrow">{company.role}</span>
                    <h3>{company.name}</h3>
                    <dl>
                      <div><dt>Package</dt><dd>{company.packageOffered ?? company.package}</dd></div>
                      <div><dt>Eligibility</dt><dd>{company.eligibilityCgpa}</dd></div>
                      <div><dt>Deadline</dt><dd>{company.deadline}</dd></div>
                    </dl>
                    <button className="primary-button" onClick={() => handleApply(company.id)} disabled={!profileComplete}>
                      {profileComplete ? "Apply Now" : "Complete Profile First"}
                    </button>
                  </article>
                ))}
              </div>
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
                <FormInput label="Company ID" value={applyCompanyId} onChange={setApplyCompanyId} />
                <button className="primary-button" type="submit" disabled={!profileComplete}>Apply</button>
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

function FormInput({ label, type = "text", value, onChange }) {
  return (
    <label>
      {label}
      <input type={type} value={value} onChange={(event) => onChange(event.target.value)} />
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
  return <span className={`status-pill status-${status.toLowerCase()}`}>{status}</span>;
}
