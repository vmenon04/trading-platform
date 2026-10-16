import os

# Runs before any test imports api/database_connection. Setting these first also stops database_connection.py's
# load_dotenv() from pointing tests at a real database from .env (it never overrides).
os.environ["ANALYTICS_DB_URL"] = "postgresql://test:test@localhost:1/fake"
os.environ["MISSION_DB_URL"] = "postgresql://test:test@localhost:1/fake"
