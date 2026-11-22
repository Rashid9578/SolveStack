import React from 'react';

export function ProblemRow({ problem, busy, saving, onToggle }) {
  const done = problem.completed;
  const label = done ? 'Undo' : 'Mark done';
  const actionLabel = done ? 'Undo completion for ' + problem.title : 'Mark done: ' + problem.title;

  return (
    <li className={'panel problem-row' + (done ? ' problem-row-done' : '')}>
      <span className="problem-number" aria-hidden="true">{problem.position}</span>
      <div className="problem-details">
        <a href={problem.externalUrl} target="_blank" rel="noreferrer">{problem.title}</a>
        <span>{done ? 'Completed' : 'Incomplete'}</span>
      </div>
      <button
        className={'button problem-action' + (done ? ' button-secondary' : '')}
        type="button"
        aria-label={saving ? 'Saving: ' + problem.title : actionLabel}
        aria-disabled={busy}
        onClick={() => { if (!busy) onToggle(problem); }}
      >
        {saving ? 'Saving…' : label}
      </button>
    </li>
  );
}
