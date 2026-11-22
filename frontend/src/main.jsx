import React, { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { SessionProvider } from './context/SessionContext.jsx';
import { AppLayout } from './layouts/AppLayout.jsx';
import { TrackingLayout } from './layouts/TrackingLayout.jsx';
import { AccountPage } from './pages/AccountPage.jsx';
import { CompletionPage } from './pages/CompletionPage.jsx';
import { DashboardPage } from './pages/DashboardPage.jsx';
import { NotFoundPage } from './pages/NotFoundPage.jsx';
import { TopicPage } from './pages/TopicPage.jsx';
import './styles/app.css';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <SessionProvider>
        <Routes>
          <Route element={<AppLayout />}>
            <Route element={<TrackingLayout />}>
              <Route index element={<DashboardPage />} />
              <Route path="topic/:slug" element={<TopicPage />} />
              <Route path="complete" element={<CompletionPage />} />
            </Route>
            <Route path="login" element={<AccountPage key="login" mode="login" />} />
            <Route path="register" element={<AccountPage key="register" mode="register" />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Routes>
      </SessionProvider>
    </BrowserRouter>
  </StrictMode>,
);
