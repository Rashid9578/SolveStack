import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useSession } from '../context/SessionContext.jsx';

export function SessionActions() {
  const { status, identity, retry, signOut } = useSession();
  const [signingOut, setSigningOut] = useState(false);
  const [error, setError] = useState('');

  async function handleSignOut() {
    setSigningOut(true);
    setError('');
    try {
      await signOut();
    } catch {
      setError('Could not sign out. Try again.');
    } finally {
      setSigningOut(false);
    }
  }

  if (status === 'loading') return <span role="status">Connecting session…</span>;
  if (status === 'error') {
    return <button className="account-action" type="button" onClick={retry}>Retry session</button>;
  }

  if (identity.kind === 'ACCOUNT') {
    return (
      <div className="session-actions">
        <span className="session-email">{identity.email}</span>
        <button className="account-action" type="button" onClick={handleSignOut} disabled={signingOut}>
          {signingOut ? 'Signing out…' : 'Sign out'}
        </button>
        {error && <span role="alert">{error}</span>}
      </div>
    );
  }

  return (
    <nav className="session-actions" aria-label="Account">
      <span>Guest</span>
      <Link className="account-link" to="/login">Sign in</Link>
      <Link className="account-action" to="/register">Create account</Link>
    </nav>
  );
}
