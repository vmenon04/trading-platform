"""Tests for scripts/transform.py.   Run:  python -m pytest -v   (from the reporting folder)

There's no database in tests. The `db` fixture swaps pd.read_sql for a fake that
returns whatever DataFrame we put in `db.result`, and remembers the sql + params it got.
"""
from datetime import date

import pandas as pd
import pytest

import transform as tf


class FakeDB:
    def __init__(self):
        self.result = pd.DataFrame()
        self.sql = None
        self.params = None

    def read_sql(self, sql, engine, params=None, parse_dates=None):
        self.sql = sql
        self.params = params
        return self.result.copy()


@pytest.fixture
def db(monkeypatch):
    fake = FakeDB()
    monkeypatch.setattr(tf.pd, "read_sql", fake.read_sql)
    return fake


def periods(*dates, **columns):
    """A DataFrame shaped like a query result: a 'period' column plus the given columns."""
    return pd.DataFrame({"period": pd.to_datetime(list(dates)), **columns})


JAN_TO_FEB = {"start": "2026-01-01", "end": "2026-03-01"}


# --- build_where: user input and SQL injection --------------------------------

def test_no_filters():
    assert tf.build_where({}) == ("WHERE TRUE", {})


def test_empty_filters_are_skipped():
    assert tf.build_where({"client_id": None, "statuses": []}) == ("WHERE TRUE", {})


def test_zero_is_a_real_filter_value():
    _, params = tf.build_where({"min_value": 0})
    assert params == {"min_value": 0}


def test_user_values_never_go_into_the_sql_text():
    evil = "BUY'; DROP TABLE trades; --"
    sql, params = tf.build_where({"side": evil})
    assert "DROP" not in sql
    assert params["side"] == evil


def test_unknown_sort_column_is_rejected(db):
    # `sort` is pasted into the SQL, so only the names in ORDER_SORTS are allowed.
    db.result = pd.DataFrame({"total": [0]})
    with pytest.raises(KeyError):
        tf.order_history(None, {}, sort="t.trade_id; DROP TABLE trades")


# --- fill_missing_periods -----------------------------------------------------

def test_missing_months_are_filled_with_zero():
    df = periods("2026-01-01", "2026-03-01", trade_count=[3, 5])
    out = tf.fill_missing_periods(df, "2026-01-01", "2026-04-01", "month")
    assert out["trade_count"].tolist() == [3, 0, 5]


def test_weeks_start_on_monday_like_postgres():
    out = tf.fill_missing_periods(periods(n=[]), "2026-10-08", "2026-10-20", "week")
    assert out["period"].iloc[0] == pd.Timestamp("2026-10-05")


# --- months with no trades ----------------------------------------------------

def test_month_with_no_orders_has_no_fill_rate(db):
    db.result = periods("2026-01-01", PENDING=[0], ACCEPTED=[1], FULFILLED=[2], REJECTED=[1], orders=[4])
    out = tf.status_by_period(None, JAN_TO_FEB, "month")
    assert out["fill_rate"].tolist()[0] == 50.0
    assert pd.isna(out["fill_rate"].tolist()[1])


def test_month_with_no_trades_has_no_price(db):
    db.result = periods("2026-01-01", avg_price=[10.0], low_price=[9.0], high_price=[11.0])
    out = tf.price_by_period(None, JAN_TO_FEB, "month")
    assert pd.isna(out["avg_price"].tolist()[1])


# --- reporting_window ---------------------------------------------------------

def test_empty_trades_table_raises(db):
    db.result = pd.DataFrame({"first_day": [None], "last_day": [None]})
    with pytest.raises(RuntimeError):
        tf.reporting_window(None)


def test_window_starts_on_the_1st_but_not_before_the_first_trade(db):
    db.result = pd.DataFrame({"first_day": [date(2020, 1, 1)], "last_day": [date(2026, 10, 7)]})
    assert tf.reporting_window(None, 12) == (date(2025, 10, 1), date(2026, 10, 8))

    db.result = pd.DataFrame({"first_day": [date(2026, 9, 15)], "last_day": [date(2026, 10, 7)]})
    assert tf.reporting_window(None, 12) == (date(2026, 9, 15), date(2026, 10, 8))
