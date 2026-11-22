import { api, requestWithCsrf } from './api.js';

export async function getProgress(signal) {
  const { data } = await api.get('/me/progress', { signal });
  return data;
}

export async function getTrackedTopic(slug, signal) {
  const { data } = await api.get('/me/topics/' + encodeURIComponent(slug), { signal });
  return data;
}

export function setCompletion(problemId, completed, signal) {
  const method = completed ? 'PUT' : 'DELETE';
  return requestWithCsrf(method, '/me/completions/' + encodeURIComponent(problemId), { signal });
}

export function resetTracker(signal) {
  return requestWithCsrf('DELETE', '/me/completions', { signal });
}
