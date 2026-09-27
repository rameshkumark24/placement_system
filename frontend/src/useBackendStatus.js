import { useEffect, useState } from "react";
import { getBackendState, subscribeBackendState } from "./api";

export function useBackendStatus() {
  const [state, setState] = useState(getBackendState);
  useEffect(() => subscribeBackendState(setState), []);
  return state;
}

/** Seconds elapsed since `startedAt`, re-rendering every second while `active`. */
export function useElapsedSeconds(startedAt, active) {
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    if (!active) {
      return undefined;
    }
    setNow(Date.now());
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [active, startedAt]);

  return active ? Math.max(0, Math.round((now - startedAt) / 1000)) : 0;
}
