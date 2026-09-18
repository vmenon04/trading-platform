"""Orchestrator: build every DataFrame, draw every chart, save the pack.

    python reporting/main.py                  # last 12 months, monthly buckets
    python reporting/main.py --grain quarter  # quarterly buckets
    python reporting/main.py --months 3       # last 3 months only
    python reporting/main.py --all-time       # every trade on record
    python reporting/main.py --top 20         # top 20 clients instead of 10

Each run writes a dated folder under reporting/reports/ holding one CSV and one PNG per
report, plus a workbook with all four as sheets. Nothing is overwritten, so
last month's pack stays intact as the database grows.
"""
from __future__ import annotations

import argparse
from datetime import datetime
from pathlib import Path

import pandas as pd

import charts
import transform as tf
from config import REPORTS_DIR, get_engine


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Trading platform reporting pack")
    p.add_argument("--grain", default="month", choices=list(tf.VALID_GRAINS),
                   help="time bucket for the period charts (default: month)")
    p.add_argument("--months", type=int, default=12,
                   help="months of history to include (default: 12)")
    p.add_argument("--all-time", action="store_true",
                   help="ignore --months and use every trade on record")
    p.add_argument("--top", type=int, default=10,
                   help="how many clients in the top-clients report (default: 10)")
    p.add_argument("--out", type=Path, default=REPORTS_DIR,
                   help="where to write the pack (default: reporting/reports/)")
    return p.parse_args()


def guard(df: pd.DataFrame, label: str) -> pd.DataFrame:
    """Stop before writing a misleading chart."""
    if df.empty:
        raise SystemExit(f"[{label}] returned no rows — check the window and trade statuses.")
    for col in ("notional", "units", "BUY", "SELL"):
        if col in df.columns and (df[col] < 0).any():
            raise SystemExit(f"[{label}] has a negative {col}, which should be impossible.")
    return df


def main() -> None:
    args = parse_args()
    engine = get_engine()
    start, end = tf.reporting_window(engine, months_back=None if args.all_time else args.months)

    out_dir = args.out / f"{start:%Y%m%d}_{end:%Y%m%d}_{args.grain}"
    out_dir.mkdir(parents=True, exist_ok=True)

    print(f"Window     : {start} to {end} (end exclusive)")
    print(f"Statuses   : {', '.join(tf.COUNTED_STATUSES)}")
    print(f"Buckets    : {args.grain}")
    print(f"Output     : {out_dir}")
    print(f"Run at     : {datetime.now():%Y-%m-%d %H:%M}\n")

    # (filename stem, DataFrame, chart function)
    reports = [
        ("1_trade_volume_by_period",
         guard(tf.trade_volume_by_period(engine, start, end, args.grain), "volume by period"),
         charts.volume_by_period),
        (f"2_top_{args.top}_clients",
         guard(tf.top_clients(engine, start, end, args.top), "top clients"),
         charts.top_customers),
        ("3_volume_by_asset_class",
         guard(tf.volume_by_asset_class(engine, start, end), "volume by asset class"),
         charts.volume_by_asset_class),
        ("4_volume_by_buy_sell",
         guard(tf.volume_by_trade_type(engine, start, end, args.grain), "volume by buy/sell"),
         charts.volume_by_trade_type),
    ]

    for stem, df, draw in reports:
        df.to_csv(out_dir / f"{stem}.csv", index=False)
        draw(df, out_dir, stem)
        print(f"--- {stem} ({len(df)} rows) ---")
        print(df.head(12).to_string(index=False), "\n")

    workbook = out_dir / "trading_report.xlsx"
    with pd.ExcelWriter(workbook) as writer:
        for stem, df, _ in reports:
            df.to_excel(writer, sheet_name=stem[:31], index=False)

    by_period = reports[0][1]
    print(f"Wrote {len(reports) * 2 + 1} files to {out_dir}")
    print(f"Total notional: ${by_period['notional'].sum():,.2f} "
          f"across {int(by_period['trade_count'].sum()):,} trades")


if __name__ == "__main__":
    main()