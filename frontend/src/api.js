// API client plus the "wake the backend" logic.
//
// The backend runs on Render's free tier, which puts the service to sleep after ~15 minutes
// without traffic. The first request afterwards has to wait for a cold start (often 30-90s for
// Spring Boot). To hide that from users we:
//   1. ping /health the moment the page loads (see main.jsx), long before the user logs in,
//   2. expose the wake-up progress so the UI can show "Waking up the server...",
//   3. make every API call wait for the backend to be awake before sending,
//   4. ping periodically while the tab is in use so the service does not fall asleep.

export const API_BASE = (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080").replace(/\/+$/, "");

const HEALTH_PATH = "/health";
const HEALTH_TIMEOUT_MS = 30_000; // Render holds requests while the service boots
const REQUEST_TIMEOUT_MS = 90_000;
const SLOW_START_AFTER_MS = 2_500; // switch the message from "connecting" to "waking up"
const RETRY_DELAY_MS = 3_000;
const GIVE_UP_AFTER_MS = 180_000;
const OFFLINE_RETRY_MS = 30_000;
const KEEP_ALIVE_IDLE_MS = 10 * 60_000; // Render sleeps after 15 minutes idle
const KEEP_ALIVE_CHECK_MS = 60_000;

export class ApiError extends Error {
  constructor(message, status = 0, data = null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.data = data;
  }
}

// ---------------------------------------------------------------------------
// Backend status store
// status: "checking" | "waking" | "online" | "offline"
// ---------------------------------------------------------------------------

let backendState = { status: "checking", startedAt: Date.now(), attempts: 0 };
let lastContactAt = 0;
let wakePromise = null;
let offlineRetryTimer = null;
const listeners = new Set();

function setBackendState(patch) {
  backendState = { ...backendState, ...patch };
  listeners.forEach((listener) => listener(backendState));
}

export function getBackendState() {
  return backendState;
}

export function subscribeBackendState(listener) {
  listeners.add(listener);
  listener(backendState);
  return () => {
    listeners.delete(listener);
  };
}

function markContact() {
  lastContactAt = Date.now();
  if (backendState.status !== "online") {
    setBackendState({ status: "online", attempts: 0 });
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function fetchWithTimeout(url, options, timeoutMs) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

// Any answer from the application itself (even 401/403/404) proves the service is up.
// 5xx responses and network/CORS failures are what Render returns while a service boots.
async function pingHealth(timeoutMs = HEALTH_TIMEOUT_MS) {
  try {
    const response = await fetchWithTimeout(`${API_BASE}${HEALTH_PATH}`, { cache: "no-store" }, timeoutMs);
    return response.status < 500;
  } catch {
    return false;
  }
}

/**
 * Wakes the backend and resolves to true once it answers, or false after giving up.
 * Concurrent callers share the same attempt. `force` re-checks even if it looked online.
 */
export function wakeBackend({ force = false } = {}) {
  if (!force && backendState.status === "online" && Date.now() - lastContactAt < KEEP_ALIVE_IDLE_MS) {
    return Promise.resolve(true);
  }
  if (wakePromise) {
    return wakePromise;
  }

  clearTimeout(offlineRetryTimer);
  wakePromise = (async () => {
    const startedAt = Date.now();
    setBackendState({ status: "checking", startedAt, attempts: 0 });
    const slowTimer = setTimeout(() => {
      if (backendState.status === "checking") {
        setBackendState({ status: "waking" });
      }
    }, SLOW_START_AFTER_MS);

    try {
      for (let attempt = 1; ; attempt += 1) {
        setBackendState({ attempts: attempt });
        if (await pingHealth()) {
          markContact();
          return true;
        }
        if (Date.now() - startedAt > GIVE_UP_AFTER_MS) {
          setBackendState({ status: "offline" });
          // Keep trying quietly in the background while the page is open.
          offlineRetryTimer = setTimeout(() => void wakeBackend(), OFFLINE_RETRY_MS);
          return false;
        }
        setBackendState({ status: "waking" });
        await sleep(RETRY_DELAY_MS);
      }
    } finally {
      clearTimeout(slowTimer);
      wakePromise = null;
    }
  })();

  return wakePromise;
}

/** Pings the backend while the tab is visible so Render never idles it mid-session. */
export function startKeepAlive() {
  const checkIdle = () => {
    if (document.visibilityState !== "visible" || wakePromise) {
      return;
    }
    if (Date.now() - lastContactAt >= KEEP_ALIVE_IDLE_MS) {
      void pingHealth().then((alive) => {
        if (alive) {
          markContact();
        } else {
          void wakeBackend({ force: true });
        }
      });
    }
  };

  const interval = setInterval(checkIdle, KEEP_ALIVE_CHECK_MS);
  document.addEventListener("visibilitychange", checkIdle);
  window.addEventListener("online", checkIdle);
  return () => {
    clearInterval(interval);
    document.removeEventListener("visibilitychange", checkIdle);
    window.removeEventListener("online", checkIdle);
  };
}

// ---------------------------------------------------------------------------
// API requests
// ---------------------------------------------------------------------------

function fallbackMessage(status) {
  if (status === 401) return "Your session has expired. Please sign in again.";
  if (status === 403) return "You do not have permission to perform this action.";
  if (status === 404) return "The requested resource was not found.";
  if (status === 429) return "Too many requests. Please slow down and try again.";
  if (status >= 500) return "The server ran into a problem. Please try again.";
  return "Request failed";
}

export async function apiRequest(path, method = "GET", body, token) {
  // Wait for a sleeping backend instead of letting the request hang or fail with a CORS error.
  if (backendState.status !== "online") {
    await wakeBackend();
  }

  let response;
  try {
    response = await send(path, method, body, token);
  } catch (error) {
    const timedOut = error?.name === "AbortError";
    // The request never reached the app (connection refused / proxy down), so a read can be
    // retried safely once the backend is awake again. Writes are never retried automatically.
    if (!timedOut && method === "GET" && (await wakeBackend({ force: true }))) {
      try {
        response = await send(path, method, body, token);
      } catch {
        response = null;
      }
    } else if (!timedOut) {
      void wakeBackend({ force: true });
    }

    if (!response) {
      throw new ApiError(
        timedOut
          ? "The server took too long to respond. Please try again."
          : "Cannot reach the server right now. It may be waking up, please try again in a moment."
      );
    }
  }

  const payload = await response.json().catch(() => ({}));

  // 500 comes from the application's own error handler; 502-504 come from Render's proxy
  // when the service is down or restarting.
  if (response.status <= 500) {
    markContact();
  } else {
    void wakeBackend({ force: true });
  }

  if (!response.ok || payload.success === false) {
    throw new ApiError(payload.message || fallbackMessage(response.status), response.status, payload.data);
  }

  return payload.data;
}

function send(path, method, body, token) {
  return fetchWithTimeout(
    `${API_BASE}${path}`,
    {
      method,
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: body ? JSON.stringify(body) : undefined
    },
    REQUEST_TIMEOUT_MS
  );
}
