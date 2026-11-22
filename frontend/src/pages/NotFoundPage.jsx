import React from 'react';
import { Link } from 'react-router-dom';

export function NotFoundPage({ topic = false }) {
  return (
    <section className="panel message">
      <h1>{topic ? 'Topic not found' : 'Page not found'}</h1>
      <Link className="back-link" to="/">Back to dashboard</Link>
    </section>
  );
}
