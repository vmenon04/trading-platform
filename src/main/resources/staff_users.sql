-- Staff logins: one ANALYST and one ADMIN user
--
-- Registration (POST /auth/register) only ever creates CLIENT users, so staff users are inserted here instead.
-- They have no client_id: they don't trade, they only see the analyst or admin pages.
--
--   username  password      role
--   analyst   AnalystPass1  ANALYST
--   admin     AdminPass1    ADMIN
--
-- The hashes are bcrypt (cost 10), the same as auth-service makes. To use another password, hash it with
--   node -e "console.log(require('bcrypt').hashSync('the password', 10))"
-- from auth-service/ and replace the hash below.
--
-- Postgres runs this once, when its volume is new (see docker-compose.yml). It is safe to run again on an
-- existing database, e.g. pasted into Adminer: rows that are already there are left alone.

INSERT INTO users (external_user_id, username, password_hash)
VALUES
    ('5a7f0c1e-3b2d-4e8a-9f61-0d2c4b8a7e01', 'analyst', '$2b$10$TSpi9uxMQ3GpdaumBd8bv.7D3OLFtHSJy/2.m.CL2hRG80wPcpW52'),
    ('5a7f0c1e-3b2d-4e8a-9f61-0d2c4b8a7e02', 'admin', '$2b$10$lXgUC92ZrDIb/njkOzjWLuBrijF6Wd/ntPl15YCmgjozgjnXGD8wu')
ON CONFLICT (username) DO NOTHING;

INSERT INTO roles (user_id, role)
SELECT user_id, 'ANALYST' FROM users WHERE username = 'analyst'
UNION ALL
SELECT user_id, 'ADMIN' FROM users WHERE username = 'admin'
ON CONFLICT DO NOTHING;
