import React from 'react';
import { Link, useParams } from 'react-router-dom';
import { CatalogStatus } from '../components/CatalogStatus.jsx';
import { useCatalogResource } from '../hooks/useCatalogResource.js';
import { NotFoundPage } from './NotFoundPage.jsx';

export function TopicPage() {
  const { slug } = useParams();
  const resource = useCatalogResource(slug);
  if (resource.status === 'not-found') return <NotFoundPage topic />;
  if (resource.status !== 'ready') return <CatalogStatus {...resource} />;

  const topic = resource.data;
  return (
    <>
      <section className="panel hero">
        <div>
          <Link className="back-link" to="/">← Back to catalog</Link>
          <h1>{topic.name}</h1>
          <p>{topic.problems.length} practice problems</p>
        </div>
      </section>
      <ol className="problem-list" aria-label="Problems">
        {topic.problems.map((problem) => (
          <li className="panel problem-row" key={problem.id}>
            <span className="problem-number" aria-hidden="true">{problem.position}</span>
            <a href={problem.externalUrl} target="_blank" rel="noreferrer">{problem.title}</a>
          </li>
        ))}
      </ol>
    </>
  );
}
