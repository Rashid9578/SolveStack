import assert from 'node:assert/strict';
import { once } from 'node:events';
import { readFile } from 'node:fs/promises';
import { createServer } from 'node:http';
import { setTimeout as delay } from 'node:timers/promises';
import { test } from 'node:test';
import { JSDOM } from 'jsdom';

const TOPIC = '/topic/array-and-string';
const TOPIC_API = '/api/me/topics/array-and-string';
const MARK_FIRST = '/api/me/completions/arr-1';
const MARK_LAST = '/api/me/completions/arr-2';

const html = await readFile(new URL('../dist/index.html', import.meta.url), 'utf8');
const scriptPath = html.match(/<script[^>]+src="([^"]+)"/)?.[1];
const bundle = await readFile(new URL('../dist' + scriptPath, import.meta.url), 'utf8');
const problems = [
  { id: 'arr-1', title: 'First exercise', externalUrl: 'https://example.com/first', position: 1 },
  { id: 'arr-2', title: 'Second exercise', externalUrl: 'https://example.com/second', position: 2 },
];

async function fixture(t, initial = []) {
  const solved = new Set(initial);
  const calls = [];
  const failures = new Map();
  let release;
  let hold;
  let sessionKind = 'GUEST';

  function stats() {
    return {
      completedProblems: solved.size,
      totalProblems: problems.length,
      percentage: 100 * solved.size / problems.length,
      complete: solved.size === problems.length,
    };
  }

  function topic() {
    return { slug: 'array-and-string', name: 'Array and String', accent: 'green', progress: stats() };
  }

  const server = createServer(async (request, response) => {
    const { method, url } = request;
    const route = method + ' ' + url;
    calls.push({ route, csrf: request.headers['x-csrf-token'] });

    function send(status, data) {
      response.writeHead(status, { 'Content-Type': 'application/json' });
      response.end(data === undefined ? undefined : JSON.stringify(data));
    }

    if (failures.has(route)) {
      const status = failures.get(route);
      failures.delete(route);
      send(status, { detail: 'Test response' });
      return;
    }

    if (hold?.route === route) {
      const { waiting } = hold;
      hold = undefined;
      await waiting;
    }

    if (route === 'GET /api/session') {
      send(200, { kind: sessionKind });
    } else if (route === 'GET /api/session/csrf') {
      send(200, { headerName: 'X-CSRF-TOKEN', token: 'fixture-token' });
    } else if (route === 'GET /api/me/progress') {
      send(200, { overall: stats(), topics: [topic()] });
    } else if (route === 'GET ' + TOPIC_API) {
      send(200, {
        ...topic(), overall: stats(),
        problems: problems.map((problem) => ({ ...problem, completed: solved.has(problem.id) })),
      });
    } else if (method === 'PUT' || method === 'DELETE') {
      if (request.headers['x-csrf-token'] !== 'fixture-token') {
        send(403, {});
      } else if (route === 'DELETE /api/me/completions') {
        solved.clear();
        send(204);
      } else {
        const id = url.split('/').at(-1);
        const done = method === 'PUT';
        if (done) solved.add(id);
        else solved.delete(id);
        send(200, { problemId: id, completed: done, overall: stats() });
      }
    } else if (route === 'POST /api/session/guest' && request.headers['x-csrf-token'] === 'fixture-token') {
      sessionKind = 'GUEST';
      send(201, { kind: sessionKind });
    } else {
      send(404, {});
    }
  });

  server.listen(0, '127.0.0.1');
  await once(server, 'listening');
  const origin = 'http://127.0.0.1:' + server.address().port;
  const windows = [];
  t.after(() => {
    release?.();
    for (const window of windows) window.close();
    server.close();
    server.closeAllConnections();
  });

  function open(route = '/') {
    const { window } = new JSDOM(html, {
      url: origin + route,
      runScripts: 'outside-only',
      pretendToBeVisual: true,
      beforeParse(window) { window.TextEncoder = TextEncoder; },
    });
    windows.push(window);
    window.eval(bundle);
    return window;
  }

  return {
    calls,
    open,
    fail: (route, status) => failures.set(route, status),
    expire() {
      sessionKind = 'ANONYMOUS';
      failures.set('GET /api/me/progress', 401);
    },
    pause(route) {
      hold = { route, waiting: new Promise((done) => { release = done; }) };
      return release;
    },
  };
}

async function waitFor(check, label) {
  const deadline = Date.now() + 5000;
  while (Date.now() < deadline) {
    const result = check();
    if (result) return result;
    await delay(20);
  }
  assert.fail('Timed out waiting for ' + label);
}

function click(window, selector) {
  const element = window.document.querySelector(selector);
  assert.ok(element, 'Expected ' + selector);
  element.dispatchEvent(new window.MouseEvent('click', { bubbles: true, cancelable: true, button: 0 }));
}

function text(window, selector) {
  return window.document.querySelector(selector)?.textContent;
}

function problemAction(index) {
  return '.problem-row:nth-child(' + index + ') .problem-action';
}

test('opens a topic, marks and undoes a problem, and displays the server totals', async (t) => {
  const api = await fixture(t);
  const window = api.open();
  await waitFor(() => text(window, '.summary dd') === '0%', 'dashboard progress');
  assert.equal(window.document.querySelector('#practice-tip').hidden, true);
  click(window, '.tip-toggle');
  await waitFor(() => !window.document.querySelector('#practice-tip').hidden, 'expanded tip');
  click(window, '.tip-toggle');
  await waitFor(() => window.document.querySelector('#practice-tip').hidden, 'collapsed tip');

  click(window, '.topic-card');
  await waitFor(() => text(window, 'h1') === 'Array and String', 'topic');
  const problemLink = window.document.querySelector('.problem-details a');
  assert.equal(problemLink.href, 'https://example.com/first');
  assert.equal(problemLink.target, '_blank');

  click(window, problemAction(1));
  await waitFor(() => text(window, problemAction(1)) === 'Undo', 'marked problem');
  assert.equal(window.document.querySelector('main .progress-bar').value, 50);
  click(window, '.back-link');
  await waitFor(() => window.document.querySelector('.topic-card') && text(window, '.summary dd') === '50%', 'updated dashboard');

  click(window, '.topic-card');
  await waitFor(() => text(window, problemAction(1)) === 'Undo', 'previously saved completion');
  click(window, problemAction(1));
  await waitFor(() => text(window, problemAction(1)) === 'Mark done', 'undone problem');
  assert.equal(window.document.querySelector('main .progress-bar').value, 0);
  assert.deepEqual(api.calls.filter(({ route }) => route.endsWith(MARK_FIRST)), [
    { route: 'PUT ' + MARK_FIRST, csrf: 'fixture-token' },
    { route: 'DELETE ' + MARK_FIRST, csrf: 'fixture-token' },
  ]);
});

test('refreshes the dashboard after an in-flight write and ignores duplicate clicks', async (t) => {
  const api = await fixture(t);
  const release = api.pause('PUT ' + MARK_FIRST);
  const window = api.open(TOPIC);
  await waitFor(() => text(window, problemAction(1)) === 'Mark done', 'topic actions');
  click(window, problemAction(1));
  click(window, problemAction(1));
  await waitFor(() => text(window, problemAction(1)) === 'Saving…', 'saving state');
  assert.equal(window.document.querySelector(problemAction(2)).getAttribute('aria-disabled'), 'true');
  click(window, '.back-link');
  await waitFor(() => text(window, '.summary dd') === '0%', 'dashboard before the pending write commits');
  release();
  await waitFor(() => text(window, '.summary dd') === '50%', 'dashboard refresh after commit');
  assert.equal(api.calls.filter(({ route }) => route === 'PUT ' + MARK_FIRST).length, 1);
});

test('guards completion, opens it after the final mark, and restarts the journey', async (t) => {
  const api = await fixture(t, ['arr-1']);
  const window = api.open('/complete');
  await waitFor(() => window.location.pathname === '/' && text(window, '.summary dd') === '50%', 'redirect to dashboard');
  click(window, '.topic-card');
  await waitFor(() => text(window, problemAction(2)) === 'Mark done', 'final problem');
  click(window, problemAction(2));
  await waitFor(() => window.location.pathname === '/complete' && text(window, 'h1') === 'Journey complete', 'completion screen');
  const reopened = api.open('/');
  await waitFor(() => reopened.location.pathname === '/complete' && text(reopened, 'h1') === 'Journey complete', 'completed dashboard redirect');
  click(reopened, '.completion-panel > button');
  await waitFor(() => reopened.location.pathname === '/' && text(reopened, '.summary dd') === '0%', 'clean restart');
  assert.equal(api.calls.filter(({ route }) => route === 'PUT ' + MARK_LAST).length, 1);
  assert.equal(api.calls.filter(({ route }) => route === 'DELETE /api/me/completions').length, 1);
});

test('recovers from failed reads and writes and reconnects an expired session', async (t) => {
  const api = await fixture(t);
  api.fail('GET /api/me/progress', 503);
  const window = api.open();
  await waitFor(() => text(window, 'main [role="alert"]')?.includes('Could not load'), 'read error');
  click(window, 'main .message button');
  await waitFor(() => text(window, '.summary dd') === '0%', 'successful retry');

  click(window, '.topic-card');
  await waitFor(() => text(window, problemAction(1)) === 'Mark done', 'topic actions');
  api.fail('PUT ' + MARK_FIRST, 503);
  click(window, problemAction(1));
  await waitFor(() => text(window, 'main [role="alert"]')?.includes('Could not update')
    && window.document.querySelector(problemAction(1)).getAttribute('aria-disabled') === 'false', 'write error and refresh');
  assert.equal(text(window, problemAction(1)), 'Mark done');

  api.expire();
  click(window, '.back-link');
  await waitFor(() => text(window, 'main [role="alert"]') === 'Your session ended.', 'expired session');
  click(window, 'main .message button');
  await waitFor(() => text(window, '.summary dd') === '0%', 'guest reconnect');
  assert.equal(api.calls.filter(({ route }) => route === 'POST /api/session/guest').length, 1);

  const missing = api.open('/topic/missing');
  await waitFor(() => text(missing, 'h1') === 'Topic not found', 'missing topic');
});

test('validates email and the BCrypt password byte limit on the registration form', async (t) => {
  const api = await fixture(t);
  const window = api.open('/register');
  const form = await waitFor(() => window.document.querySelector('.account-form'), 'registration form');
  const email = form.elements.namedItem('email');
  const password = form.elements.namedItem('password');
  email.value = 'invalid-address';
  assert.equal(email.checkValidity(), false);
  assert.equal(password.minLength, 12);

  email.value = 'practitioner@example.com';
  password.value = String.fromCodePoint(233).repeat(37);
  form.dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }));
  await waitFor(() => text(window, '#password-error')?.includes('72 UTF-8 bytes'), 'password validation');
  assert.equal(api.calls.some(({ route }) => route === 'POST /api/session/register'), false);
});
