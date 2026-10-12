"""One function per chart. Each writes a PNG and returns its path."""
from __future__ import annotations

from pathlib import Path

import matplotlib
matplotlib.use("Agg")           # no display needed; required for scheduled runs
import matplotlib.pyplot as plt
import pandas as pd
from matplotlib.ticker import FuncFormatter

import transform as tr

FIGSIZE = (10, 6)
DPI = 150


def _money(ax, axis: str = "y") -> None:
    fmt = FuncFormatter(lambda v, _: f"${v:,.0f}")
    (ax.yaxis if axis == "y" else ax.xaxis).set_major_formatter(fmt)


def _save(fig, out_dir: Path, name: str) -> Path:
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{name}.png"
    fig.savefig(path, dpi=DPI, bbox_inches="tight")
    plt.close(fig)               # closing matters: repeated runs otherwise leak figures
    return path


def volume_by_period(df: pd.DataFrame, out_dir: Path, name: str) -> Path:
    """A time series, so a line chart: notional plus trade count on a second axis."""
    fig, ax = plt.subplots(figsize=FIGSIZE)
    labels = tr.period_labels(df)

    ax.plot(labels, df["notional"], marker="o", linewidth=2, color="#1f77b4",
            label="Notional traded")
    ax.set_ylabel("Notional traded (quantity x price)")
    ax.set_xlabel("Period")
    _money(ax)

    ax2 = ax.twinx()
    ax2.plot(labels, df["trade_count"], marker="s", linewidth=1.5,
             linestyle="--", color="#ff7f0e", label="Trade count")
    ax2.set_ylabel("Number of trades")

    lines = ax.get_lines() + ax2.get_lines()
    ax.legend(lines, [l.get_label() for l in lines], loc="upper left")
    ax.set_title("Trade volume by period")
    ax.grid(axis="y", alpha=0.3)
    plt.setp(ax.get_xticklabels(), rotation=45, ha="right")
    return _save(fig, out_dir, name)


def top_customers(df: pd.DataFrame, out_dir: Path, name: str) -> Path:
    """A ranking, so horizontal bars with the biggest at the top.

    Bars carry one measure only — traded value. Trade counts are in the CSV
    beside this chart; putting them on the bars mixes dollars and counts in a
    single graphic and invites the reader to compare two different units.
    """
    fig, ax = plt.subplots(figsize=FIGSIZE)
    plot_df = df.iloc[::-1]      # matplotlib draws bottom-up
    ax.barh(tr.shorten(plot_df["client_name"]), plot_df["notional"], color="#2ca02c")
    ax.set_xlabel("Notional traded (quantity x price)")
    ax.set_ylabel("")
    ax.set_title(f"Top {len(df)} clients by traded value")
    _money(ax, axis="x")
    ax.grid(axis="x", alpha=0.3)
    return _save(fig, out_dir, name)


def volume_by_asset_class(df: pd.DataFrame, out_dir: Path, name: str) -> Path:
    """Two panels sharing the category axis: value on the left, number of trades
    on the right. Each measure gets its own scale, so neither borrows authority
    from the other."""
    fig, (ax_value, ax_count) = plt.subplots(
        1, 2, figsize=(12, 6), sharey=True, gridspec_kw={"width_ratios": [2, 1]})
    plot_df = df.iloc[::-1]

    ax_value.barh(plot_df["asset_class"], plot_df["notional"], color="#9467bd")
    ax_value.set_xlabel("Notional traded (quantity x price)")
    ax_value.set_title("Traded value")
    _money(ax_value, axis="x")
    ax_value.grid(axis="x", alpha=0.3)

    ax_count.barh(plot_df["asset_class"], plot_df["trade_count"], color="#8c8c8c")
    ax_count.set_xlabel("Number of trades")
    ax_count.set_title("Trade count")
    ax_count.grid(axis="x", alpha=0.3)

    fig.suptitle("Trade volume by asset class")
    fig.tight_layout()
    return _save(fig, out_dir, name)


def volume_by_trade_type(wide: pd.DataFrame, out_dir: Path, name: str) -> Path:
    """Two categories within each period, so grouped bars."""
    fig, ax = plt.subplots(figsize=FIGSIZE)
    labels = tr.period_labels(wide)
    x = range(len(wide))
    width = 0.4

    ax.bar([i - width / 2 for i in x], wide["BUY"], width, label="BUY", color="#2ca02c")
    ax.bar([i + width / 2 for i in x], wide["SELL"], width, label="SELL", color="#d62728")

    ax.set_xticks(list(x))
    ax.set_xticklabels(labels, rotation=45, ha="right")
    ax.set_ylabel("Notional traded (quantity x price)")
    ax.set_xlabel("Period")
    ax.set_title("Trade volume by buy / sell")
    _money(ax)
    ax.legend()
    ax.grid(axis="y", alpha=0.3)
    return _save(fig, out_dir, name)