"""Tests for scripts/api.py.   Run:  python -m pytest -v   (from the reporting folder)

TestClient sends pretend web requests to the app, without starting a real server.
There's no database in tests, so any transform function an endpoint needs is
swapped for a fake with monkeypatch.
"""
import pandas as pd
import pytest
from fastapi.testclient import TestClient

import api

client = TestClient(api.app)

DATES = "start=2026-01-01&end=2026-02-01"   # given in full so the API doesn't ask the database for defaults


# --- bad input is rejected with 422 before any of our code runs -------------

@pytest.mark.parametrize("url", [
    "/api/admin/orders?page=0",
    "/api/admin/orders?sort=bad",
    "/api/admin/orders?sort=t.trade_id;DROP TABLE trades",
    "/api/admin/orders?status=NOPE",
    "/api/admin/orders?status=SUBMITTED",       # the Java TradeStatus enum calls it PENDING
    "/api/admin/orders?side=sell",              # must be upper case
    "/api/admin/orders?order=up",
    "/api/admin/orders?trade_id=abc",
    f"/api/analyst/volume?{DATES}&period=hour",
    f"/api/analyst/volume?{DATES}&client_id=abc",
    "/api/analyst/volume?start=not-a-date&end=2026-02-01",
    "/api/clients",                             # q is required
    "/api/analyst/portfolios?quartile=5",       # quartiles are 1-4
    "/api/analyst/portfolios?min_value=-1",
])
def test_bad_input_returns_422(url):
    response = client.get(url)
    assert response.status_code == 422


# --- good input reaches transform.py correctly -------------------------------

def test_limit_is_capped_at_max_rows(monkeypatch):
    received = {}

    def fake_top_clients(engine, filters, top_n):
        received["top_n"] = top_n
        return pd.DataFrame()

    monkeypatch.setattr(api.tf, "top_clients", fake_top_clients)

    client.get(f"/api/analyst/top-clients?{DATES}&limit=999")
    assert received["top_n"] == api.MAX_ROWS


def test_portfolios_pass_filters_and_work_out_pages(monkeypatch):
    received = {}

    def fake_client_portfolios(engine, filters, page, page_size):
        received.update(filters=filters, page=page, page_size=page_size)
        rows = pd.DataFrame({"client_id": [7], "portfolio_value": [1500.0], "quartile": [4]})
        return {"total": 101, "total_value": 9e6}, rows

    monkeypatch.setattr(api.tf, "client_portfolios", fake_client_portfolios)

    body = client.get("/api/analyst/portfolios?min_value=1000000&quartile=4&page=2&page_size=999").json()
    assert received["filters"] == {"client_id": None, "min_value": 1000000, "max_value": None, "quartile": 4}
    assert received["page_size"] == api.MAX_ROWS
    assert body["pages"] == 3                     # 50 + 50 + 1
    assert body["total_value"] == 9e6
    assert body["rows"] == [{"client_id": 7, "portfolio_value": 1500.0, "quartile": 4}]


def test_search_text_is_passed_as_plain_text(monkeypatch):
    received = {}

    def fake_search_clients(engine, search, limit):
        received["search"] = search
        return pd.DataFrame()

    monkeypatch.setattr(api.tf, "search_clients", fake_search_clients)

    response = client.get("/api/clients?q=' OR 1=1 --")
    assert response.status_code == 200
    assert received["search"] == "' OR 1=1 --"


def test_orders_works_out_the_number_of_pages(monkeypatch):
    def fake_order_history(engine, filters, sort, descending, page, page_size):
        return 101, pd.DataFrame()   # 101 matching orders

    monkeypatch.setattr(api.tf, "order_history", fake_order_history)

    body = client.get("/api/admin/orders?page_size=50").json()
    assert body["pages"] == 3        # 50 + 50 + 1


def test_unknown_order_history_returns_404(monkeypatch):
    monkeypatch.setattr(api.tf, "order_status_history", lambda engine, trade_id: pd.DataFrame())

    response = client.get("/api/admin/orders/999/history")
    assert response.status_code == 404
