"""Run with: python reporting/api.py, then open http://localhost:8050/analyst or /admin"""
import json
import math
from datetime import date, datetime, timedelta, timezone
from typing import Literal

import uvicorn
from fastapi import APIRouter, Depends, FastAPI, HTTPException, Query
from fastapi.staticfiles import StaticFiles
from starlette.exceptions import HTTPException as StarletteHTTPException

import transform as tf
from config import PACKAGE_DIR, get_analytics_engine

MAX_ROWS = 50
SYNC_EVERY = timedelta(hours=1)
WEB_BUILD = PACKAGE_DIR / "web" / "dist" / "web" / "browser"

Period = Literal["day", "week", "month", "quarter", "year"]
Status = Literal["PENDING", "ACCEPTED", "FULFILLED", "REJECTED"]
Side = Literal["BUY", "SELL"]
SortKey = Literal["id", "time", "status", "ticker", "quantity", "price", "value"]

engine = get_analytics_engine()
app = FastAPI(title="F-Trade reporting")

shared = APIRouter(prefix="/api")
analyst = APIRouter(prefix="/api/analyst")
admin = APIRouter(prefix="/api/admin")


def records(df):
    return json.loads(df.to_json(orient="records", date_format="iso"))


def cap(limit):
    return max(1, min(limit, MAX_ROWS))


def date_range(start: date | None = None, end: date | None = None):
    if start and end:
        return start, end
    default_start, default_end = tf.reporting_window(engine)
    return start or default_start, end or default_end


def chart_filters(client_id: int | None = None, asset_class: str | None = None, instrument_id: int | None = None):
    return {"client_id": client_id, "asset_class": asset_class, "instrument_id": instrument_id}


@shared.get("/status")
def status():
    sync = tf.last_sync(engine)
    start, end = date_range()
    stale = sync is None or datetime.now(timezone.utc) - sync["finished_at"] > 2 * SYNC_EVERY
    return {
        "last_sync": sync,
        "stale": stale,
        "default_start": start,
        "default_end": end,
        "max_rows": MAX_ROWS,
    }


@shared.get("/asset-classes")
def asset_classes():
    return tf.asset_classes(engine)


@shared.get("/clients")
def search_clients(q: str, limit: int = 20):
    return records(tf.search_clients(engine, q.strip(), cap(limit)))


@shared.get("/instruments")
def search_instruments(q: str, asset_class: str | None = None, limit: int = 20):
    return records(tf.search_instruments(engine, q.strip(), cap(limit), asset_class))


@analyst.get("/volume")
def volume(dates=Depends(date_range), filters=Depends(chart_filters), period: Period = "month"):
    return records(tf.trade_volume_by_period(engine, *dates, period, **filters))


@analyst.get("/buy-sell")
def buy_sell(dates=Depends(date_range), filters=Depends(chart_filters), period: Period = "month"):
    return records(tf.volume_by_trade_type(engine, *dates, period, **filters))


@analyst.get("/status-mix")
def status_mix(dates=Depends(date_range), filters=Depends(chart_filters), period: Period = "month"):
    return records(tf.status_by_period(engine, *dates, period, **filters))


@analyst.get("/price")
def price(dates=Depends(date_range), filters=Depends(chart_filters), period: Period = "month"):
    return records(tf.price_by_period(engine, *dates, period, **filters))


@analyst.get("/by-asset-class")
def by_asset_class(dates=Depends(date_range), filters=Depends(chart_filters)):
    return records(tf.volume_by_asset_class(engine, *dates, **filters))


@analyst.get("/top-clients")
def top_clients(dates=Depends(date_range), filters=Depends(chart_filters), limit: int = 10):
    return records(tf.top_clients(engine, *dates, cap(limit), **filters))


@analyst.get("/top-instruments")
def top_instruments(dates=Depends(date_range), filters=Depends(chart_filters), limit: int = 10):
    return records(tf.top_instruments(engine, *dates, cap(limit), **filters))


@analyst.get("/trades")
def recent_trades(filters=Depends(chart_filters), limit: int = MAX_ROWS):
    return records(tf.recent_trades(engine, cap(limit), **filters))


@admin.get("/orders")
def orders(
    filters=Depends(chart_filters),
    trade_id: int | None = None,
    status: list[Status] = Query(default=[]),
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
    page_size = cap(page_size)
    total, df = tf.order_history(
        engine,
        sort=sort,
        descending=order == "desc",
        page=page,
        page_size=page_size,
        trade_id=trade_id,
        statuses=status,
        side=side,
        start=start,
        end=end,
        min_value=min_value,
        max_value=max_value,
        **filters,
    )
    return {
        "total": total,
        "page": page,
        "page_size": page_size,
        "pages": max(1, math.ceil(total / page_size)),
        "rows": records(df),
    }


@admin.get("/orders/{trade_id}/history")
def order_status_history(trade_id: int):
    df = tf.order_status_history(engine, trade_id)
    if df.empty:
        raise HTTPException(404, f"No order {trade_id}")
    return records(df)


class AngularApp(StaticFiles):
    async def get_response(self, path, scope):
        try:
            return await super().get_response(path, scope)
        except StarletteHTTPException as error:
            if error.status_code != 404 or scope["path"].startswith("/api/"):
                raise
            return await super().get_response("index.html", scope)


app.include_router(shared)
app.include_router(analyst)
app.include_router(admin)

if WEB_BUILD.exists():
    app.mount("/", AngularApp(directory=WEB_BUILD, html=True), name="web")


if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8050)
