import { useEffect, useId, useRef, useState } from "react";

export function Field({ label, hint, type = "text", value, onChange, className = "", ...inputProps }) {
  const id = useId();
  return (
    <div className={`field ${className}`}>
      <label htmlFor={id}>{label}</label>
      <input id={id} type={type} value={value} onChange={(event) => onChange(event.target.value)} {...inputProps} />
      {hint ? <small className="field-hint">{hint}</small> : null}
    </div>
  );
}

export function PasswordField({ label = "Password", value, onChange, ...inputProps }) {
  const id = useId();
  const [visible, setVisible] = useState(false);
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <div className="input-with-action">
        <input
          id={id}
          type={visible ? "text" : "password"}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          {...inputProps}
        />
        <button
          type="button"
          className="input-action"
          onClick={() => setVisible((current) => !current)}
          aria-label={visible ? "Hide password" : "Show password"}
          aria-pressed={visible}
        >
          {visible ? "Hide" : "Show"}
        </button>
      </div>
    </div>
  );
}

export function SelectField({ label, value, onChange, options, className = "" }) {
  const id = useId();
  return (
    <div className={`field ${className}`}>
      <label htmlFor={id}>{label}</label>
      <select id={id} value={value} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => (
          <option key={option.value} value={option.value}>{option.label}</option>
        ))}
      </select>
    </div>
  );
}

export function SearchInput({ value, onChange, placeholder, label = "Search" }) {
  return (
    <div className="search-input">
      <svg className="search-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
        <circle cx="11" cy="11" r="7" fill="none" stroke="currentColor" strokeWidth="2" />
        <path d="m20 20-3.5-3.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      </svg>
      <input
        type="search"
        aria-label={label}
        placeholder={placeholder}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
    </div>
  );
}

export function Card({ title, subtitle, actions, children, className = "" }) {
  return (
    <section className={`card ${className}`}>
      {title || actions ? (
        <header className="card-head">
          <div>
            {title ? <h2>{title}</h2> : null}
            {subtitle ? <p>{subtitle}</p> : null}
          </div>
          {actions ? <div className="card-actions">{actions}</div> : null}
        </header>
      ) : null}
      {children}
    </section>
  );
}

export function StatCard({ label, value, accent, hint }) {
  return (
    <article className="stat-card" style={{ "--accent": accent }}>
      <span>{label}</span>
      <strong>{value}</strong>
      {hint ? <small>{hint}</small> : null}
    </article>
  );
}

export function StatusPill({ status }) {
  const value = status || "UNKNOWN";
  const label = value.charAt(0) + value.slice(1).toLowerCase();
  return <span className={`status-pill status-${value.toLowerCase()}`}>{label}</span>;
}

export function Badge({ tone = "neutral", children }) {
  return <span className={`badge badge-${tone}`}>{children}</span>;
}

const TRACK = ["APPLIED", "SHORTLISTED", "SELECTED"];

/** Applied -> Shortlisted -> Selected, or Applied -> Not selected. */
export function StatusTracker({ status }) {
  const steps = status === "REJECTED"
    ? [{ label: "Applied", state: "done" }, { label: "Not selected", state: "rejected" }]
    : TRACK.map((step, index) => ({
      label: step.charAt(0) + step.slice(1).toLowerCase(),
      state: index <= TRACK.indexOf(status) ? "done" : "pending"
    }));

  return (
    <ol className="tracker" aria-label={`Application status: ${status}`}>
      {steps.map((step) => (
        <li key={step.label} className={`tracker-step tracker-${step.state}`}>
          <span className="tracker-dot" aria-hidden="true">
            {step.state === "done" ? "✓" : step.state === "rejected" ? "✕" : ""}
          </span>
          <span>{step.label}</span>
        </li>
      ))}
    </ol>
  );
}

export function EmptyState({ title, message, action }) {
  return (
    <div className="empty-state">
      <strong>{title}</strong>
      {message ? <p>{message}</p> : null}
      {action}
    </div>
  );
}

export function Skeleton({ height = 18, width = "100%" }) {
  return <span className="skeleton" style={{ height, width }} aria-hidden="true" />;
}

export function SkeletonCards({ count = 3 }) {
  return (
    <div className="card-grid" aria-busy="true" aria-label="Loading">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="card skeleton-card">
          <Skeleton width="40%" height={12} />
          <Skeleton width="70%" height={22} />
          <Skeleton height={12} />
          <Skeleton height={12} width="80%" />
          <Skeleton height={40} />
        </div>
      ))}
    </div>
  );
}

/**
 * Table on wide screens, stacked cards on phones (each cell shows its column label).
 * columns: [{ key, label, render(row), className }]
 */
export function DataTable({ columns, rows, rowKey = (row) => row.id, empty, loading }) {
  if (loading) {
    return (
      <div className="table-skeleton" aria-busy="true" aria-label="Loading">
        {Array.from({ length: 4 }, (_, index) => <Skeleton key={index} height={44} />)}
      </div>
    );
  }
  if (!rows.length) {
    return empty || <EmptyState title="Nothing here yet" />;
  }
  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key} className={column.className}>{column.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={rowKey(row)}>
              {columns.map((column) => (
                <td key={column.key} data-label={column.label} className={column.className}>
                  {column.render ? column.render(row) : row[column.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function Tabs({ tabs, active, onChange }) {
  const navRef = useRef(null);

  // Keep the active tab visible when the tab bar scrolls horizontally on small screens.
  useEffect(() => {
    navRef.current?.querySelector(".tab.active")?.scrollIntoView({ block: "nearest", inline: "nearest" });
  }, [active]);

  return (
    <nav className="tabs" role="tablist" aria-label="Sections" ref={navRef}>
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={active === tab.id}
          className={active === tab.id ? "tab active" : "tab"}
          onClick={() => onChange(tab.id)}
        >
          {tab.label}
          {tab.count != null ? <span className="tab-count">{tab.count}</span> : null}
        </button>
      ))}
    </nav>
  );
}

export function FilterChips({ options, value, onChange, label }) {
  return (
    <div className="chips" role="radiogroup" aria-label={label}>
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          role="radio"
          aria-checked={value === option.value}
          className={value === option.value ? "chip active" : "chip"}
          onClick={() => onChange(option.value)}
        >
          {option.label}
          {option.count != null ? <span className="chip-count">{option.count}</span> : null}
        </button>
      ))}
    </div>
  );
}

export function Toasts({ toasts, onDismiss }) {
  return (
    <div className="toast-stack" aria-live="polite" aria-atomic="false">
      {toasts.map((toast) => (
        <div key={toast.id} className={`toast toast-${toast.tone}`} role={toast.tone === "error" ? "alert" : "status"}>
          <span>{toast.message}</span>
          <button type="button" className="toast-close" onClick={() => onDismiss(toast.id)} aria-label="Dismiss">
            ×
          </button>
        </div>
      ))}
    </div>
  );
}

export function ConfirmDialog({ request, onClose }) {
  const confirmRef = useRef(null);

  useEffect(() => {
    if (!request) {
      return undefined;
    }
    confirmRef.current?.focus();
    const onKey = (event) => {
      if (event.key === "Escape") {
        onClose(false);
      }
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [request, onClose]);

  if (!request) {
    return null;
  }

  return (
    <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose(false)}>
      <div className="modal" role="alertdialog" aria-modal="true" aria-labelledby="confirm-title" aria-describedby="confirm-message">
        <h2 id="confirm-title">{request.title}</h2>
        <p id="confirm-message">{request.message}</p>
        <div className="modal-actions">
          <button type="button" className="button ghost" onClick={() => onClose(false)}>Cancel</button>
          <button
            ref={confirmRef}
            type="button"
            className={`button ${request.tone === "danger" ? "danger" : "primary"}`}
            onClick={() => onClose(true)}
          >
            {request.confirmLabel || "Confirm"}
          </button>
        </div>
      </div>
    </div>
  );
}
