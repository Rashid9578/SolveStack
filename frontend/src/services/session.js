import { api } from './api.js';

async function postSession(path, body) {
  const { data: csrf } = await api.get('/session/csrf');
  const { data } = await api.post('/session/' + path, body, {
    headers: { [csrf.headerName]: csrf.token },
  });
  return data;
}

let bootstrap;

export function startOrResumeSession() {
  if (!bootstrap) {
    bootstrap = api.get('/session')
      .then(({ data }) => data.kind === 'ANONYMOUS' ? postSession('guest') : data)
      .finally(() => { bootstrap = undefined; });
  }
  return bootstrap;
}

export function register(credentials) {
  return postSession('register', credentials);
}

export function login(credentials) {
  return postSession('login', credentials);
}

export function logout() {
  return postSession('logout');
}
