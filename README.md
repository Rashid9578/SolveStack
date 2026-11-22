# SolveStack

SolveStack is a data structures and algorithms practice tracker. The catalog has 100 problems across 12 topics, with links to the original practice sites.

## Local setup

Install Java 21 or later, Maven 3.6.3 or later, Node.js 20.19 or later in the 20.x line or Node.js 22.12 or later, and PostgreSQL. Create a PostgreSQL database and a database role for SolveStack. The role needs permission to create and update objects in the application's schema so Flyway can apply migrations.

Start the Spring Boot API from the project root in PowerShell. Set the database connection values for your local database.

```powershell
$env:SOLVESTACK_DB_URL = 'jdbc:postgresql://localhost:5432/solvestack'
$databaseCredential = Get-Credential -UserName 'solvestack' -Message 'Enter the PostgreSQL password'
$env:SOLVESTACK_DB_USERNAME = $databaseCredential.UserName
$env:SOLVESTACK_DB_PASSWORD = $databaseCredential.GetNetworkCredential().Password
mvn -f backend/pom.xml spring-boot:run
```

In another terminal, start the React development server.

```powershell
cd frontend
npm ci
npm run dev
```

Open the URL printed by Vite. In development, Vite forwards `/api` requests to Spring Boot at `http://127.0.0.1:8080`. Set `API_PROXY_TARGET` in the frontend terminal to change that address.

## Catalog API

| Method | Path | Response |
| --- | --- | --- |
| GET | `/api/topics` | Total number of problems and ordered topic summaries |
| GET | `/api/topics/{slug}` | Topic information and ordered problems |

An unknown topic returns HTTP 404 with an `application/problem+json` body.

## Checks

Run `mvn -f backend/pom.xml test` for the catalog API tests and `npm run build` from `frontend/` for the production frontend build. To include the PostgreSQL integration test, point the database variables at a dedicated test database and set `SOLVESTACK_POSTGRES_TEST=true` before running Maven. Flyway applies the same schema and seed used by the application.
