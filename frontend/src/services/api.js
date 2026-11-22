import axios from 'axios';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  withCredentials: true,
});

export async function requestWithCsrf(method, url, { data, signal } = {}) {
  const { data: csrf } = await api.get('/session/csrf', { signal });

  const response = await api.request({
    method,
    url,
    data,
    signal,
    headers: {
      [csrf.headerName]: csrf.token,
    },
  });

  return response.data;
}