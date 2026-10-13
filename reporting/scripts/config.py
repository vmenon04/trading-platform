"""Settings shared by every script: folder paths and the two database connections."""
import os
from pathlib import Path

from dotenv import load_dotenv
from sqlalchemy import create_engine

# This file is reporting/scripts/config.py, so two .parent steps up is the reporting/ folder.
REPORTING_DIR = Path(__file__).resolve().parent.parent
REPORTS_DIR = REPORTING_DIR / "reports"

# Read MISSION_DB_URL and ANALYTICS_DB_URL from the .env file in the repo root.
# Variables that are already set are left alone, so tests can point at a fake database.
load_dotenv(REPORTING_DIR.parent / ".env")


def get_source_engine():
    """The main trading database. We only ever read from it.

    - default_transaction_read_only: Postgres refuses any write, so a bug here can't damage it.
    - statement_timeout: give up on any query that runs longer than 30 seconds.
    - REPEATABLE READ: every query in one connection sees the data as it was when the
      connection started, so trades added halfway through a sync can't mix in.
    """
    return create_engine(
        os.environ["MISSION_DB_URL"],
        isolation_level="REPEATABLE READ",
        connect_args={"options": "-c default_transaction_read_only=on -c statement_timeout=30000"},
    )


def get_analytics_engine():
    """Our own copy of the data, which the reports and the dashboard read from."""
    return create_engine(os.environ["ANALYTICS_DB_URL"], pool_pre_ping=True)
