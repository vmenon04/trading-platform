import os

# Runs before any test imports api/database_connection. Setting these first also stops database_connection.py's
# load_dotenv() from pointing tests at a real database from .env (it never overrides).
os.environ["ANALYTICS_DB_URL"] = "postgresql://test:test@localhost:1/fake"
os.environ["MISSION_DB_URL"] = "postgresql://test:test@localhost:1/fake"
# api.py checks login tokens with this; the tests sign their own tokens with it (see test_api.py)
os.environ["JWT_SHARED_SECRET"] = "test-secret-that-is-at-least-32-characters-long"
