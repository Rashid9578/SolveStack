import { useEffect, useState } from 'react';
import { getTopic, listTopics } from '../services/catalog.js';

const loading = { status: 'loading' };

export function useCatalogResource(slug) {
  const [result, setResult] = useState(loading);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    const request = slug === undefined
      ? listTopics(controller.signal)
      : getTopic(slug, controller.signal);

    request.then(
      (data) => {
        if (!controller.signal.aborted) {
          setResult({ slug, attempt, status: 'ready', data });
        }
      },
      (error) => {
        if (!controller.signal.aborted) {
          const status = error.response?.status === 404 ? 'not-found' : 'error';
          setResult({ slug, attempt, status });
        }
      },
    );

    return () => controller.abort();
  }, [slug, attempt]);

  const current = result.slug === slug && result.attempt === attempt ? result : loading;
  return { ...current, retry: () => setAttempt((value) => value + 1) };
}
