"""Tests for scripts/consumer.py and the part of scripts/sync.py that works alongside it.

Run:  python -m pytest -v   (from the reporting folder)

There's no database or Kafka in tests. FakeConnection stands in for the database
connection and remembers the parameters of every SQL statement it was given.
"""
from datetime import datetime, timezone
from decimal import Decimal

import pandas as pd

import consumer
import sync

AT = datetime(2026, 10, 14, 12, 0, tzinfo=timezone.utc)
FINISHED = {"tradeId": 9001, "accountId": 1, "instrumentId": 2, "side": "BUY", "quantity": Decimal("10")}


class FakeConnection:
    def __init__(self):
        self.statements = []

    def execute(self, statement, params):
        self.statements.append(params)


def test_rejected_trade_keeps_its_reason():
    conn = FakeConnection()
    consumer.handle_finished(conn, {**FINISHED, "price": None, "reason": "Insufficient funds"}, AT)

    trade = conn.statements[0]
    assert trade["status"] == "REJECTED"
    assert trade["reject_reason"] == "Insufficient funds"
    assert trade["notional"] is None


def test_fulfilled_trade_has_no_reason():
    conn = FakeConnection()
    consumer.handle_finished(conn, {**FINISHED, "price": Decimal("150.30"), "reason": None}, AT)

    trade = conn.statements[0]
    assert trade["status"] == "FULFILLED"
    assert trade["reject_reason"] is None
    assert trade["notional"] == Decimal("1503.00")


def test_rejection_without_a_reason_field_still_works():
    # Older messages may not have "reason" at all.
    conn = FakeConnection()
    consumer.handle_finished(conn, {**FINISHED, "price": None}, AT)
    assert conn.statements[0]["reject_reason"] is None


def test_sync_keeps_the_reason_the_consumer_saved(monkeypatch):
    # The trading database has no reasons, so sync must carry over what's already in analytics.
    saved = pd.DataFrame({"trade_id": [1], "reject_reason": ["Market closed"]})
    monkeypatch.setattr(sync.pd, "read_sql", lambda sql, conn, params=None: saved)

    from_trading_db = pd.DataFrame({"trade_id": [1, 2], "status": ["REJECTED", "FULFILLED"]})
    out = sync.keep_reject_reasons(None, from_trading_db)

    assert out.loc[out.trade_id == 1, "reject_reason"].item() == "Market closed"
    assert pd.isna(out.loc[out.trade_id == 2, "reject_reason"].item())
