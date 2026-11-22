import React from 'react';
import { Link, Outlet } from 'react-router-dom';
import { SessionActions } from '../components/SessionActions.jsx';

export function AppLayout() {
  return (
    <>
      <header className="topbar">
        <Link className="brand" to="/">SolveStack</Link>
        <SessionActions />
      </header>
      <main className="page-shell"><Outlet /></main>
    </>
  );
}
