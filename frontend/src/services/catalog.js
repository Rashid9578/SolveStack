import { api } from './api.js';

export async function listTopics(signal) {
  const { data } = await api.get('/topics', { signal });
  return data;
}

export async function getTopic(slug, signal) {
  const { data } = await api.get('/topics/' + encodeURIComponent(slug), { signal });
  return data;
}
