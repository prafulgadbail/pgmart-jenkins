#!/bin/bash
# Runs once, when the local database volume is new.
# Same layout as RDS: one database and one login per service, so a service
# can read and write only its own data.
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<EOSQL
CREATE USER user_svc    WITH PASSWORD '${USER_DB_PASSWORD}';
CREATE USER catalog_svc WITH PASSWORD '${CATALOG_DB_PASSWORD}';
CREATE USER order_svc   WITH PASSWORD '${ORDER_DB_PASSWORD}';

CREATE DATABASE userdb    OWNER user_svc;
CREATE DATABASE catalogdb OWNER catalog_svc;
CREATE DATABASE orderdb   OWNER order_svc;
EOSQL
