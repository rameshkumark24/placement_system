import { useMemo, useRef, useState } from "react";
import {
  Card,
  DataTable,
  EmptyState,
  Field,
  FilterChips,
  PasswordField,
  SearchInput,
  SelectField,
  StatCard,
  StatusPill
} from "../components/ui";
import {
  APPLICATION_STATUSES,
  deadlineLabel,
  displaySkills,
  formatDate,
  isClosed,
  matchesQuery,
  packageOf,
  skillList,
  todayIso
} from "../lib/format";

function plural(count, word) {
  return `${count} ${word}${count === 1 ? "" : "s"}`;
}

const emptyStudent = { name: "", email: "", password: "", cgpa: "", skills: "", resumeLink: "" };
const emptyCompany = { name: "", role: "", package: "", eligibilityCgpa: "", deadline: "" };

const STATUS_COLORS = {
  APPLIED: "var(--sun)",
  SHORTLISTED: "var(--mint)",
  SELECTED: "var(--sky)",
  REJECTED: "var(--rose)"
};

export default function AdminView({ tab, setTab, data, loaded, busy, actions }) {
  const applicantCounts = useMemo(() => {
    const counts = new Map();
    data.applications.forEach((application) => {
      counts.set(application.companyId, (counts.get(application.companyId) || 0) + 1);
    });
    return counts;
  }, [data.applications]);

  const applicationsByStudent = useMemo(() => {
    const counts = new Map();
    data.applications.forEach((application) => {
      counts.set(application.studentId, (counts.get(application.studentId) || 0) + 1);
    });
    return counts;
  }, [data.applications]);

  if (tab === "students") {
    return <StudentsTab data={data} loaded={loaded} busy={busy} actions={actions} applicationsByStudent={applicationsByStudent} />;
  }
  if (tab === "companies") {
    return <CompaniesTab data={data} loaded={loaded} busy={busy} actions={actions} applicantCounts={applicantCounts} />;
  }
  if (tab === "applications") {
    return <ApplicationsTab data={data} loaded={loaded} busy={busy} actions={actions} />;
  }
  return <Overview data={data} loaded={loaded} setTab={setTab} applicantCounts={applicantCounts} />;
}

function Overview({ data, loaded, setTab, applicantCounts }) {
  const { stats, applications, companies } = data;
  const byStatus = APPLICATION_STATUSES.map((status) => ({
    status,
    count: applications.filter((application) => application.status === status).length
  }));
  const total = applications.length;
  const openCompanies = companies.filter((company) => !isClosed(company));
  const upcoming = openCompanies.slice(0, 5);
  const recent = applications.slice(0, 5);
  const placementRate = total ? Math.round(((stats?.selectedApplications ?? 0) / total) * 100) : 0;

  return (
    <div className="stack">
      <section className="stats-grid">
        <StatCard label="Students" value={loaded ? stats?.totalStudents ?? 0 : "-"} accent="var(--sun)" />
        <StatCard label="Companies" value={loaded ? stats?.totalCompanies ?? 0 : "-"} accent="var(--mint)" hint={`${openCompanies.length} open now`} />
        <StatCard label="Applications" value={loaded ? stats?.totalApplications ?? 0 : "-"} accent="var(--sky)" />
        <StatCard label="Shortlisted" value={loaded ? stats?.shortlistedApplications ?? 0 : "-"} accent="var(--mint)" />
        <StatCard label="Selected" value={loaded ? stats?.selectedApplications ?? 0 : "-"} accent="var(--rose)" hint={total ? `${placementRate}% of applications` : undefined} />
      </section>

      <div className="split even">
        <Card title="Pipeline" subtitle="Where every application currently stands.">
          {total ? (
            <ul className="bars">
              {byStatus.map(({ status, count }) => (
                <li key={status}>
                  <div className="bar-label">
                    <StatusPill status={status} />
                    <span>{count} <span className="muted">({Math.round((count / total) * 100)}%)</span></span>
                  </div>
                  <div className="bar-track">
                    <span style={{ width: `${(count / total) * 100}%`, background: STATUS_COLORS[status] }} />
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <EmptyState title="No applications yet" message="The pipeline fills up as students apply." />
          )}
        </Card>

        <Card
          title="Closing soon"
          subtitle="Open companies by deadline."
          actions={<button type="button" className="button ghost small" onClick={() => setTab("companies")}>All companies</button>}
        >
          {upcoming.length ? (
            <ul className="list">
              {upcoming.map((company) => (
                <li key={company.id}>
                  <div>
                    <strong>{company.name}</strong>
                    <span className="muted small">{company.role}</span>
                  </div>
                  <div className="list-meta">
                    <span className="small">{deadlineLabel(company.deadline)}</span>
                    <span className="muted small">{plural(applicantCounts.get(company.id) || 0, "applicant")}</span>
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <EmptyState
              title="No open companies"
              action={<button type="button" className="button primary" onClick={() => setTab("companies")}>Post a company</button>}
            />
          )}
        </Card>
      </div>

      <Card
        title="Recent applications"
        actions={<button type="button" className="button ghost small" onClick={() => setTab("applications")}>View all</button>}
      >
        <DataTable
          loading={!loaded}
          rows={recent}
          columns={[
            { key: "studentEmail", label: "Student" },
            { key: "companyName", label: "Company" },
            { key: "appliedDate", label: "Applied", render: (row) => formatDate(row.appliedDate) },
            { key: "status", label: "Status", render: (row) => <StatusPill status={row.status} /> }
          ]}
          empty={<EmptyState title="No applications yet" />}
        />
      </Card>
    </div>
  );
}

function StudentsTab({ data, loaded, busy, actions, applicationsByStudent }) {
  const [query, setQuery] = useState("");
  const [minCgpa, setMinCgpa] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyStudent);

  const rows = data.students.filter((student) =>
    matchesQuery(query, student.name, student.email, displaySkills(student.skills))
    && (!minCgpa || Number(student.cgpa) >= Number(minCgpa)));

  async function handleSubmit(event) {
    event.preventDefault();
    if (await actions.createStudent(form)) {
      setForm(emptyStudent);
      setShowForm(false);
    }
  }

  return (
    <div className="stack">
      {showForm ? (
        <Card title="Add student" subtitle="Creates the login account and the student profile together.">
          <form className="form-grid" onSubmit={handleSubmit}>
            <Field label="Full name" value={form.name} onChange={(name) => setForm((current) => ({ ...current, name }))} required maxLength={100} />
            <Field label="Email" type="email" value={form.email} onChange={(email) => setForm((current) => ({ ...current, email }))} required />
            <PasswordField label="Initial password" value={form.password} onChange={(password) => setForm((current) => ({ ...current, password }))} required minLength={6} maxLength={72} autoComplete="new-password" />
            <Field label="CGPA" type="number" value={form.cgpa} onChange={(cgpa) => setForm((current) => ({ ...current, cgpa }))} required min={0} max={10} step="0.01" />
            <Field label="Skills" value={form.skills} onChange={(skills) => setForm((current) => ({ ...current, skills }))} required maxLength={255} placeholder="Java, Spring Boot, React" />
            <Field label="Resume link" type="url" value={form.resumeLink} onChange={(resumeLink) => setForm((current) => ({ ...current, resumeLink }))} maxLength={255} placeholder="https://..." />
            <div className="form-actions span-2">
              <button type="button" className="button ghost" onClick={() => { setShowForm(false); setForm(emptyStudent); }}>Cancel</button>
              <button type="submit" className="button primary" disabled={busy}>Add student</button>
            </div>
          </form>
        </Card>
      ) : null}

      <Card
        title="Students"
        subtitle={`${data.students.length} registered`}
        actions={!showForm ? <button type="button" className="button primary" onClick={() => setShowForm(true)}>Add student</button> : null}
      >
        <div className="toolbar">
          <SearchInput value={query} onChange={setQuery} placeholder="Search name, email, or skill" />
          <Field className="compact-field" label="Min CGPA" type="number" value={minCgpa} onChange={setMinCgpa} min={0} max={10} step="0.1" />
        </div>
        <DataTable
          loading={!loaded}
          rows={rows}
          columns={[
            {
              key: "name",
              label: "Student",
              render: (student) => (
                <div className="cell-stack">
                  <strong>{student.name}</strong>
                  <span className="muted small">{student.email}</span>
                </div>
              )
            },
            { key: "cgpa", label: "CGPA", render: (student) => (student.cgpa ? student.cgpa : <span className="muted">-</span>) },
            {
              key: "skills",
              label: "Skills",
              render: (student) => {
                const tags = skillList(student.skills);
                return tags.length
                  ? <div className="tag-list">{tags.slice(0, 4).map((tag) => <span key={tag} className="tag">{tag}</span>)}{tags.length > 4 ? <span className="muted small">+{tags.length - 4}</span> : null}</div>
                  : <span className="muted">Profile incomplete</span>;
              }
            },
            { key: "applications", label: "Applications", render: (student) => applicationsByStudent.get(student.id) || 0 },
            {
              key: "resume",
              label: "Resume",
              render: (student) => student.resumeLink
                ? <a href={student.resumeLink} target="_blank" rel="noopener noreferrer">Open</a>
                : <span className="muted">Missing</span>
            },
            {
              key: "actions",
              label: "",
              className: "actions-cell",
              render: (student) => (
                <button type="button" className="button danger-ghost small" onClick={() => actions.deleteStudent(student)} disabled={busy}>
                  Delete
                </button>
              )
            }
          ]}
          empty={
            <EmptyState
              title={data.students.length ? "No students match" : "No students yet"}
              message={data.students.length ? "Try a different search." : "Students appear here when they register or when you add them."}
            />
          }
        />
      </Card>
    </div>
  );
}

function CompaniesTab({ data, loaded, busy, actions, applicantCounts }) {
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("open");
  const [form, setForm] = useState(emptyCompany);
  const [editingId, setEditingId] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const formRef = useRef(null);

  const openCount = data.companies.filter((company) => !isClosed(company)).length;
  const rows = data.companies.filter((company) =>
    matchesQuery(query, company.name, company.role)
    && (status === "all" || (status === "open") === !isClosed(company)));

  function startEdit(company) {
    setEditingId(company.id);
    setForm({
      name: company.name || "",
      role: company.role || "",
      package: packageOf(company) ?? "",
      eligibilityCgpa: company.eligibilityCgpa ?? "",
      deadline: company.deadline || ""
    });
    setShowForm(true);
    requestAnimationFrame(() => formRef.current?.scrollIntoView({ behavior: "smooth", block: "start" }));
  }

  function closeForm() {
    setShowForm(false);
    setEditingId(null);
    setForm(emptyCompany);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (await actions.saveCompany(form, editingId)) {
      closeForm();
    }
  }

  return (
    <div className="stack">
      {showForm ? (
        <div ref={formRef}>
          <Card title={editingId ? "Edit company" : "Post a company"} subtitle="Students below the CGPA cut-off or past the deadline cannot apply.">
            <form className="form-grid" onSubmit={handleSubmit}>
              <Field label="Company name" value={form.name} onChange={(name) => setForm((current) => ({ ...current, name }))} required maxLength={255} />
              <Field label="Role" value={form.role} onChange={(role) => setForm((current) => ({ ...current, role }))} required maxLength={255} placeholder="e.g. Backend Engineer" />
              <Field label="Package (LPA)" type="number" value={form.package} onChange={(value) => setForm((current) => ({ ...current, package: value }))} required min={0} step="0.01" />
              <Field label="Minimum CGPA" type="number" value={form.eligibilityCgpa} onChange={(eligibilityCgpa) => setForm((current) => ({ ...current, eligibilityCgpa }))} required min={0} max={10} step="0.01" />
              <Field label="Application deadline" type="date" value={form.deadline} onChange={(deadline) => setForm((current) => ({ ...current, deadline }))} required min={todayIso()} />
              <div className="form-actions span-2">
                <button type="button" className="button ghost" onClick={closeForm}>Cancel</button>
                <button type="submit" className="button primary" disabled={busy}>{editingId ? "Save changes" : "Post company"}</button>
              </div>
            </form>
          </Card>
        </div>
      ) : null}

      <Card
        title="Companies"
        subtitle={`${openCount} open, ${data.companies.length - openCount} closed`}
        actions={!showForm ? <button type="button" className="button primary" onClick={() => setShowForm(true)}>Post company</button> : null}
      >
        <div className="toolbar">
          <SearchInput value={query} onChange={setQuery} placeholder="Search company or role" />
          <FilterChips
            label="Company status"
            value={status}
            onChange={setStatus}
            options={[
              { value: "open", label: "Open", count: openCount },
              { value: "closed", label: "Closed", count: data.companies.length - openCount },
              { value: "all", label: "All", count: data.companies.length }
            ]}
          />
        </div>
        <DataTable
          loading={!loaded}
          rows={rows}
          columns={[
            {
              key: "name",
              label: "Company",
              render: (company) => (
                <div className="cell-stack">
                  <strong>{company.name}</strong>
                  <span className="muted small">{company.role}</span>
                </div>
              )
            },
            { key: "package", label: "Package", render: (company) => `${packageOf(company)} LPA` },
            { key: "eligibilityCgpa", label: "Min CGPA" },
            {
              key: "deadline",
              label: "Deadline",
              render: (company) => (
                <div className="cell-stack">
                  <span>{formatDate(company.deadline)}</span>
                  <span className={isClosed(company) ? "muted small" : "small accent-text"}>{deadlineLabel(company.deadline)}</span>
                </div>
              )
            },
            { key: "applicants", label: "Applicants", render: (company) => applicantCounts.get(company.id) || 0 },
            {
              key: "actions",
              label: "",
              className: "actions-cell",
              render: (company) => (
                <div className="row-actions">
                  <button type="button" className="button ghost small" onClick={() => startEdit(company)}>Edit</button>
                  <button type="button" className="button danger-ghost small" onClick={() => actions.deleteCompany(company, applicantCounts.get(company.id) || 0)} disabled={busy}>
                    Delete
                  </button>
                </div>
              )
            }
          ]}
          empty={
            <EmptyState
              title={data.companies.length ? "No companies match" : "No companies yet"}
              message={data.companies.length ? "Try a different search or filter." : "Post the first opening for students to apply to."}
            />
          }
        />
      </Card>
    </div>
  );
}

function ApplicationsTab({ data, loaded, busy, actions }) {
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");

  const rows = data.applications.filter((application) =>
    matchesQuery(query, application.studentEmail, application.companyName)
    && (!status || application.status === status));

  return (
    <Card title="Applications" subtitle="Newest first. Change a status and the student sees it right away.">
      <div className="toolbar">
        <SearchInput value={query} onChange={setQuery} placeholder="Search student email or company" />
        <SelectField
          className="compact-field"
          label="Status"
          value={status}
          onChange={setStatus}
          options={[{ value: "", label: "All statuses" }, ...APPLICATION_STATUSES.map((value) => ({ value, label: value.charAt(0) + value.slice(1).toLowerCase() }))]}
        />
      </div>
      <DataTable
        loading={!loaded}
        rows={rows}
        columns={[
          { key: "studentEmail", label: "Student" },
          { key: "companyName", label: "Company" },
          { key: "appliedDate", label: "Applied", render: (row) => formatDate(row.appliedDate) },
          {
            key: "status",
            label: "Status",
            render: (row) => (
              <select
                className={`status-select status-${row.status.toLowerCase()}`}
                value={row.status}
                disabled={busy}
                aria-label={`Status for ${row.studentEmail} at ${row.companyName}`}
                onChange={(event) => actions.updateStatus(row, event.target.value)}
              >
                {APPLICATION_STATUSES.map((value) => (
                  <option key={value} value={value}>{value.charAt(0) + value.slice(1).toLowerCase()}</option>
                ))}
              </select>
            )
          }
        ]}
        empty={
          <EmptyState
            title={data.applications.length ? "No applications match" : "No applications yet"}
            message={data.applications.length ? "Try a different search or status." : "Applications appear here as students apply."}
          />
        }
      />
    </Card>
  );
}
