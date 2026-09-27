import { wakeBackend } from "../api";
import { useBackendStatus, useElapsedSeconds } from "../useBackendStatus";

const LABELS = {
  checking: "Connecting to server...",
  waking: "Waking up the server",
  online: "Server online",
  offline: "Server unreachable"
};

/**
 * Shows whether the Render backend is awake. `compact` renders a small pill for the top bar;
 * the full version explains the free-tier cold start on the login screen.
 */
export default function BackendStatus({ compact = false }) {
  const { status, startedAt } = useBackendStatus();
  const busy = status === "checking" || status === "waking";
  const elapsed = useElapsedSeconds(startedAt, busy);

  if (compact) {
    return (
      <span className={`server-pill server-${status}`} role="status" aria-live="polite">
        <span className="server-dot" aria-hidden="true" />
        {LABELS[status]}
        {status === "waking" ? ` (${elapsed}s)` : ""}
        {status === "offline" ? (
          <button type="button" className="link-button" onClick={() => void wakeBackend({ force: true })}>
            Retry
          </button>
        ) : null}
      </span>
    );
  }

  return (
    <div className={`server-status server-${status}`} role="status" aria-live="polite">
      <div className="server-status-head">
        <span className="server-dot" aria-hidden="true" />
        <strong>{LABELS[status]}</strong>
        {busy ? <span className="server-elapsed">{elapsed}s</span> : null}
      </div>
      {status === "waking" ? (
        <p>
          The backend is hosted on a free plan that sleeps when idle, so the first visit can take up to a
          minute. It starts automatically; you can fill in the form meanwhile.
          {elapsed >= 5 ? <span className="progress-track"><span className="progress-bar" style={{ width: `${Math.min(95, (elapsed / 75) * 100)}%` }} /></span> : null}
        </p>
      ) : null}
      {status === "offline" ? (
        <p>
          The server did not respond. It keeps retrying in the background.{" "}
          <button type="button" className="link-button" onClick={() => void wakeBackend({ force: true })}>
            Retry now
          </button>
        </p>
      ) : null}
    </div>
  );
}
