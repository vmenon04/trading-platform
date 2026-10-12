"""Database connection. Nothing else lives here."""
from __future__ import annotations

import os
from pathlib import Path

from dotenv import load_dotenv
from sqlalchemy import create_engine
from sqlalchemy.engine import Engine

PACKAGE_DIR = Path(__file__).resolve().parent      # reporting/
REPO_ROOT = PACKAGE_DIR.parents[0]
REPORTS_DIR = PACKAGE_DIR / "reports"              # where each run's pack lands

# .env may sit beside the package or one level up; try both.
for candidate in (PACKAGE_DIR / ".env", REPO_ROOT / ".env", REPO_ROOT.parent / ".env"):
    if candidate.exists():
        load_dotenv(candidate)
        break


def get_engine() -> Engine:
    """SQLAlchemy engine for the trading platform database."""
    return create_engine(os.environ["MISSION_DB_URL"], pool_pre_ping=True)