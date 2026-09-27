import { useEffect, useMemo, useState } from "react";
import {
  Badge,
  Card,
  EmptyState,
  Field,
  FilterChips,
  SearchInput,
  SkeletonCards,
  StatCard,
  StatusPill,
  StatusTracker
} from "../components/ui";
import {
  companyEligibility,
  daysUntil,
  deadlineLabel,
  displaySkills,
  formatDate,
  matchesQuery,
  packageOf,
  profileChecklist,
  skillList
} from "../lib/format";

export default function StudentView({ tab, setTab, data, loaded, busy, actions }) {
  const { companies, applications, profile } = data;
  const appliedCompanyIds = useMemo(
    () => new Set(applications.map((application) => application.companyId)),
    [applications]
  );
  const withEligibility = useMemo(
    () => companies.map((company) => ({ company, eligibility: companyEligibility(company, { profile, appliedCompanyIds }) })),
    [companies, profile, appliedCompanyIds]
  );

  if (tab === "applications") {
    return <MyApplications applications={applications} loaded={loaded} setTab={setTab} />;
  }
  if (tab === "profile") {
    return <ProfileForm profile={profile} loaded={loaded} busy={busy} onSave={actions.updateProfile} />;
  }
  return (
    <Opportunities
      items={withEligibility}
      applications={applications}
      profile={profile}
      loaded={loaded}
      busy={busy}
      setTab={setTab}
      onApply={actions.apply}
    />
  );
}

function Opportunities({ items, applications, profile, loaded, busy, setTab, onApply }) {
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("all");
  const checklist = profileChecklist(profile);
  const completed = checklist.filter((item) => item.done).length;

  const counts = items.reduce((totals, item) => {
    totals[item.eligibility.group] = (totals[item.eligibility.group] || 0) + 1;
    return totals;
  }, {});
  const visible = items.filter(({ company, eligibility }) =>
    (filter === "all" || eligibility.group === filter)
    && matchesQuery(query, company.name, company.role));
  const shortlisted = applications.filter((application) => ["SHORTLISTED", "SELECTED"].includes(application.status)).length;

  return (
    <div className="stack">
      <section className="stats-grid">
        <StatCard label="Open for you" value={counts.eligible || 0} accent="var(--sun)" hint="Eligible and not yet applied" />
        <StatCard label="Applications" value={applications.length} accent="var(--sky)" />
        <StatCard label="Shortlisted" value={shortlisted} accent="var(--mint)" hint="Including selected" />
      </section>

      {loaded && completed < checklist.length ? (
        <div className="callout">
          <div>
            <strong>Finish your profile to start applying</strong>
            <p>
              {completed} of {checklist.length} done. Still missing:{" "}
              {checklist.filter((item) => !item.done).map((item) => item.label).join(", ")}.
            </p>
            <div className="progress" aria-hidden="true">
              <span style={{ width: `${(completed / checklist.length) * 100}%` }} />
            </div>
          </div>
          <button type="button" className="button primary" onClick={() => setTab("profile")}>Complete profile</button>
        </div>
      ) : null}

      <Card title="Opportunities" subtitle="Sorted by deadline. Your eligibility is shown on every opening.">
        <div className="toolbar">
          <SearchInput value={query} onChange={setQuery} placeholder="Search company or role" />
          <FilterChips
            label="Filter openings"
            value={filter}
            onChange={setFilter}
            options={[
              { value: "all", label: "All", count: items.length },
              { value: "eligible", label: "Eligible", count: counts.eligible || 0 },
              { value: "applied", label: "Applied", count: counts.applied || 0 },
              { value: "closed", label: "Closed", count: counts.closed || 0 }
            ]}
          />
        </div>

        {!loaded ? (
          <SkeletonCards count={3} />
        ) : visible.length ? (
          <div className="card-grid">
            {visible.map(({ company, eligibility }) => (
              <CompanyCard
                key={company.id}
                company={company}
                eligibility={eligibility}
                busy={busy}
                onApply={() => onApply(company)}
                onFixProfile={() => setTab("profile")}
              />
            ))}
          </div>
        ) : (
          <EmptyState
            title={items.length ? "No openings match" : "No openings yet"}
            message={items.length ? "Try a different search or filter." : "New companies will show up here as soon as they are posted."}
            action={items.length ? (
              <button type="button" className="button ghost" onClick={() => { setQuery(""); setFilter("all"); }}>Clear filters</button>
            ) : null}
          />
        )}
      </Card>
    </div>
  );
}

function CompanyCard({ company, eligibility, busy, onApply, onFixProfile }) {
  const days = daysUntil(company.deadline);
  const urgent = days !== null && days >= 0 && days <= 3;
  const needsProfile = eligibility.group === "blocked" && eligibility.tone === "warn";

  return (
    <article className="opening">
      <div className="opening-head">
        <span className="eyebrow">{company.role}</span>
        <Badge tone={eligibility.tone}>{eligibility.label}</Badge>
      </div>
      <h3>{company.name}</h3>
      <dl className="facts">
        <div><dt>Package</dt><dd>{packageOf(company)} LPA</dd></div>
        <div><dt>Min CGPA</dt><dd>{company.eligibilityCgpa}</dd></div>
        <div>
          <dt>Deadline</dt>
          <dd>
            {formatDate(company.deadline)}
            <span className={urgent ? "deadline urgent" : "deadline"}>{deadlineLabel(company.deadline)}</span>
          </dd>
        </div>
      </dl>
      {needsProfile ? (
        <button type="button" className="button ghost block" onClick={onFixProfile}>Complete profile</button>
      ) : (
        <button type="button" className="button primary block" onClick={onApply} disabled={!eligibility.canApply || busy}>
          {eligibility.action}
        </button>
      )}
    </article>
  );
}

function MyApplications({ applications, loaded, setTab }) {
  if (!loaded) {
    return <SkeletonCards count={2} />;
  }
  return (
    <Card title="My applications" subtitle="Newest first. Statuses update as the placement team reviews them.">
      {applications.length ? (
        <ul className="application-list">
          {applications.map((application) => (
            <li key={application.id} className="application">
              <div className="application-head">
                <div>
                  <h3>{application.companyName}</h3>
                  <span className="muted small">Applied {formatDate(application.appliedDate)}</span>
                </div>
                <StatusPill status={application.status} />
              </div>
              <StatusTracker status={application.status} />
            </li>
          ))}
        </ul>
      ) : (
        <EmptyState
          title="You have not applied anywhere yet"
          message="Browse the openings you are eligible for and apply in one click."
          action={<button type="button" className="button primary" onClick={() => setTab("opportunities")}>Browse opportunities</button>}
        />
      )}
    </Card>
  );
}

function profileToForm(profile) {
  return {
    name: profile?.name || "",
    cgpa: profile?.cgpa ? String(profile.cgpa) : "",
    skills: displaySkills(profile?.skills),
    resumeLink: profile?.resumeLink || ""
  };
}

function ProfileForm({ profile, loaded, busy, onSave }) {
  // Filled from the saved profile on the first render, so there is never an empty form to type into.
  const [form, setForm] = useState(() => profileToForm(profile));
  const [dirty, setDirty] = useState(false);

  // Later server updates are applied only while there are no unsaved edits, so a background
  // reload never wipes what the student is typing.
  useEffect(() => {
    if (profile && !dirty) {
      setForm(profileToForm(profile));
    }
  }, [profile, dirty]);

  function update(patch) {
    setForm((current) => ({ ...current, ...patch }));
    setDirty(true);
  }

  const checklist = profileChecklist(profile);
  const tags = skillList(form.skills);

  async function handleSubmit(event) {
    event.preventDefault();
    if (await onSave(form)) {
      setDirty(false);
    }
  }

  return (
    <div className="split">
      <Card title="Your profile" subtitle="Companies check your CGPA against their cut-off when you apply.">
        {!loaded ? (
          <SkeletonCards count={1} />
        ) : (
          <form className="form-grid" onSubmit={handleSubmit}>
            <Field label="Full name" value={form.name} onChange={(name) => update({ name })} required maxLength={100} autoComplete="name" />
            <Field
              label="CGPA"
              type="number"
              value={form.cgpa}
              onChange={(cgpa) => update({ cgpa })}
              required
              min={0}
              max={10}
              step="0.01"
              placeholder="e.g. 8.25"
              hint="On a 10-point scale"
            />
            <Field
              className="span-2"
              label="Skills"
              value={form.skills}
              onChange={(skills) => update({ skills })}
              required
              maxLength={255}
              placeholder="Java, Spring Boot, React, SQL"
              hint="Separate skills with commas"
            />
            {tags.length ? (
              <div className="tag-list span-2" aria-label="Skills preview">
                {tags.map((tag) => <span key={tag} className="tag">{tag}</span>)}
              </div>
            ) : null}
            <Field
              className="span-2"
              label="Resume link"
              type="url"
              value={form.resumeLink}
              onChange={(resumeLink) => update({ resumeLink })}
              required
              maxLength={255}
              placeholder="https://drive.google.com/..."
              hint="Make sure the link is viewable by anyone with it"
            />
            <div className="form-actions span-2">
              {dirty ? <span className="muted small">Unsaved changes</span> : null}
              <button className="button primary" type="submit" disabled={busy}>
                {busy ? <span className="spinner" aria-hidden="true" /> : null}
                Save profile
              </button>
            </div>
          </form>
        )}
      </Card>

      <Card title="Profile checklist" subtitle="All three are needed before you can apply." className="aside">
        <ul className="checklist">
          {checklist.map((item) => (
            <li key={item.key} className={item.done ? "done" : ""}>
              <span className="check" aria-hidden="true">{item.done ? "✓" : ""}</span>
              {item.label}
              <span className="sr-only">{item.done ? "complete" : "missing"}</span>
            </li>
          ))}
        </ul>
        {profile?.resumeLink ? (
          <a className="button ghost block" href={profile.resumeLink} target="_blank" rel="noopener noreferrer">
            Open saved resume
          </a>
        ) : null}
      </Card>
    </div>
  );
}
