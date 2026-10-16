-- Auth test seed data
--
-- This script inserts the minimal rows needed to support the provided users row:
--   user_id = 2
--   external_user_id = cfe3fcd1-7520-45a5-9a50-d870d47e3b61
--   username = efe
--   password_hash = $2b$10$kPf3i0mdvOh//y.0WzFL8uysl/eeeMSy7td7FJ6vBmbwrSrdcxAqq
--   linked client = 1 with one linked account = 1
--
-- It also creates the matching client, account, client_accounts, and roles rows so the auth service
-- and the Java authorization flow can load the user cleanly.
-- Run schema.sql first.

TRUNCATE TABLE
    client_accounts,
    roles,
    refresh_tokens,
    users,
    accounts,
    clients
RESTART IDENTITY CASCADE;

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
    100.0000
);

INSERT INTO client_accounts (client_id, account_id)
VALUES (1, 1);

INSERT INTO users (
    external_user_id,
    username,
    password_hash,
    client_id,
    created_at
)
OVERRIDING SYSTEM VALUE
VALUES (
    'cfe3fcd1-7520-45a5-9a50-d870d47e3b61',
    'efe',
    '$2b$10$kPf3i0mdvOh//y.0WzFL8uysl/eeeMSy7td7FJ6vBmbwrSrdcxAqq',
    1,
    TIMESTAMPTZ '2026-10-16 14:56:58.034498+00'
);

SELECT setval(pg_get_serial_sequence('users', 'user_id'), 2, true);

INSERT INTO roles (user_id, role)
VALUES (2, 'CLIENT');





