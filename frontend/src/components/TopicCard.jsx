import React from 'react';
import { Link } from 'react-router-dom';
import { ProgressBar } from './ProgressBar.jsx';

export function TopicCard({ topic }) {
  const { progress } = topic;
  return (
    <Link className={'topic-card accent-' + topic.accent} to={'/topic/' + topic.slug}>
      <h2>{topic.name}</h2>
      <span>{progress.completedProblems} of {progress.totalProblems} complete</span>
      <ProgressBar progress={progress} label={topic.name + ' progress'} />
      <strong className="topic-progress-label">
        {progress.complete ? 'Completed' : progress.percentage + '% done'}
      </strong>
    </Link>
  );
}
