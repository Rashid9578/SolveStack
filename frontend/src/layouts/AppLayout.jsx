import React, { useState } from 'react';
import { Link, Outlet } from 'react-router-dom';
import { SessionActions } from '../components/SessionActions.jsx';

export function AppLayout() {
  const [tipOpen, setTipOpen] = useState(false);
  return (
    <>
      <header className="topbar">
        <div className="brand-actions">
          <Link className="brand" to="/">SolveStack</Link>
          <button className="tip-toggle" type="button" aria-expanded={tipOpen} aria-controls="practice-tip"
            onClick={() => setTipOpen((open) => !open)}>
            Hot tip
          </button>
        </div>
        <SessionActions />
      </header>
      <div id="practice-tip" className="tip-banner" hidden={!tipOpen}>
        Open a problem in a new tab, then mark it done here when you finish.
      </div>
      <main className="page-shell"><Outlet /></main>
      <footer className="site-footer">
        <a href="https://in.linkedin.com/in/rashid-ahmed-siddiquee-002072325" target="_blank" rel="noreferrer">Rashid Ahmed Siddiquee</a>
        <a href="https://github.com/Rashid9578" target="_blank" rel="noreferrer">GitHub</a>
      </footer>
    </>
  );
}
