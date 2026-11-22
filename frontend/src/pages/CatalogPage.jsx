import React from 'react';
import { Link } from 'react-router-dom';
import { CatalogStatus } from '../components/CatalogStatus.jsx';
import { useCatalogResource } from '../hooks/useCatalogResource.js';

export function CatalogPage() {
  const resource = useCatalogResource();
  if (resource.status !== 'ready') return <CatalogStatus {...resource} />;

  return (
    <>
      <section className="panel hero">
        <div>
          <h1>DSA problem catalog</h1>
          <p>Choose a topic to browse its practice problems.</p>
        </div>
        <div className="summary">
          <strong>{resource.data.totalProblems}</strong>
          <span>Practice problems</span>
        </div>
      </section>
      <ul className="topic-grid" aria-label="Topics">
        {resource.data.topics.map((topic) => (
          <li key={topic.slug}>
            <Link className={'topic-card accent-' + topic.accent} to={'/topic/' + topic.slug}>
              <h2>{topic.name}</h2>
              <span>{topic.problemCount} problems</span>
            </Link>
          </li>
        ))}
      </ul>
    </>
  );
}
