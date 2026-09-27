import { useState } from "react";
import { getBackendState, wakeBackend } from "../api";
import BackendStatus from "../components/BackendStatus";
import { Field, PasswordField } from "../components/ui";
import { useBackendStatus } from "../useBackendStatus";

const FEATURES = [
  { title: "Know where you stand", text: "Every opening shows whether you are eligible before you apply." },
  { title: "Track every application", text: "Follow each one from applied to shortlisted to selected." },
  { title: "One console for admins", text: "Students, companies, and the full pipeline in one place." }
];

export default function AuthView({ onLogin, onRegister }) {
  const backend = useBackendStatus();
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ name: "", email: "", password: "" });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  function switchMode(next) {
    setMode(next);
    setError("");
    setNotice("");
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setError("");
    setNotice("");

    try {
      if (getBackendState().status !== "online" && !(await wakeBackend())) {
        throw new Error("The server is not reachable right now. Please try again in a moment.");
      }
      if (mode === "register") {
        await onRegister(form);
        setMode("login");
        setForm({ name: "", email: form.email.trim(), password: "" });
        setNotice("Account created. Sign in to continue.");
      } else {
        await onLogin(form.email, form.password);
      }
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setLoading(false);
    }
  }

  const waitingForServer = loading && backend.status !== "online";
  const submitLabel = waitingForServer
    ? "Waiting for server..."
    : loading
      ? mode === "login" ? "Signing in..." : "Creating account..."
      : mode === "login" ? "Sign in" : "Create account";

  return (
    <main className="auth-shell">
      <section className="auth-hero">
        <span className="brand-mark" aria-hidden="true">P</span>
        <span className="eyebrow">Placement Management System</span>
        <h1>Your campus placements, in one place.</h1>
        <p className="lead">
          Students build a profile, discover openings they qualify for, and follow every application.
          Placement admins run the whole pipeline from a single console.
        </p>
        <ul className="feature-list">
          {FEATURES.map((feature) => (
            <li key={feature.title}>
              <strong>{feature.title}</strong>
              <span>{feature.text}</span>
            </li>
          ))}
        </ul>
      </section>

      <section className="auth-panel card">
        <BackendStatus />

        <div className="segmented" role="tablist" aria-label="Account">
          <button type="button" role="tab" aria-selected={mode === "login"} className={mode === "login" ? "active" : ""} onClick={() => switchMode("login")}>
            Sign in
          </button>
          <button type="button" role="tab" aria-selected={mode === "register"} className={mode === "register" ? "active" : ""} onClick={() => switchMode("register")}>
            Create account
          </button>
        </div>

        <form className="form-stack" onSubmit={handleSubmit}>
          {mode === "register" ? (
            <Field
              label="Full name"
              value={form.name}
              onChange={(name) => setForm({ ...form, name })}
              placeholder="Your full name"
              autoComplete="name"
              maxLength={100}
              required
            />
          ) : null}

          <Field
            label="Email"
            type="email"
            value={form.email}
            onChange={(email) => setForm({ ...form, email })}
            placeholder="you@college.edu"
            autoComplete="email"
            required
          />

          <PasswordField
            value={form.password}
            onChange={(password) => setForm({ ...form, password })}
            placeholder={mode === "register" ? "At least 6 characters" : "Your password"}
            autoComplete={mode === "register" ? "new-password" : "current-password"}
            minLength={mode === "register" ? 6 : undefined}
            maxLength={72}
            required
          />

          {error ? <p className="form-message error" role="alert">{error}</p> : null}
          {notice ? <p className="form-message success" role="status">{notice}</p> : null}

          <button className="button primary block" type="submit" disabled={loading}>
            {loading ? <span className="spinner" aria-hidden="true" /> : null}
            {submitLabel}
          </button>
        </form>

        <p className="muted small">
          {mode === "login"
            ? "New student? Choose Create account. Admin accounts are set up by the placement office."
            : "Creating an account registers you as a student. You can complete your profile after signing in."}
        </p>
      </section>
    </main>
  );
}
