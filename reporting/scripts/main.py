"""Builds the reporting pack: four reports, each saved as a CSV table and a PNG chart.

    python reporting/scripts/main.py                  # last 12 months, monthly buckets
    python reporting/scripts/main.py --grain quarter  # quarterly buckets
    python reporting/scripts/main.py --months 3       # last 3 months only
    python reporting/scripts/main.py --all-time       # every trade on record
    python reporting/scripts/main.py --top 20         # top 20 clients instead of 10

Each run writes a new dated folder under reporting/reports/, plus an Excel workbook with
all four reports as sheets. Nothing is overwritten, so older packs stay as they were.
"""
import argparse
from pathlib import Path

import pandas as pd

import charts
import transform as tf
from config import REPORTS_DIR, get_analytics_engine


def read_options():
    """Read the --options typed after `python main.py`."""
    parser = argparse.ArgumentParser(description="Trading platform reporting pack")
    parser.add_argument("--grain", default="month", choices=["day", "week", "month", "quarter", "year"])
    parser.add_argument("--months", type=int, default=12, help="months of history to include")
    parser.add_argument("--all-time", action="store_true", help="ignore --months and use every trade")
    parser.add_argument("--top", type=int, default=10, help="how many clients in the top-clients report")
    parser.add_argument("--out", default=REPORTS_DIR, help="folder to write the pack to")
    return parser.parse_args()


def check(df, name):
    """Stop the run rather than save a misleading report."""
    if df.empty:
        raise SystemExit(f"[{name}] returned no rows. Check the date range and trade statuses.")
    for column in ["notional", "units", "BUY", "SELL"]:
        if column in df.columns and (df[column] < 0).any():
            raise SystemExit(f"[{name}] has a negative {column}, which should be impossible.")


def main():
    options = read_options()
    engine = get_analytics_engine()

    months_back = None if options.all_time else options.months
    start, end = tf.reporting_window(engine, months_back)
    filters = {"start": start, "end": end}

    out_dir = Path(options.out) / f"{start:%Y%m%d}_{end:%Y%m%d}_{options.grain}"
    print(f"Dates  : {start} up to {end} (not including {end})")
    print(f"Buckets: {options.grain}")
    print(f"Output : {out_dir}\n")

    # Each report: (file name, its data, the function that draws its chart)
    reports = [
        ("1_trade_volume_by_period", tf.trade_volume_by_period(engine, filters, options.grain), charts.volume_by_period),
        (f"2_top_{options.top}_clients", tf.top_clients(engine, filters, options.top), charts.top_customers),
        ("3_volume_by_asset_class", tf.volume_by_asset_class(engine, filters), charts.volume_by_asset_class),
        ("4_volume_by_buy_sell", tf.volume_by_trade_type(engine, filters, options.grain), charts.volume_by_trade_type),
    ]

    # Check every report before writing anything, so a bad run doesn't leave half a pack.
    for name, df, draw_chart in reports:
        check(df, name)

    out_dir.mkdir(parents=True, exist_ok=True)
    for name, df, draw_chart in reports:
        df.to_csv(out_dir / f"{name}.csv", index=False)
        draw_chart(df, out_dir, name)
        print(f"--- {name} ({len(df)} rows) ---")
        print(df.head(12).to_string(index=False), "\n")

    # Excel sheet names can be at most 31 characters long.
    with pd.ExcelWriter(out_dir / "trading_report.xlsx") as workbook:
        for name, df, draw_chart in reports:
            df.to_excel(workbook, sheet_name=name[:31], index=False)

    volume = reports[0][1]
    print(f"Wrote {len(reports) * 2 + 1} files to {out_dir}")
    print(f"Total notional: ${volume['notional'].sum():,.2f} across {int(volume['trade_count'].sum()):,} trades")


if __name__ == "__main__":
    main()
