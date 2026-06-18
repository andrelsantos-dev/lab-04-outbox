CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE ROLE app_user LOGIN PASSWORD 'app_password';
CREATE ROLE migration_user LOGIN PASSWORD 'migration_password' BYPASSRLS;
CREATE ROLE worker_user LOGIN PASSWORD 'worker_password' BYPASSRLS;

GRANT ALL ON SCHEMA public TO migration_user;