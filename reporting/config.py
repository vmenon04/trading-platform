import os
from pathlib import Path

from dotenv import load_dotenv
from sqlalchemy import create_engine

PACKAGE_DIR = Path(__file__).resolve().parent
REPO_ROOT = PACKAGE_DIR.parent
REPORTS_DIR = PACKAGE_DIR / "reports"

for env_file in (PACKAGE_DIR / ".env", REPO_ROOT / ".env", REPO_ROOT.parent / ".env"):
    if env_file.exists():
        load_dotenv(env_file)
        break


def get_source_engine():
    read_only = "-c default_transaction_read_only=on -c statement_timeout=30000"
    return create_engine(os.environ["MISSION_DB_URL"], pool_pre_ping=True, connect_args={"options": read_only})


def get_analytics_engine():
    return create_engine(os.environ["ANALYTICS_DB_URL"], pool_pre_ping=True)
