"""The web server for the dashboard.

Run:  python reporting/scripts/api.py
Then open http://localhost:8050/analyst or http://localhost:8050/admin

Every /api/... address below returns JSON for the Angular pages in reporting/web.

How FastAPI checks user input
-----------------------------
FastAPI reads each function's parameters from the URL. For example
    /api/analyst/volume?period=week&client_id=7
calls volume(period="week", client_id=7).

The type written next to each parameter is enforced before our code runs:
- `client_id: int` rejects "abc".
- `Period` (below) only allows the five words listed, so "hour" or "1; DROP TABLE" is rejected.
- `Query(ge=1)` means "greater than or equal to 1".
Anything that doesn't fit gets an automatic "422 Unprocessable Entity" error.
"""
import json
import math
from datetime import date, timedelta
from typing import Literal

import pandas as pd
import uvicorn
from fastapi import Depends, FastAPI, HTTPException, Query
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles

import transform as tf
from config import REPORTING_DIR, get_analytics_engine

MAX_ROWS = 50                      # the most rows any endpoint returns at once
SYNC_EVERY = timedelta(hours=1)    # how often sync.py is expected to run
WEB_BUILD = REPORTING_DIR / "web" / "dist" / "web" / "browser"   # made by `npm run build` in reporting/web

# The only values these parameters may have.
Period = Literal["day", "week", "month", "quarter", "year"]
Status = Literal["SUBMITTED", "ACCEPTED", "FULFILLED", "REJECTED"]
Side = Literal["BUY", "SELL"]
SortKey = Literal["id", "time", "status", "ticker", "quantity", "price", "value"]

engine = get_analytics_engine()
app = FastAPI(title="F-Trade reporting")


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def to_json(df):
    """A DataFrame as a list of row dictionaries, ready to send as JSON.

    pandas does the conversion so that dates become text like "2026-01-01T00:00:00.000"
    and NaN (no value) becomes null.
    """
    return json.loads(df.to_json(orient="records", date_format="iso"))


def cap(limit):
    """Keep a requested row count between 1 and MAX_ROWS."""
    return max(1, min(limit, MAX_ROWS))


def chart_filters(
    start: date | None = None,
    end: date | None = None,
    client_id: int | None = None,
    asset_class: str | None = None,
    instrument_id: int | None = None,
):
    """The filters every chart accepts, as one dictionary.

    Chart endpoints say `filters=Depends(chart_filters)`. FastAPI then reads these five
    values from the URL, calls this function, and passes the dictionary it returns.
    Missing dates default to the last 12 months of trades.
    """
    if start is None or end is None:
        default_start, default_end = tf.reporting_window(engine)
        if start is None:
            start = default_start
        if end is None:
            end = default_end

    return {
        "start": start,
        "end": end,
        "client_id": client_id,
        "asset_class": asset_class,
        "instrument_id": instrument_id,
    }


# ---------------------------------------------------------------------------
# Used by every page
# ---------------------------------------------------------------------------

@app.get("/api/status")
def status():
    """When the data was last synced, and the default date range for the pages."""
    sync = tf.last_sync(engine)
    if sync.empty:
        last_sync = None
        stale = True
    else:
        last_sync = to_json(sync)[0]
        finished_at = sync["finished_at"][0]
        stale = pd.Timestamp.now(tz="UTC") - finished_at > 2 * SYNC_EVERY

    default_start, default_end = tf.reporting_window(engine)
    return {
        "last_sync": last_sync,
        "stale": bool(stale),
        "default_start": default_start,
        "default_end": default_end,
        "max_rows": MAX_ROWS,
    }


@app.get("/api/asset-classes")
def asset_classes():
    return tf.asset_classes(engine)


@app.get("/api/clients")
def search_clients(q: str, limit: int = 20):
    return to_json(tf.search_clients(engine, q.strip(), cap(limit)))


@app.get("/api/instruments")
def search_instruments(q: str, asset_class: str | None = None, limit: int = 20):
    return to_json(tf.search_instruments(engine, q.strip(), cap(limit), asset_class))


# ---------------------------------------------------------------------------
# Analyst pages: charts
# ---------------------------------------------------------------------------

@app.get("/api/analyst/volume")
def volume(filters=Depends(chart_filters), period: Period = "month"):
    return to_json(tf.trade_volume_by_period(engine, filters, period))


@app.get("/api/analyst/buy-sell")
def buy_sell(filters=Depends(chart_filters), period: Period = "month"):
    return to_json(tf.volume_by_trade_type(engine, filters, period))


@app.get("/api/analyst/status-mix")
def status_mix(filters=Depends(chart_filters), period: Period = "month"):
    return to_json(tf.status_by_period(engine, filters, period))


@app.get("/api/analyst/price")
def price(filters=Depends(chart_filters), period: Period = "month"):
    return to_json(tf.price_by_period(engine, filters, period))


@app.get("/api/analyst/by-asset-class")
def by_asset_class(filters=Depends(chart_filters)):
    return to_json(tf.volume_by_asset_class(engine, filters))


@app.get("/api/analyst/top-clients")
def top_clients(filters=Depends(chart_filters), limit: int = 10):
    return to_json(tf.top_clients(engine, filters, cap(limit)))


@app.get("/api/analyst/top-instruments")
def top_instruments(filters=Depends(chart_filters), limit: int = 10):
    return to_json(tf.top_instruments(engine, filters, cap(limit)))


@app.get("/api/analyst/trades")
def recent_trades(
    client_id: int | None = None,
    asset_class: str | None = None,
    instrument_id: int | None = None,
    limit: int = MAX_ROWS,
):
    # No date range here: the latest trades, whenever they happened.
    filters = {"client_id": client_id, "asset_class": asset_class, "instrument_id": instrument_id}
    return to_json(tf.recent_trades(engine, filters, cap(limit)))


# ---------------------------------------------------------------------------
# Admin page: order history
# ---------------------------------------------------------------------------

@app.get("/api/admin/orders")
def orders(
    trade_id: int | None = None,
    client_id: int | None = None,
    asset_class: str | None = None,
    instrument_id: int | None = None,
    status: list[Status] = Query(default=[]),   # can be given more than once: ?status=SUBMITTED&status=REJECTED
    side: Side | None = None,
    start: date | None = None,
    end: date | None = None,
    min_value: float | None = None,
    max_value: float | None = None,
    sort: SortKey = "time",
    order: Literal["asc", "desc"] = "desc",
    page: int = Query(default=1, ge=1),
    page_size: int = MAX_ROWS,
):
    filters = {
        "trade_id": trade_id,
        "client_id": client_id,
        "asset_class": asset_class,
        "instrument_id": instrument_id,
        "statuses": status,
        "side": side,
        "start": start,
        "end": end,
        "min_value": min_value,
        "max_value": max_value,
    }
    page_size = cap(page_size)
    descending = order == "desc"
    total, df = tf.order_history(engine, filters, sort, descending, page, page_size)

    return {
        "total": total,
        "page": page,
        "page_size": page_size,
        "pages": max(1, math.ceil(total / page_size)),
        "rows": to_json(df),
    }


@app.get("/api/admin/orders/{trade_id}/history")
def order_status_history(trade_id: int):
    df = tf.order_status_history(engine, trade_id)
    if df.empty:
        raise HTTPException(404, f"No order {trade_id}")
    return to_json(df)


# ---------------------------------------------------------------------------
# The Angular web pages
#
# Angular is a "single page app": index.html is the only real page, and Angular
# itself decides what to show for /analyst, /analyst/clients or /admin. So each of
# those addresses just gets index.html, and every other file (the .js and .css
# files) is served straight from the build folder.
# ---------------------------------------------------------------------------

if WEB_BUILD.exists():
    @app.get("/analyst")
    @app.get("/analyst/{page}")
    @app.get("/admin")
    def web_page(page: str = ""):
        return FileResponse(WEB_BUILD / "index.html")

    app.mount("/", StaticFiles(directory=WEB_BUILD, html=True))


if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8050)
