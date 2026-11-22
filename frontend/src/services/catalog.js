import axios from 'axios';

const api = axios.create({ baseURL: '/api', timeout: 10000 });

export async function listTopics(signal) {
  const { data } = await api.get('/topics', { signal });
  return data;
}

export async function getTopic(slug, signal) {
  const { data } = await api.get('/topics/' + encodeURIComponent(slug), { signal });
  return data;
}
