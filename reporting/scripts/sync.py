"""Copies the trading database into the analytics database.

    python reporting/scripts/sync.py          copy only what changed since the last run
    python reporting/scripts/sync.py --full   copy everything again (e.g. after reloading the trading database)

How "only what changed" works
-----------------------------
Every time a trade's status changes, the trading database adds a row to
account_trade_status with the time it happened. Each sync remembers the newest of
those times it has copied (the "watermark") in the sync_runs table. Next time, it
only copies trades with a status change after that watermark.

We go back 5 extra minutes (OVERLAP) in case a change was being saved at the exact
moment the last sync ran. Copying a trade twice is harmless, because its old rows
are deleted before the new ones are inserted.
"""
import sys
from datetime import datetime, timedelta, timezone

import pandas as pd
from sqlalchemy import text

from config import REPORTING_DIR, get_analytics_engine, get_source_engine

OVERLAP = timedelta(minutes=5)
BEGINNING_OF_TIME = datetime(1970, 1, 1, tzinfo=timezone.utc)

# Small tables that are copied in full every run: {analytics table name: query on the trading database}
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

# Trades with a status change after %(since)s, reshaped to fit the analytics `trades` table:
#   trade_time  = when the trade was first placed (its earliest status)
#   status      = its latest status
#   status_time = when that latest status happened
CHANGED_TRADES = """
    SELECT t.trade_id,
           t.account_id,
           t.instrument_id,
           t.trade_side AS trade_type,
           t.quantity,
           p.price_per_unit AS price,
           p.total_price AS notional,
           (SELECT MIN(s.trade_time) FROM account_trade_status s WHERE s.trade_id = t.trade_id) AS trade_time,
           (SELECT s.status FROM account_trade_status s WHERE s.trade_id = t.trade_id
            ORDER BY s.trade_time DESC LIMIT 1) AS status,
           (SELECT MAX(s.trade_time) FROM account_trade_status s WHERE s.trade_id = t.trade_id) AS status_time
    FROM account_trades t
    LEFT JOIN account_trade_price p ON p.trade_id = t.trade_id
    WHERE t.trade_id IN (SELECT trade_id FROM account_trade_status WHERE trade_time > %(since)s)
"""

# Every status step of those same trades.
CHANGED_STATUS_HISTORY = """
    SELECT trade_id, status, trade_time AS status_time
    FROM account_trade_status
    WHERE trade_id IN (SELECT trade_id FROM account_trade_status WHERE trade_time > %(since)s)
"""


def copy_reference_tables(source, analytics):
    """Empty each reference table in analytics and fill it again from the trading database."""
    for table, query in REFERENCE_TABLES.items():
        rows = pd.read_sql(query, source)
        analytics.execute(text(f"DELETE FROM {table}"))
        rows.to_sql(table, analytics, if_exists="append", index=False)
        print(f"  {table:<22} {len(rows):>7,} rows")


def replace_trade_rows(analytics, table, rows):
    """Delete these trades' old rows from `table`, then insert the new ones."""
    if rows.empty:
        return
    trade_ids = rows["trade_id"].unique().tolist()
    analytics.execute(text(f"DELETE FROM {table} WHERE trade_id = ANY(:trade_ids)"), {"trade_ids": trade_ids})
    rows.to_sql(table, analytics, if_exists="append", index=False)


def main():
    full = "--full" in sys.argv
    started_at = datetime.now(timezone.utc)

    # engine.begin() opens a transaction: if anything fails, every change is undone,
    # so the analytics database is never left half-copied.
    with get_source_engine().connect() as source, get_analytics_engine().begin() as analytics:
        # Create the analytics tables if this is the first run.
        schema = (REPORTING_DIR / "analytics_schema.sql").read_text()
        analytics.exec_driver_sql(schema)

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

        # The new watermark is the newest status change we've now copied.
        new_watermark = last_watermark
        if not trades.empty:
            newest_change = trades["status_time"].max()
            if new_watermark is None or newest_change > new_watermark:
                new_watermark = newest_change

        analytics.execute(
            text("""
                INSERT INTO sync_runs (started_at, finished_at, trades_copied, watermark)
                VALUES (:started_at, clock_timestamp(), :trades_copied, :watermark)
            """),
            {"started_at": started_at, "trades_copied": len(trades), "watermark": new_watermark},
        )

    if new_watermark is None:
        print("Done. No trades yet.")
    else:
        print(f"Done. Analytics data is current to {new_watermark:%Y-%m-%d %H:%M:%S}")


if __name__ == "__main__":
    main()
