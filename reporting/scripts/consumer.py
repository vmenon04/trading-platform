import json
import os
from datetime import datetime, timezone
from decimal import Decimal

from confluent_kafka import Consumer, KafkaError, TIMESTAMP_NOT_AVAILABLE
from sqlalchemy import text

from database_connection import REPORTING_DIR, get_analytics_engine

GROUP_ID = "analytics-service"

# Same strings as KafkaTopics.java
TRADE_RECORDED = "trade.recorded"
TRADE_ACCEPTED = "trade.accepted"
TRADE_FINISHED = "trade.finished"

UPSERT_RECORDED = """
    INSERT INTO trades (trade_id, account_id, instrument_id, trade_type, quantity,
                        price, notional, trade_time, status, status_time)
    VALUES (:trade_id, :account_id, :instrument_id, :trade_type, :quantity,
            NULL, NULL, :at, 'PENDING', :at)
    ON CONFLICT (trade_id) DO UPDATE
        SET trade_time = LEAST(trades.trade_time, EXCLUDED.trade_time)
"""

# trade.accepted and trade.finished are separate topics, so a trade's FULFILLED message can be
# processed before its ACCEPTED one. Only moving on from PENDING keeps a late ACCEPTED from undoing it.
UPSERT_ACCEPTED = """
    INSERT INTO trades (trade_id, account_id, instrument_id, trade_type, quantity,
                        price, notional, trade_time, status, status_time)
    VALUES (:trade_id, :account_id, :instrument_id, :trade_type, :quantity,
            :price, :notional, :at, 'ACCEPTED', :at)
    ON CONFLICT (trade_id) DO UPDATE
        SET status      = EXCLUDED.status,
            price       = EXCLUDED.price,
            notional    = EXCLUDED.notional,
            status_time = EXCLUDED.status_time
        WHERE trades.status = 'PENDING'
"""

UPSERT_FINISHED = """
    INSERT INTO trades (trade_id, account_id, instrument_id, trade_type, quantity,
                        price, notional, trade_time, status, status_time, reject_reason)
    VALUES (:trade_id, :account_id, :instrument_id, :trade_type, :quantity,
            :price, :notional, :at, :status, :at, :reject_reason)
    ON CONFLICT (trade_id) DO UPDATE
        SET status        = EXCLUDED.status,
            price         = EXCLUDED.price,
            notional      = EXCLUDED.notional,
            status_time   = EXCLUDED.status_time,
            reject_reason = EXCLUDED.reject_reason
        WHERE trades.status IN ('PENDING', 'ACCEPTED')
"""


INSERT_STATUS = """
    INSERT INTO trade_status_history (trade_id, status, status_time)
    VALUES (:trade_id, :status, :at)
    ON CONFLICT (trade_id, status) DO NOTHING
"""


def build_consumer():
    consumer = Consumer({
        "bootstrap.servers": os.environ["KAFKA_BOOTSTRAP_SERVERS"],
        "group.id": GROUP_ID,
        "auto.offset.reset": "earliest",
        "enable.auto.commit": False
    })
    consumer.subscribe([TRADE_RECORDED, TRADE_ACCEPTED, TRADE_FINISHED])
    return consumer


def event_time(msg):
    timestamp_type, timestamp = msg.timestamp()
    if timestamp_type == TIMESTAMP_NOT_AVAILABLE:
        return datetime.now(timezone.utc)
    return datetime.fromtimestamp(timestamp / 1000, tz=timezone.utc)


# Java sends camelCase field names (tradeId, accountId, side, ...)
def handle_recorded(conn, event, at):
    conn.execute(text(UPSERT_RECORDED), {
        "trade_id": event["tradeId"],
        "account_id": event["accountId"],
        "instrument_id": event["instrumentId"],
        "trade_type": event["side"],
        "quantity": event["quantity"],
        "at": at
    })
    conn.execute(text(INSERT_STATUS), {
        "trade_id": event["tradeId"],
        "status": "PENDING",
        "at": at
    })


# Only fulfilled trades are published to trade.accepted, so the price is always there
def handle_accepted(conn, event, at):
    conn.execute(text(UPSERT_ACCEPTED), {
        "trade_id": event["tradeId"],
        "account_id": event["accountId"],
        "instrument_id": event["instrumentId"],
        "trade_type": event["side"],
        "quantity": event["quantity"],
        "price": event["price"],
        "notional": event["price"] * event["quantity"],
        "at": at
    })
    conn.execute(text(INSERT_STATUS), {
        "trade_id": event["tradeId"],
        "status": "ACCEPTED",
        "at": at
    })


def handle_finished(conn, event, at):
    price = event["price"]
    if price is None:
        status, notional, reject_reason = "REJECTED", None, event.get("reason")
    else:
        status, notional, reject_reason = "FULFILLED", price * event["quantity"], None
    conn.execute(text(UPSERT_FINISHED), {
        "trade_id": event["tradeId"],
        "account_id": event["accountId"],
        "instrument_id": event["instrumentId"],
        "trade_type": event["side"],
        "quantity": event["quantity"],
        "price": price,
        "notional": notional,
        "status": status,
        "reject_reason": reject_reason,
        "at": at
    })
    conn.execute(text(INSERT_STATUS), {
        "trade_id": event["tradeId"],
        "status": status,
        "at": at
    })


HANDLERS = {
    TRADE_RECORDED: handle_recorded,
    TRADE_ACCEPTED: handle_accepted,
    TRADE_FINISHED: handle_finished
}


def process(msg, engine):
    try:
        # parse_float=Decimal keeps prices exact instead of turning them into floats
        event = json.loads(msg.value(), parse_float=Decimal)
        handler = HANDLERS[msg.topic()]
    except (json.JSONDecodeError, KeyError, TypeError) as e:
        print(f"Skipping unreadable message on {msg.topic()} at offset {msg.offset()}: {e}")
        return

    # commits both writes together when the block ends, or neither if something fails
    with engine.begin() as conn:
        handler(conn, event, event_time(msg))
    print(f"Processed {msg.topic()} for trade {event.get('tradeId')}")


def main():
    engine = get_analytics_engine()
    with engine.begin() as conn:
        conn.exec_driver_sql((REPORTING_DIR / "analytics_schema.sql").read_text())

    consumer = build_consumer()
    print(f"Listening to {TRADE_RECORDED}, {TRADE_ACCEPTED} and {TRADE_FINISHED} as {GROUP_ID}. Ctrl+C to stop.")
    try:
        while True:
            msg = consumer.poll(1.0)
            if msg is None:
                continue
            if msg.error():
                if msg.error().code() != KafkaError._PARTITION_EOF:
                    print(f"Consumer error: {msg.error()}")
                continue
            process(msg, engine)
            # only tell Kafka we're done once the database write has committed
            consumer.commit(message=msg, asynchronous=False)
    except KeyboardInterrupt:
        pass
    finally:
        consumer.close()


if __name__ == "__main__":
    main()
