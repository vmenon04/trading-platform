"""Tests for scripts/transform.py.   Run:  python -m pytest -v   (from the reporting folder)

There's no database in tests. The `db` fixture swaps pd.read_sql for a fake that
returns whatever DataFrame we put in `db.result`, and remembers the sql + params it got.
"""
import re
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


# --- client_portfolios --------------------------------------------------------

def test_portfolio_filters_are_parameters_not_sql_text(db):
    tf.client_portfolios(None, {"client_id": 7, "min_value": 1_000_000}, page=3, page_size=10)
    assert db.params == {"client_id": 7, "client_ids": None, "min_value": 1_000_000, "max_value": None,
                         "quartile": None, "page_size": 10, "offset": 20}
    assert "1000000" not in db.sql


def test_no_picked_clients_means_every_client(db):
    # an empty list would match nobody in SQL, so it is sent as NULL ("no filter")
    tf.portfolio_distribution(None, {"client_ids": []})
    assert db.params["client_ids"] is None


# --- several clients ----------------------------------------------------------

def test_portfolio_range_filters_trades_by_what_the_client_holds():
    sql, params = tf.build_where({"min_portfolio": 250_000})
    assert "portfolio_value >= %(min_portfolio)s" in sql
    assert params == {"min_portfolio": 250_000}


def test_every_picked_client_gets_every_period(db):
    # client 3 traded in January only, client 9 never: both still get a row for January and February
    db.result = periods("2026-01-01", client_id=[3], trade_count=[2], notional=[500.0])
    out = tf.volume_by_client(None, {**JAN_TO_FEB, "client_ids": [3, 9]}, "month")
    assert out["client_id"].tolist() == [3, 3, 9, 9]
    assert out["notional"].tolist() == [500.0, 0, 0, 0]


def test_quartiles_are_ranked_before_filtering(db):
    # If the WHERE ran first, NTILE would rank only the clients left (a $1M filter would still have a "bottom 25%").
    tf.client_portfolios(None, {"min_value": 1_000_000})
    assert db.sql.index("NTILE(4)") < db.sql.index("portfolio_value >= %(min_value)s")


def test_portfolio_summary_comes_from_the_first_row(db):
    db.result = pd.DataFrame({"client_id": [1, 2], "portfolio_value": [5.0, 3.0], "matching": [9, 9],
                              "matching_value": [40.0, 40.0]})
    summary, rows = tf.client_portfolios(None, {})
    assert summary == {"total": 9, "total_value": 40.0}
    assert list(rows.columns) == ["client_id", "portfolio_value"]


def test_no_matching_portfolios_gives_zero_summary(db):
    summary, rows = tf.client_portfolios(None, {"min_value": 1e15})
    assert summary == {"total": 0, "total_value": 0.0}


# --- queries.sql ----------------------------------------------------------------

def test_every_query_in_transform_exists_in_queries_sql():
    source = (tf.REPORTING_DIR / "scripts" / "transform.py").read_text()
    used = set(re.findall(r'query\("(\w+)"', source))
    assert used <= set(tf.QUERIES), f"missing from queries.sql: {used - set(tf.QUERIES)}"


def test_comments_are_dropped_so_their_percent_signs_cant_reach_the_driver():
    for name, sql in tf.QUERIES.items():
        assert "--" not in sql, name
