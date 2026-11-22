import React from 'react';
import { Navigate } from 'react-router-dom';
import { TrackingActionError, TrackingStatus } from '../components/TrackingStatus.jsx';
import { useTrackingAction } from '../hooks/useTrackingAction.js';
import { useTrackingResource } from '../hooks/useTrackingResource.js';
import { getProgress, resetTracker } from '../services/tracking.js';

export function CompletionPage() {
  const resource = useTrackingResource(getProgress);
  const { pending, failure, run } = useTrackingAction();

  if (resource.status !== 'ready') return <TrackingStatus {...resource} />;

  const { overall } = resource.data;
  if (!overall.complete) {
    return pending !== null || resource.refreshing
      ? <TrackingStatus status="loading" />
      : <Navigate to="/" replace />;
  }

  const restarting = pending === 'restart';
  return (
    <section className="panel completion-panel">
      <div className="completion-icon" aria-hidden="true">✓</div>
      <h1>Journey complete</h1>
      <p>You solved all {overall.totalProblems} problems.</p>
      <button
        className="button"
        type="button"
        disabled={pending !== null || resource.refreshing || failure?.sessionEnded === true}
        onClick={() => run('restart', resetTracker, 'Could not restart your tracker. Try again.')}
      >
        {restarting ? 'Restarting…' : 'Restart the journey'}
      </button>
      <p className="restart-help">Restarting clears your saved completions.</p>
      <TrackingActionError failure={failure} />
    </section>
  );
}
