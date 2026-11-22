# SolveStack deployment

SolveStack serves React and the Spring Boot API from one origin. NGINX serves static files and forwards `/api/` to Spring Boot. PostgreSQL stores both progress and sessions. The database bootstrap creates a separate application login that owns only the SolveStack database. Flyway applies application migrations using that login.

## Generate local database credentials

Run from the repository root in PowerShell. This creates separate passwords without printing them or replacing existing files.

```powershell
New-Item -ItemType Directory -Force .secrets | Out-Null
$random = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    foreach ($file in '.secrets/postgres_admin_password', '.secrets/postgres_password') {
        if (-not (Test-Path -LiteralPath $file)) {
            $bytes = New-Object byte[] 32
            $random.GetBytes($bytes)
            Set-Content -LiteralPath $file -Value ([Convert]::ToBase64String($bytes)) -Encoding Ascii -NoNewline
        }
    }
} finally {
    $random.Dispose()
}
```

Keep the `.secrets` directory private. On Linux, keep its mode at `0700` and set the two password files to `0444` so the non-root containers can read the bind-mounted files. For an existing database volume, change credentials through PostgreSQL before updating the corresponding deployment secret. The bootstrap runs only when initializing an empty data directory.

## Docker Compose

Use Docker with Compose V2 and BuildKit. Start the local stack from the repository root.

```powershell
docker compose config --quiet
docker compose up --build --wait --wait-timeout 180
docker compose ps
```

Open `http://127.0.0.1:3000`. To use a different port, copy `.env.example` to `.env` and set `SOLVESTACK_HTTP_PORT`.

Only the frontend publishes a host port. The backend and database connect on a private Compose network, and the database uses a named volume. Compose waits for PostgreSQL before starting Spring Boot and for API readiness before starting the frontend. The Compose configuration uses HTTP for local development.

Run the smoke test with Node.js from the repository root. It opens the dashboard and a direct topic URL, starts a guest session, and verifies mark and undo through NGINX.

```powershell
node deployment/smoke.mjs http://127.0.0.1:3000
```

To verify retention, add `--pause-for-restart` to the smoke command. While it waits, run `docker compose restart postgres` in another terminal, then press Enter. The smoke test checks that both the original guest session and its saved completion survive the restart.

Use `docker compose logs` to inspect startup or migration output. `docker compose down` stops the stack and retains PostgreSQL data in the named volume.

## Kubernetes

The base uses two Deployments, internal Services, a PostgreSQL StatefulSet with a volume claim, and a TLS Ingress. For a local smoke test, create a kind cluster and load the app images.

```powershell
docker compose build
kind create cluster --name solvestack
kind load docker-image solvestack/frontend:0.1.0 solvestack/backend:0.1.0 --name solvestack
```

If you already have a local cluster, use its name for `--name`. For a shared cluster, update the image references to versioned images available to its nodes.

Create the namespace and database Secret before applying the workloads. The checked-in Secret example records the expected keys. The Kustomize base uses the database Secret created from your local credential files.

```powershell
kubectl apply -f deployment/kubernetes/namespace.yaml
kubectl -n solvestack create secret generic solvestack-db --from-file=POSTGRES_ADMIN_PASSWORD=.secrets/postgres_admin_password --from-file=SOLVESTACK_DB_PASSWORD=.secrets/postgres_password
kubectl kustomize deployment/kubernetes
kubectl apply -k deployment/kubernetes
kubectl -n solvestack rollout status statefulset/postgres --timeout=180s
kubectl -n solvestack rollout status deployment/backend --timeout=180s
kubectl -n solvestack rollout status deployment/frontend --timeout=180s
kubectl -n solvestack get pods,services,pvc,ingress
```

The PostgreSQL StatefulSet requests a 2 GiB volume from the cluster's default StorageClass. For a static local volume, use `postgres-pv.example.yaml` as a starting point. Set its node hostname and prepare the path on that node for the PostgreSQL container's UID and GID 999. Before the first rollout, set `storageClassName: ""` in the StatefulSet volume claim template, create the PV, and confirm that the claim `data-postgres-0` is bound. The example PV retains its data when the claim is released.

For browser access through a local HTTP port-forward, set `SOLVESTACK_SESSION_COOKIE_SECURE` to `"false"` in the local ConfigMap and restart the backend.

```powershell
$cookiePatch = New-TemporaryFile
'{"data":{"SOLVESTACK_SESSION_COOKIE_SECURE":"false"}}' | Set-Content -LiteralPath $cookiePatch.FullName -Encoding Ascii
kubectl -n solvestack patch configmap solvestack-config --type merge --patch-file $cookiePatch.FullName
Remove-Item -LiteralPath $cookiePatch.FullName
kubectl -n solvestack rollout restart deployment/backend
kubectl -n solvestack rollout status deployment/backend --timeout=180s
kubectl -n solvestack port-forward svc/frontend 3000:8080
```

Run the smoke test against `http://127.0.0.1:3000` in another terminal. Use `kubectl -n solvestack rollout restart statefulset/postgres` during the optional persistence check.

For HTTPS, set the hostname in both sections of `ingress.yaml`, configure the default IngressClass or set `spec.ingressClassName` for your controller, and point DNS at it. Provision `solvestack-tls` in the `solvestack` namespace before serving traffic. The Kubernetes configuration sets secure cookies by default.

If your cluster uses a custom DNS suffix, update `SOLVESTACK_API_UPSTREAM` in `config.yaml` to use the backend Service's fully qualified name.

## Environment and probes

| Setting | Purpose |
| --- | --- |
| `SOLVESTACK_HTTP_PORT` | Compose loopback port, default `3000` |
| `SOLVESTACK_DB_URL` | Spring Boot PostgreSQL JDBC URL |
| `SOLVESTACK_DB_USERNAME` | Application PostgreSQL role |
| `SOLVESTACK_DB_PASSWORD` | Application password from the environment or mounted config tree |
| `SOLVESTACK_DB_PASSWORD_FILE` | Path overriding the Compose app password file |
| `SOLVESTACK_DB_ADMIN_PASSWORD_FILE` | Path overriding the Compose PostgreSQL bootstrap password file |
| `SOLVESTACK_SESSION_COOKIE_SECURE` | Set to `true` for HTTPS and `false` for local HTTP |
| `SOLVESTACK_API_UPSTREAM` | Internal backend host and port used by NGINX |
| `SPRING_CONFIG_IMPORT` | Spring Boot config tree path for the mounted secret |

NGINX responds at `/healthz`. The backend's Kubernetes liveness probe checks `/livez`, and readiness at `/readyz` includes database connectivity. The backend's startup probe gives Flyway time to apply migrations before liveness begins.
