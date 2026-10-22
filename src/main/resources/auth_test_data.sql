-- Auth test seed data
--
-- This script inserts the minimal rows needed to support the provided users row:
--   external_user_id = cfe3fcd1-7520-45a5-9a50-d870d47e3b61
--   username = efe
--   password_hash = $2b$10$Cm.S3UnWmQTGLYbMxRvU1eqLoi5rGX/5DHwbnMrrIBSIeyI/ky0ZS
--   linked client = cfe3fcd1-7520-45a5-9a50-d870d47e3b60 with one linked account = cfe3fcd1-7520-45a5-9a50-d870d47e3b62
-- and a second client login, manan / manan, at the end of the file.
--
-- It also creates the matching client, account, client_accounts, and roles rows so the auth service
-- and the Java authorization flow can load the user cleanly.
-- Run schema.sql and sample-data.sql first. Nothing is deleted: the rows are added after the sample data, so the
-- internal IDs are whatever Postgres assigns and each row is linked by its external ID.

INSERT INTO clients (
    external_client_id,
    first_name,
    last_name,
    email,
    birth_date
)
VALUES (
    'cfe3fcd1-7520-45a5-9a50-d870d47e3b60',
    'Efe',
    'Test',
    'efe.test@example.com',
    DATE '1990-01-01'
);

INSERT INTO accounts (
    external_account_id,
    balance
)
VALUES (
    'cfe3fcd1-7520-45a5-9a50-d870d47e3b62',
    1000000.0000
);

INSERT INTO client_accounts (client_id, account_id)
SELECT clients.client_id, accounts.account_id
FROM clients, accounts
WHERE clients.external_client_id = 'cfe3fcd1-7520-45a5-9a50-d870d47e3b60'
  AND accounts.external_account_id = 'cfe3fcd1-7520-45a5-9a50-d870d47e3b62';

INSERT INTO users (
    external_user_id,
    username,
    password_hash,
    client_id,
    created_at
)
SELECT
    'cfe3fcd1-7520-45a5-9a50-d870d47e3b61',
    'efe',
    '$2b$10$Cm.S3UnWmQTGLYbMxRvU1eqLoi5rGX/5DHwbnMrrIBSIeyI/ky0ZS',
    client_id,
    TIMESTAMPTZ '2026-10-16 14:56:58.034498+00'
FROM clients
WHERE external_client_id = 'cfe3fcd1-7520-45a5-9a50-d870d47e3b60';

INSERT INTO roles (user_id, role)
SELECT user_id, 'CLIENT'
FROM users
WHERE username = 'efe';

-- A second CLIENT login: username manan, password manan, with its own client and one account.
--   client = 285a0443-7cad-4945-adeb-8e2a4eefdb60, user = ...db61, account = ...db62

INSERT INTO clients (
    external_client_id,
    first_name,
    last_name,
    email,
    birth_date
)
VALUES (
    '285a0443-7cad-4945-adeb-8e2a4eefdb60',
    'Manan',
    'Test',
    'manan@gmail.com',
    DATE '1990-01-01'
);

INSERT INTO accounts (
    external_account_id,
    balance
)
VALUES (
    '285a0443-7cad-4945-adeb-8e2a4eefdb62',
    1000000.0000
);

INSERT INTO client_accounts (client_id, account_id)
SELECT clients.client_id, accounts.account_id
FROM clients, accounts
WHERE clients.external_client_id = '285a0443-7cad-4945-adeb-8e2a4eefdb60'
  AND accounts.external_account_id = '285a0443-7cad-4945-adeb-8e2a4eefdb62';

-- bcrypt of "manan", 10 rounds like auth-service
INSERT INTO users (
    external_user_id,
    username,
    password_hash,
    client_id
)
SELECT
    '285a0443-7cad-4945-adeb-8e2a4eefdb61',
    'manan',
    '$2b$10$LOL2EwlvsTdqlYmDhCNE3eh5IhoyPP/uXJ5MW1v9YYpGXJBmIVJxG',
    client_id
FROM clients
WHERE external_client_id = '285a0443-7cad-4945-adeb-8e2a4eefdb60';

INSERT INTO roles (user_id, role)
SELECT user_id, 'CLIENT'
FROM users
WHERE username = 'manan';
