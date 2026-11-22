import React, { createContext, useContext, useEffect, useState } from 'react';
import { login, logout, register, startOrResumeSession } from '../services/session.js';

const SessionContext = createContext(null);

export function SessionProvider({ children }) {
  const [session, setSession] = useState({ status: 'loading' });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let active = true;

    startOrResumeSession().then(
      (identity) => { if (active) setSession({ status: 'ready', identity }); },
      () => { if (active) setSession({ status: 'error' }); },
    );

    return () => { active = false; };
  }, [attempt]);

  function retry() {
    setSession({ status: 'loading' });
    setAttempt((previous) => previous + 1);
  }

  async function signIn(credentials) {
    const identity = await login(credentials);
    setSession({ status: 'ready', identity });
  }

  async function signUp(credentials) {
    const identity = await register(credentials);
    setSession({ status: 'ready', identity });
  }

  async function signOut() {
    try {
      await logout();
    } catch (error) {
      if (error.response?.status !== 401) throw error;
    }
    retry();
  }

  return (
    <SessionContext.Provider value={{ ...session, retry, signIn, signUp, signOut }}>
      {children}
    </SessionContext.Provider>
  );
}

export function useSession() {
  return useContext(SessionContext);
}
