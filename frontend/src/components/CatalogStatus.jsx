import React from 'react';

export function CatalogStatus({ status, retry }) {
  if (status !== 'loading') {
    return (
      <div className="panel message" role="alert">
        <p>Could not load the catalog.</p>
        <button className="button" type="button" onClick={retry}>Try again</button>
      </div>
    );
  }

  return <p className="panel message" role="status">Loading catalog…</p>;
}
