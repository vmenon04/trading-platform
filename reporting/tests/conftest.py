import os

# Runs before any test imports api/config. Setting these first also stops config.py's
# load_dotenv() from pointing tests at a real database from .env (it never overrides).
os.environ["ANALYTICS_DB_URL"] = "postgresql://test:test@localhost:1/fake"
os.environ["MISSION_DB_URL"] = "postgresql://test:test@localhost:1/fake"
