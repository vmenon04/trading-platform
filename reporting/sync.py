"""Copies the trading database into the analytics database.

    python reporting/sync.py          copy what changed since the last run
    python reporting/sync.py --full   copy everything again (after reloading the trading database)
"""
import argparse
from datetime import datetime, timedelta, timezone

import pandas as pd
from sqlalchemy import text

from config import PACKAGE_DIR, get_analytics_engine, get_source_engine

OVERLAP = timedelta(minutes=5)
BEGINNING_OF_TIME = datetime(1970, 1, 1, tzinfo=timezone.utc)

REFERENCE_TABLES = {
    "clients": """
        SELECT c.client_id,
               c.first_name,
               c.last_name,
               c.email,
               (SELECT u.username FROM users u WHERE u.client_id = c.client_id ORDER BY u.user_id LIMIT 1) AS username
        FROM clients c
    """,
    "accounts": "SELECT account_id, balance FROM accounts",
    "client_accounts": "SELECT client_id, account_id FROM client_accounts",
    "instruments": "SELECT instrument_id, name, ticker, asset_class FROM instruments",
    "account_holdings": "SELECT account_id, instrument_id, as_of_date, quantity, status FROM account_holdings",
}

CHANGED_TRADES = """
    SELECT t.trade_id,
           t.account_id,
           t.instrument_id,
           t.trade_side AS trade_type,
           t.quantity,
           p.total_price / t.quantity AS price,
           p.total_price AS notional,
           (SELECT MIN(s.trade_time) FROM account_trade_status s WHERE s.trade_id = t.trade_id) AS trade_time,
           (SELECT s.status FROM account_trade_status s WHERE s.trade_id = t.trade_id
            ORDER BY s.trade_time DESC LIMIT 1) AS status,
           (SELECT MAX(s.trade_time) FROM account_trade_status s WHERE s.trade_id = t.trade_id) AS status_time
    FROM account_trades t
    LEFT JOIN trade_total_price p ON p.trade_id = t.trade_id
    WHERE t.trade_id IN (SELECT trade_id FROM account_trade_status WHERE trade_time > %(since)s)
"""

CHANGED_STATUS_HISTORY = """
    SELECT trade_id, status, trade_time AS status_time
    FROM account_trade_status
    WHERE trade_id IN (SELECT trade_id FROM account_trade_status WHERE trade_time > %(since)s)
"""


def copy_reference_tables(source, analytics):
    for table, query in REFERENCE_TABLES.items():
        rows = pd.read_sql(query, source)
        analytics.execute(text(f"DELETE FROM {table}"))
        rows.to_sql(table, analytics, if_exists="append", index=False, method="multi", chunksize=1000)
        print(f"  {table:<22} {len(rows):>7,} rows")


def replace_trade_rows(analytics, table, rows):
    if rows.empty:
        return
    trade_ids = rows["trade_id"].unique().tolist()
    analytics.execute(text(f"DELETE FROM {table} WHERE trade_id = ANY(:trade_ids)"), {"trade_ids": trade_ids})
    rows.to_sql(table, analytics, if_exists="append", index=False, method="multi", chunksize=1000)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--full", action="store_true")
    full = parser.parse_args().full

    started_at = datetime.now(timezone.utc)
    source = get_source_engine().connect().execution_options(isolation_level="REPEATABLE READ")

    with source, get_analytics_engine().begin() as analytics:
        analytics.exec_driver_sql((PACKAGE_DIR / "analytics_schema.sql").read_text())

        last_watermark = analytics.execute(text("SELECT MAX(watermark) FROM sync_runs")).scalar()
        if full or last_watermark is None:
            since = BEGINNING_OF_TIME
        else:
            since = last_watermark - OVERLAP
        print(f"Copying changes since {since:%Y-%m-%d %H:%M:%S}")

        copy_reference_tables(source, analytics)

        trades = pd.read_sql(CHANGED_TRADES, source, params={"since": since})
        history = pd.read_sql(CHANGED_STATUS_HISTORY, source, params={"since": since})
        replace_trade_rows(analytics, "trades", trades)
        replace_trade_rows(analytics, "trade_status_history", history)
        print(f"  {'trades':<22} {len(trades):>7,} rows changed")

        watermark = last_watermark
        if not trades.empty and (watermark is None or trades["status_time"].max() > watermark):
            watermark = trades["status_time"].max()

        analytics.execute(
            text("""
                INSERT INTO sync_runs (started_at, finished_at, trades_copied, watermark)
                VALUES (:started_at, clock_timestamp(), :trades_copied, :watermark)
            """),
            {"started_at": started_at, "trades_copied": len(trades), "watermark": watermark},
        )

    print(f"Done. Analytics data is current to {watermark:%Y-%m-%d %H:%M:%S}" if watermark else "Done. No trades yet.")


if __name__ == "__main__":
    main()
