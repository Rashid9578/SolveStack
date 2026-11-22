import React from 'react';
import { Outlet } from 'react-router-dom';
import { TrackingStatus } from '../components/TrackingStatus.jsx';
import { useSession } from '../context/SessionContext.jsx';
import { TrackingProvider } from '../context/TrackingContext.jsx';

export function TrackingLayout() {
  const { status, identity, retry } = useSession();
  if (status !== 'ready') return <TrackingStatus status={status} retry={retry} />;

  const trackerKey = identity.email || identity.kind;
  return (
    <TrackingProvider key={trackerKey}>
      <Outlet />
    </TrackingProvider>
  );
}
