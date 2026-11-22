import { useEffect, useState } from 'react';
import { useTracking } from '../context/TrackingContext.jsx';

export function useTrackingResource(load) {
  const { revision } = useTracking();
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState(null);

  useEffect(() => {
    const controller = new AbortController();

    load(controller.signal).then(
      (data) => {
        if (!controller.signal.aborted) {
          setResult({ status: 'ready', revision, attempt, data });
        }
      },
      (error) => {
        if (controller.signal.aborted) return;
        const code = error.response?.status;
        const status = code === 401 ? 'session-ended' : code === 404 ? 'not-found' : 'error';
        setResult({ status, revision, attempt });
      },
    );

    return () => controller.abort();
  }, [load, revision, attempt]);

  const current = result?.revision === revision && result.attempt === attempt;
  const visible = current || result?.status === 'ready' ? result : { status: 'loading' };
  return { ...visible, refreshing: !current, retry: () => setAttempt((attempt) => attempt + 1) };
}
