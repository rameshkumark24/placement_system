import { initials } from "../lib/format";
import BackendStatus from "./BackendStatus";
import { Tabs } from "./ui";

export default function AppHeader({ user, tabs, tab, setTab, loading, onRefresh, onLogout }) {
  const isAdmin = user.role === "ADMIN";
  return (
    <header className="app-header">
      <div className="header-row">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">P</span>
          <div>
            <strong>Placement System</strong>
            <span className="muted small">{isAdmin ? "Admin console" : "Student portal"}</span>
          </div>
        </div>

        <div className="header-actions">
          <BackendStatus compact />
          <button
            type="button"
            className="icon-button"
            onClick={onRefresh}
            disabled={loading}
            aria-label="Refresh data"
            title="Refresh data"
          >
            <span className={loading ? "refresh-icon spinning" : "refresh-icon"} aria-hidden="true">↻</span>
          </button>
          <div className="user-chip">
            <span className="avatar" aria-hidden="true">{initials(user.email)}</span>
            <div className="user-meta">
              <strong>{user.email}</strong>
              <span className="muted small">{isAdmin ? "Administrator" : "Student"}</span>
            </div>
          </div>
          <button type="button" className="button ghost small" onClick={onLogout}>Sign out</button>
        </div>
      </div>
      <Tabs tabs={tabs} active={tab} onChange={setTab} />
    </header>
  );
}
