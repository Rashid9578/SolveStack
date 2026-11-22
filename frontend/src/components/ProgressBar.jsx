import React from 'react';

export function ProgressBar({ progress, label }) {
  return <progress className="progress-bar" max={100} value={progress.percentage} aria-label={label} />;
}
