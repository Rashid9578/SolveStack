import React, { createContext, useContext, useEffect, useRef, useState } from 'react';

const TrackingContext = createContext(null);

export function TrackingProvider({ children }) {
  const activeWrite = useRef(null);
  const [pending, setPending] = useState(null);
  const [revision, setRevision] = useState(0);

  useEffect(() => () => activeWrite.current?.abort(), []);

  async function write(key, request) {
    if (activeWrite.current) return undefined;

    const controller = new AbortController();
    activeWrite.current = controller;
    setPending(key);

    try {
      return await request(controller.signal);
    } finally {
      activeWrite.current = null;
      if (!controller.signal.aborted) {
        // A lost response can leave a write committed, so always reread the tracker.
        setRevision((number) => number + 1);
        setPending(null);
      }
    }
  }

  return <TrackingContext.Provider value={{ revision, pending, write }}>{children}</TrackingContext.Provider>;
}

export function useTracking() {
  return useContext(TrackingContext);
}
