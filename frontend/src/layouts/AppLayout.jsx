import React from 'react';
import { Link, Outlet } from 'react-router-dom';

export function AppLayout() {
  return (
    <>
      <header className="topbar">
        <Link className="brand" to="/">SolveStack</Link>
      </header>
      <main className="page-shell"><Outlet /></main>
    </>
  );
}
