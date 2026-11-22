import React from 'react';

export function ProgressSummary({ metrics }) {
  return (
    <dl className="progress-metrics" aria-live="polite">
      {metrics.map(({ label, value }) => (
        <div className="summary" key={label}>
          <dt>{label}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  );
}
