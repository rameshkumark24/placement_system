import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { apiRequest } from "./api";
import AppHeader from "./components/AppHeader";
import { ConfirmDialog, Toasts } from "./components/ui";
import AdminView from "./views/AdminView";
import AuthView from "./views/AuthView";
import StudentView from "./views/StudentView";

const SESSION_KEY = "placement-system-session";
const SESSION_EXPIRED = "Your session has expired. Please sign in again.";
const TOKEN_REFRESH_MARGIN_MS = 30_000;
const TOAST_TIMEOUT_MS = 4_500;

const ADMIN_TABS = [
  { id: "overview", label: "Overview" },
  { id: "students", label: "Students" },
  { id: "companies", label: "Companies" },
  { id: "applications", label: "Applications" }
];

const STUDENT_TABS = [
  { id: "opportunities", label: "Opportunities" },
  { id: "applications", label: "My applications" },
  { id: "profile", label: "Profile" }
];

const emptyData = { students: [], companies: [], applications: [], stats: null, profile: null };

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

function tabFromHash(tabs) {
  const hash = window.location.hash.replace("#", "");
  return tabs.some((tab) => tab.id === hash) ? hash : tabs[0].id;
}

export default function App() {
  const [session, setSessionState] = useState(readSession);
  const sessionRef = useRef(session);
  const refreshPromiseRef = useRef(null);
  const [data, setData] = useState(emptyData);
  const [loaded, setLoaded] = useState(false);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [toasts, setToasts] = useState([]);
  const [confirmRequest, setConfirmRequest] = useState(null);

  const currentUser = session?.user || null;
  const isAdmin = currentUser?.role === "ADMIN";
  const sessionIdentity = currentUser ? `${currentUser.email}|${currentUser.role}` : "";
  const tabs = isAdmin ? ADMIN_TABS : STUDENT_TABS;
  const [tab, setTabState] = useState(() => tabFromHash(tabs));

  // ------------------------------------------------------------------ feedback

  const dismissToast = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const toast = useCallback((message, tone = "success") => {
    const id = `${Date.now()}-${Math.random()}`;
    setToasts((current) => [...current.slice(-2), { id, message, tone }]);
    setTimeout(() => dismissToast(id), tone === "error" ? TOAST_TIMEOUT_MS * 2 : TOAST_TIMEOUT_MS);
  }, [dismissToast]);

  const confirmResolveRef = useRef(null);

  const confirm = useCallback((request) => new Promise((resolve) => {
    confirmResolveRef.current?.(false);
    confirmResolveRef.current = resolve;
    setConfirmRequest(request);
  }), []);

  const closeConfirm = useCallback((answer) => {
    confirmResolveRef.current?.(answer);
    confirmResolveRef.current = null;
    setConfirmRequest(null);
  }, []);

  // ------------------------------------------------------------------ navigation

  useEffect(() => {
    if (!sessionIdentity) {
      return undefined;
    }
    setTabState(tabFromHash(tabs));
    const onHashChange = () => setTabState(tabFromHash(tabs));
    window.addEventListener("hashchange", onHashChange);
    return () => window.removeEventListener("hashchange", onHashChange);
  }, [sessionIdentity]);

  function setTab(id) {
    if (window.location.hash !== `#${id}`) {
      window.location.hash = id;
    }
    setTabState(id);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  // ------------------------------------------------------------------ session

  function updateSession(next) {
    sessionRef.current = next;
    writeSession(next);
    setSessionState(next);
  }

  function logout(message) {
    updateSession(null);
    setData(emptyData);
    setLoaded(false);
    if (window.location.hash) {
      window.history.replaceState(null, "", window.location.pathname + window.location.search);
    }
    if (message) {
      toast(message, "info");
    }
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
          const response = await apiRequest("/auth/refresh", "POST", { refreshToken: current.refreshToken });
          const next = buildSession(response);
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

  async function login(email, password) {
    const response = await apiRequest("/auth/login", "POST", { email, password });
    const next = buildSession(response);
    updateSession(next);
    toast(`Welcome back, ${next.user.email}`);
  }

  async function register(form) {
    await apiRequest("/auth/register", "POST", form);
  }

  // ------------------------------------------------------------------ data

  async function loadData() {
    const active = sessionRef.current;
    if (!active?.token) {
      return;
    }
    if (active.user.role === "ADMIN") {
      const [students, companies, applications, stats] = await Promise.all([
        authorizedRequest("/students"),
        authorizedRequest("/companies"),
        authorizedRequest("/applications"),
        authorizedRequest("/dashboard/stats")
      ]);
      setData({ ...emptyData, students: students || [], companies: companies || [], applications: applications || [], stats });
    } else {
      const [companies, applications, profile] = await Promise.all([
        authorizedRequest("/companies"),
        authorizedRequest("/applications/my"),
        authorizedRequest("/students/me")
      ]);
      setData({ ...emptyData, companies: companies || [], applications: applications || [], profile });
    }
    setLoaded(true);
  }

  async function refreshData() {
    setLoading(true);
    try {
      await loadData();
    } catch (error) {
      if (sessionRef.current) {
        toast(error.message, "error");
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (sessionIdentity) {
      void refreshData();
    }
    // Reload only when the signed-in identity changes, not on every token refresh.
  }, [sessionIdentity]);

  /** Runs a change, reports the outcome as a toast, then reloads data. Resolves to true on success. */
  async function runAction(action, successMessage) {
    setBusy(true);
    try {
      await action();
    } catch (error) {
      toast(error.message, "error");
      setBusy(false);
      return false;
    }
    toast(successMessage);
    try {
      await loadData();
    } catch (error) {
      toast(`Saved, but reloading the latest data failed: ${error.message}`, "error");
    }
    setBusy(false);
    return true;
  }

  const actions = {
    refresh: refreshData,
    apply: (company) => runAction(
      () => authorizedRequest(`/applications/apply/${company.id}`, "POST"),
      `Applied to ${company.name}. Track it under My applications.`
    ),
    updateProfile: (form) => runAction(
      () => authorizedRequest("/students/me", "PUT", {
        name: form.name,
        cgpa: Number(form.cgpa),
        skills: form.skills,
        resumeLink: form.resumeLink
      }),
      "Profile saved."
    ),
    createStudent: (form) => runAction(
      () => authorizedRequest("/students", "POST", { ...form, cgpa: Number(form.cgpa) }),
      `${form.name} added.`
    ),
    deleteStudent: async (student) => {
      const ok = await confirm({
        title: `Delete ${student.name || student.email}?`,
        message: "Their login account and all of their applications will be permanently removed.",
        confirmLabel: "Delete student",
        tone: "danger"
      });
      return ok && runAction(() => authorizedRequest(`/students/${student.id}`, "DELETE"), "Student deleted.");
    },
    saveCompany: (form, id) => runAction(
      () => authorizedRequest(id ? `/companies/${id}` : "/companies", id ? "PUT" : "POST", {
        ...form,
        package: Number(form.package),
        eligibilityCgpa: Number(form.eligibilityCgpa)
      }),
      id ? `${form.name} updated.` : `${form.name} posted. Eligible students can apply now.`
    ),
    deleteCompany: async (company, applicantCount) => {
      const ok = await confirm({
        title: `Delete ${company.name}?`,
        message: applicantCount
          ? `This also removes ${applicantCount} application${applicantCount === 1 ? "" : "s"} to this company. This cannot be undone.`
          : "This cannot be undone.",
        confirmLabel: "Delete company",
        tone: "danger"
      });
      return ok && runAction(() => authorizedRequest(`/companies/${company.id}`, "DELETE"), "Company deleted.");
    },
    updateStatus: (application, status) => runAction(
      () => authorizedRequest(`/applications/${application.id}/status`, "PUT", { status }),
      `${application.studentEmail} is now ${status.toLowerCase()} for ${application.companyName}.`
    )
  };

  const tabsWithCounts = useMemo(() => tabs.map((item) => {
    if (!loaded) return item;
    if (isAdmin && item.id === "students") return { ...item, count: data.students.length };
    if (isAdmin && item.id === "companies") return { ...item, count: data.companies.length };
    if (item.id === "applications") return { ...item, count: data.applications.length };
    return item;
  }), [tabs, loaded, isAdmin, data]);

  return (
    <>
      {session?.token ? (
        <div className="app-shell">
          <AppHeader
            user={currentUser}
            tabs={tabsWithCounts}
            tab={tab}
            setTab={setTab}
            loading={loading}
            onRefresh={refreshData}
            onLogout={() => logout("You have been signed out.")}
          />
          <main className="app-main" aria-busy={loading}>
            {isAdmin ? (
              <AdminView tab={tab} setTab={setTab} data={data} loaded={loaded} busy={busy} actions={actions} />
            ) : (
              <StudentView tab={tab} setTab={setTab} data={data} loaded={loaded} busy={busy} actions={actions} />
            )}
          </main>
        </div>
      ) : (
        <AuthView onLogin={login} onRegister={register} />
      )}
      <Toasts toasts={toasts} onDismiss={dismissToast} />
      <ConfirmDialog request={confirmRequest} onClose={closeConfirm} />
    </>
  );
}
