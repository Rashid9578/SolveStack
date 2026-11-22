import { useState } from 'react';
import { useTracking } from '../context/TrackingContext.jsx';

export function useTrackingAction() {
  const { pending, write } = useTracking();
  const [failure, setFailure] = useState(null);

  async function run(key, request, message) {
    setFailure(null);
    try {
      return await write(key, request);
    } catch (error) {
      if (error.code === 'ERR_CANCELED') return undefined;
      const status = error.response?.status;
      setFailure(status === 401 || status === 403
        ? { sessionEnded: true, message: 'The session changed before this action could finish.' }
        : { message });
      return undefined;
    }
  }

  return { pending, failure, run };
}
