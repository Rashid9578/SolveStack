import React from 'react';
import { Navigate } from 'react-router-dom';
import { ProgressBar } from '../components/ProgressBar.jsx';
import { ProgressSummary } from '../components/ProgressSummary.jsx';
import { TopicCard } from '../components/TopicCard.jsx';
import { TrackingStatus } from '../components/TrackingStatus.jsx';
import { useTracking } from '../context/TrackingContext.jsx';
import { useTrackingResource } from '../hooks/useTrackingResource.js';
import { getProgress } from '../services/tracking.js';

export function DashboardPage() {
  const resource = useTrackingResource(getProgress);
  const { pending } = useTracking();
  if (resource.status !== 'ready') return <TrackingStatus {...resource} />;

  const { overall, topics } = resource.data;
  if (overall.complete && !resource.refreshing && pending === null) return <Navigate to="/complete" replace />;

  return (
    <>
      <section className="panel hero">
        <div>
          <h1>Track your DSA practice</h1>
          <p>Pick a topic, solve a problem, and mark it done.</p>
        </div>
        <ProgressSummary metrics={[
          { label: 'Overall progress', value: overall.percentage + '%' },
          { label: 'Solved', value: overall.completedProblems + ' / ' + overall.totalProblems },
        ]} />
      </section>
      <div className="panel progress-panel">
        <ProgressBar progress={overall} label="Overall progress" />
      </div>
      <ul className="topic-grid" aria-label="Topics">
        {topics.map((topic) => <li key={topic.slug}><TopicCard topic={topic} /></li>)}
      </ul>
    </>
  );
}
