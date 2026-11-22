# SolveStack

SolveStack is a data structures and algorithms practice tracker. The catalog has 100 problems across 12 topics, with links to the original practice sites.

Open a topic from the dashboard, follow a problem link, and mark it done when you finish. Marked problems update the topic and overall totals, and you can undo any completion. Finishing the last problem opens the completion screen, where you can restart the tracker.

## Local setup

Install Java 21 or later, Maven 3.6.3 or later, Node.js 20.19 or later in the 20.x line or Node.js 22.12 or later, and PostgreSQL. Create a PostgreSQL database and a database role for SolveStack. The role needs permission to create and update objects in the application's schema so Flyway can apply migrations.

Start the Spring Boot API from the project root in PowerShell. Set the database connection values for your local database.

```powershell
$env:SOLVESTACK_DB_URL = 'jdbc:postgresql://localhost:5432/solvestack'
$databaseCredential = Get-Credential -UserName 'solvestack' -Message 'Enter the PostgreSQL password'
$env:SOLVESTACK_DB_USERNAME = $databaseCredential.UserName
$env:SOLVESTACK_DB_PASSWORD = $databaseCredential.GetNetworkCredential().Password
$env:SOLVESTACK_SESSION_COOKIE_SECURE = 'false'
mvn -f backend/pom.xml spring-boot:run
```

In another terminal, start the React development server.

```powershell
cd frontend
npm ci
npm run dev
```

Open the URL printed by Vite. In development, Vite forwards `/api` requests to Spring Boot at `http://127.0.0.1:8080`. Set `API_PROXY_TARGET` in the frontend terminal to change that address.

Use `SOLVESTACK_SESSION_COOKIE_SECURE=false` for local HTTP development. The default is `true` for HTTPS deployments.

## Catalog API

| Method | Path | Response |
| --- | --- | --- |
| GET | `/api/topics` | Total number of problems and ordered topic summaries |
| GET | `/api/topics/{slug}` | Topic information and ordered problems |

An unknown topic returns HTTP 404 with an `application/problem+json` body.

## Guest access and accounts

The frontend starts or resumes a guest session when you open SolveStack. A guest cookie lasts up to one year. Creating an account upgrades that guest tracker in place, so any progress belongs to the new account. Signing in opens the existing account's tracker. Signing out ends the current session and opens a fresh guest tracker.

Account sessions expire after 30 minutes of inactivity. Session cookies are `HttpOnly` and `SameSite=Lax`. Passwords use BCrypt. Registration requires at least 12 characters and accepts up to 72 UTF-8 bytes.

| Method | Path | Response |
| --- | --- | --- |
| GET | `/api/session` | `ANONYMOUS`, `GUEST`, or `ACCOUNT`, with the email for an account |
| GET | `/api/session/csrf` | The header name and CSRF token for the next session request |
| POST | `/api/session/guest` | Starts a guest session or returns the current session |
| POST | `/api/session/register` | Registers with an `email` and `password` and signs in |
| POST | `/api/session/login` | Signs in with an `email` and `password` |
| POST | `/api/session/logout` | Invalidates the current session and returns HTTP 204 |

Keep the session cookie when fetching the CSRF token and submitting the `POST` request. Fetch a fresh token after a session changes. Duplicate registration returns HTTP 409. Invalid sign-in credentials return HTTP 401.

## Tracking API

Tracking requests use the active guest or account session. Send a token from `/api/session/csrf` with each `PUT` or `DELETE` request. The server identifies the tracker from the session cookie.

| Method | Path | Response |
| --- | --- | --- |
| GET | `/api/me/progress` | Overall progress and progress for each topic in catalog order |
| GET | `/api/me/topics/{slug}` | Topic and overall progress, plus ordered problems with a `completed` flag |
| PUT | `/api/me/completions/{problemId}` | Marks a problem done and returns its ID, completion state, and updated overall progress |
| DELETE | `/api/me/completions/{problemId}` | Undoes completion and returns the same response shape |
| DELETE | `/api/me/completions` | Restarts the current tracker and returns HTTP 204 |

Each progress object has `completedProblems`, `totalProblems`, `percentage`, and `complete`. Percentages round to whole numbers. The overall `complete` flag becomes true after the last problem and clears after an undo or restart. Repeat requests to mark, undo, or restart are safe. An unknown topic or problem returns HTTP 404.

## Checks

Run `mvn -f backend/pom.xml test` for the catalog API tests. To include catalog, session, and tracking integration tests, point the database variables at a dedicated test database and set `SOLVESTACK_POSTGRES_TEST=true` before running Maven. Flyway applies the same schema and seed used by the application.

Run `npm test` from `frontend/` to build the production bundle and run the React workflow tests. These tests use a local API fixture and require no database.

## Deployment

Use the [deployment guide](docs/deployment.md) to start the Docker Compose stack or deploy the Kubernetes manifests. The guide covers credentials, persistent storage, TLS, and smoke tests.
