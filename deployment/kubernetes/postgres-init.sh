#!/bin/sh

(
    set -eu
    SOLVESTACK_DB_PASSWORD=$(cat "${SOLVESTACK_DB_PASSWORD_FILE:?}")
    export SOLVESTACK_DB_PASSWORD
    psql --set=ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
        --set=app_user="${SOLVESTACK_DB_USERNAME:?}" --set=app_db="$POSTGRES_DB" <<'SQL'
\getenv app_password SOLVESTACK_DB_PASSWORD
CREATE ROLE :"app_user" LOGIN PASSWORD :'app_password';
ALTER DATABASE :"app_db" OWNER TO :"app_user";
SQL
)
