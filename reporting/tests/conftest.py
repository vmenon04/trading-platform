import os

# Runs before any test imports api/database_connection. Setting these first also stops database_connection.py's
# load_dotenv() from pointing tests at a real database from .env (it never overrides).
os.environ["ANALYTICS_DB_URL"] = "postgresql://test:test@localhost:1/fake"
os.environ["MISSION_DB_URL"] = "postgresql://test:test@localhost:1/fake"
# signs the test login tokens (see test_auth.py); long enough for HS256
os.environ["JWT_SHARED_SECRET"] = "test-secret-at-least-32-characters-long"
