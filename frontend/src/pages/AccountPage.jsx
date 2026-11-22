import React, { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useSession } from '../context/SessionContext.jsx';

export function AccountPage({ mode }) {
  const { status, identity, signIn, signUp } = useSession();
  const [submitting, setSubmitting] = useState(false);
  const [problem, setProblem] = useState({});
  const isRegistration = mode === 'register';
  const label = isRegistration ? 'Create account' : 'Sign in';

  async function handleSubmit(event) {
    event.preventDefault();
    setProblem({});
    const form = new FormData(event.currentTarget);
    const credentials = { email: form.get('email').trim(), password: form.get('password') };

    if (new TextEncoder().encode(credentials.password).length > 72) {
      setProblem({ errors: { password: ['Use a password of at most 72 UTF-8 bytes.'] } });
      return;
    }

    setSubmitting(true);
    try {
      await (isRegistration ? signUp : signIn)(credentials);
    } catch (error) {
      const response = error.response;
      if (response?.status < 500 && response.data?.detail) {
        setProblem(response.data);
      } else {
        setProblem({ detail: 'Could not reach SolveStack. Try again.' });
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (identity?.kind === 'ACCOUNT') return <Navigate to="/" replace />;

  const errors = problem.errors || {};
  return (
    <section className="panel account-panel">
      <Link className="back-link" to="/">← Back to dashboard</Link>
      <h1>{label}</h1>
      <p>{isRegistration ? 'Keep your guest tracker and use it across browsers.' : 'Open your SolveStack tracker.'}</p>
      <form className="account-form" onSubmit={handleSubmit}>
        <label htmlFor="email">Email address</label>
        <input id="email" name="email" type="email" autoComplete="email" maxLength={254} required
          aria-invalid={Boolean(errors.email)} aria-describedby={errors.email ? 'email-error' : undefined} />
        {errors.email && <span className="field-error" id="email-error" role="alert">{errors.email[0]}</span>}

        <label htmlFor="password">Password</label>
        <input id="password" name="password" type="password" required
          autoComplete={isRegistration ? 'new-password' : 'current-password'}
          minLength={isRegistration ? 12 : undefined} maxLength={72}
          aria-invalid={Boolean(errors.password)}
          aria-describedby={[isRegistration && 'password-help', errors.password && 'password-error'].filter(Boolean).join(' ') || undefined} />
        {isRegistration && <small id="password-help">Use at least 12 characters.</small>}
        {errors.password && <span className="field-error" id="password-error" role="alert">{errors.password[0]}</span>}

        {problem.detail && !Object.keys(errors).length && <p className="field-error" role="alert">{problem.detail}</p>}
        <button className="button" type="submit" disabled={status !== 'ready' || submitting}>
          {submitting ? (isRegistration ? 'Creating account…' : 'Signing in…') : label}
        </button>
      </form>
      <p>
        {isRegistration ? 'Already have an account? ' : 'Need an account? '}
        <Link to={isRegistration ? '/login' : '/register'}>{isRegistration ? 'Sign in' : 'Create account'}</Link>
      </p>
    </section>
  );
}
