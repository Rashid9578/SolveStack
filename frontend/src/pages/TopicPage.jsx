import React, { useCallback, useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { ProblemRow } from '../components/ProblemRow.jsx';
import { ProgressBar } from '../components/ProgressBar.jsx';
import { ProgressSummary } from '../components/ProgressSummary.jsx';
import { TrackingActionError, TrackingStatus } from '../components/TrackingStatus.jsx';
import { useTrackingAction } from '../hooks/useTrackingAction.js';
import { useTrackingResource } from '../hooks/useTrackingResource.js';
import { getTrackedTopic, setCompletion } from '../services/tracking.js';
import { NotFoundPage } from './NotFoundPage.jsx';

export function TopicPage() {
  const { slug } = useParams();
  return <TopicView key={slug} slug={slug} />;
}

function TopicView({ slug }) {
  const load = useCallback((signal) => getTrackedTopic(slug, signal), [slug]);
  const resource = useTrackingResource(load);
  const { pending, failure, run } = useTrackingAction();
  const [journeyComplete, setJourneyComplete] = useState(false);

  async function toggle(problem) {
    const result = await run(problem.id, (signal) => setCompletion(problem.id, !problem.completed, signal),
      'Could not update this problem. Try again.');
    if (result?.overall.complete) setJourneyComplete(true);
  }

  if (journeyComplete) return <Navigate to="/complete" replace />;
  if (resource.status === 'not-found') return <NotFoundPage topic />;
  if (resource.status !== 'ready') return <TrackingStatus {...resource} />;

  const topic = resource.data;
  return (
    <>
      <section className="panel hero">
        <div>
          <Link className="back-link" to="/">← Back to dashboard</Link>
          <h1>{topic.name}</h1>
          <p>{topic.progress.completedProblems} of {topic.progress.totalProblems} problems complete.</p>
        </div>
        <ProgressSummary metrics={[
          { label: 'Topic progress', value: topic.progress.percentage + '%' },
          { label: 'Overall progress', value: topic.overall.percentage + '%' },
        ]} />
      </section>
      <div className="panel progress-panel">
        <ProgressBar progress={topic.progress} label={topic.name + ' progress'} />
      </div>
      <TrackingActionError failure={failure} />
      <ol className="problem-list" aria-label="Problems">
        {topic.problems.map((problem) => (
          <ProblemRow key={problem.id} problem={problem} onToggle={toggle}
            busy={pending !== null || resource.refreshing || failure?.sessionEnded === true} saving={pending === problem.id} />
        ))}
      </ol>
    </>
  );
}
