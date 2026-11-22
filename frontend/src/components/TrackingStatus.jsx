import React from 'react';
import { useSession } from '../context/SessionContext.jsx';

export function TrackingStatus({ status, retry }) {
  const { retry: reconnect } = useSession();

  if (status === 'loading') {
    return <p className="panel message" role="status">Loading progress…</p>;
  }

  const sessionEnded = status === 'session-ended';
  return (
    <div className="panel message">
      <p role="alert">{sessionEnded ? 'Your session ended.' : 'Could not load your progress.'}</p>
      <button className="button" type="button" onClick={sessionEnded ? reconnect : retry}>
        {sessionEnded ? 'Reconnect session' : 'Try again'}
      </button>
    </div>
  );
}

export function TrackingActionError({ failure }) {
  const { retry: reconnect } = useSession();
  if (!failure) return null;

  return (
    <div className="panel tracking-feedback">
      <p role="alert">{failure.message}</p>
      {failure.sessionEnded && (
        <button className="button" type="button" onClick={reconnect}>Reconnect session</button>
      )}
    </div>
  );
}
