"""Tests for scripts/charts.py.   Run:  python -m pytest -v   (from the reporting folder)

`tmp_path` is a built-in pytest fixture: an empty temporary folder, made fresh for
each test, so the charts can be saved somewhere and checked.
"""
import pandas as pd

import charts


def test_shorten_long_names():
    assert charts.shorten(["abcd", "abcde", "abc def"], width=4) == ["abcd", "abcd…", "abc…"]


def test_period_labels():
    df = pd.DataFrame({"period": pd.to_datetime(["2026-01-01", "2026-12-01"])})
    assert charts.period_labels(df).tolist() == ["Jan 2026", "Dec 2026"]


def test_every_chart_saves_a_png(tmp_path):
    periods = pd.to_datetime(["2026-01-01", "2026-02-01"])
    volume = pd.DataFrame({"period": periods, "notional": [100.0, 200.0], "trade_count": [1, 2]})
    buy_sell = pd.DataFrame({"period": periods, "BUY": [60.0, 0.0], "SELL": [40.0, 0.0]})
    clients = pd.DataFrame({"client_name": ["Ann Lee", "A Very Long Client Name Indeed"], "notional": [9.0, 5.0]})
    classes = pd.DataFrame({"asset_class": ["STOCK", "ETF"], "notional": [9.0, 5.0], "trade_count": [3, 1]})

    charts.volume_by_period(volume, tmp_path, "volume")
    charts.volume_by_trade_type(buy_sell, tmp_path, "buy_sell")
    charts.top_customers(clients, tmp_path, "clients")
    charts.volume_by_asset_class(classes, tmp_path, "classes")

    for name in ["volume", "buy_sell", "clients", "classes"]:
        assert (tmp_path / f"{name}.png").exists()
