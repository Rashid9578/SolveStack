import assert from 'node:assert/strict';
import { createInterface } from 'node:readline/promises';
import { setTimeout as delay } from 'node:timers/promises';

const [base = 'http://127.0.0.1:3000', mode] = process.argv.slice(2);
assert.ok(mode === undefined || mode === '--pause-for-restart', 'Expected --pause-for-restart');
const origin = new URL(base).origin;
let cookie;

async function request(path, method = 'GET', headers = {}) {
  const response = await fetch(origin + path, {
    method,
    headers: cookie ? { ...headers, Cookie: cookie } : headers,
    signal: AbortSignal.timeout(5000),
  });
  for (const value of response.headers.getSetCookie()) {
    if (value.startsWith('SOLVESTACK_SESSION=')) cookie = value.split(';', 1)[0];
  }
  return response;
}

async function json(path, expected = 200, method = 'GET', headers) {
  const response = await request(path, method, headers);
  assert.equal(response.status, expected, method + ' ' + path);
  return response.json();
}

async function csrf() {
  const token = await json('/api/session/csrf');
  return { [token.headerName]: token.token };
}

const home = await request('/');
assert.equal(home.status, 200);
assert.match(await home.text(), /<title>SolveStack<\/title>/);
const topic = await request('/topic/array-and-string');
assert.equal(topic.status, 200);
assert.match(await topic.text(), /<title>SolveStack<\/title>/);
assert.equal((await request('/healthz')).status, 200);
assert.equal((await json('/api/topics')).totalProblems, 100);

const guest = await json('/api/session/guest', 201, 'POST', await csrf());
assert.equal(guest.kind, 'GUEST');
assert.match(cookie, /^SOLVESTACK_SESSION=.+/);
const completion = '/api/me/completions/arr-1';
assert.equal((await json(completion, 200, 'PUT', await csrf())).overall.completedProblems, 1);

if (mode === '--pause-for-restart') {
  console.log('One completion is saved for this guest session.');
  const terminal = createInterface({ input: process.stdin, output: process.stdout });
  try {
    await terminal.question('Restart PostgreSQL in another terminal, then press Enter. ');
  } finally {
    terminal.close();
  }

  for (let attempt = 1; ; attempt++) {
    try {
      const response = await request('/api/me/progress');
      if (response.ok) {
        assert.equal((await response.json()).overall.completedProblems, 1, 'Progress survived the restart');
        break;
      }
      if (response.status === 401) throw new Error('The guest session did not survive the restart.');
      if (![500, 502, 503, 504].includes(response.status)) {
        throw new Error('Unexpected progress response ' + response.status);
      }
    } catch (error) {
      if (!(error instanceof TypeError) && !['AbortError', 'TimeoutError'].includes(error.name)) throw error;
    }
    if (attempt === 30) throw new Error('The deployment did not recover after PostgreSQL restarted.');
    await delay(2000);
  }
}

assert.equal((await json('/api/me/progress')).overall.completedProblems, 1);
assert.equal((await json(completion, 200, 'DELETE', await csrf())).overall.completedProblems, 0);
console.log('SolveStack smoke test passed' + (mode ? ', including PostgreSQL restart persistence.' : '.'));
