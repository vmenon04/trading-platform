"""Draws the four report charts as PNG images, using matplotlib.

Each chart function takes a DataFrame from transform.py and saves `<name>.png` in `out_dir`.
"""
import matplotlib
matplotlib.use("Agg")   # draw straight to files, without opening a window
import matplotlib.pyplot as plt
import pandas as pd

FIGURE_SIZE = (10, 6)   # width, height in inches
DPI = 150               # dots per inch, i.e. image sharpness


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def period_labels(df):
    """The 'period' column as short text labels, e.g. 'Jan 2026'."""
    return pd.to_datetime(df["period"]).dt.strftime("%b %Y")


def shorten(names, width=22):
    """Cut long names down to `width` characters and add '…', so they fit on the chart."""
    short_names = []
    for name in names:
        if len(name) > width:
            name = name[:width].strip() + "…"
        short_names.append(name)
    return short_names


def show_dollars(axis):
    """Label an axis as dollars: 1500000 becomes $1,500,000."""
    axis.set_major_formatter(lambda value, position: f"${value:,.0f}")


def save(fig, out_dir, name):
    """Save the figure as a PNG and close it (otherwise matplotlib keeps it in memory)."""
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{name}.png"
    fig.savefig(path, dpi=DPI, bbox_inches="tight")
    plt.close(fig)
    return path


# ---------------------------------------------------------------------------
# The charts
# ---------------------------------------------------------------------------

def volume_by_period(df, out_dir, name):
    """Line chart: traded value per period (left axis) and number of trades (right axis)."""
    fig, ax = plt.subplots(figsize=FIGURE_SIZE)
    labels = period_labels(df)

    ax.plot(labels, df["notional"], marker="o", linewidth=2, color="#1f77b4", label="Notional traded")
    ax.set_ylabel("Notional traded (quantity x price)")
    ax.set_xlabel("Period")
    show_dollars(ax.yaxis)

    # A second y-axis on the right, sharing the same x-axis, for the trade count.
    ax2 = ax.twinx()
    ax2.plot(labels, df["trade_count"], marker="s", linewidth=1.5, linestyle="--", color="#ff7f0e", label="Trade count")
    ax2.set_ylabel("Number of trades")

    # One legend that lists the lines from both axes.
    both_lines = ax.get_lines() + ax2.get_lines()
    ax.legend(handles=both_lines, loc="upper left")
    ax.set_title("Trade volume by period")
    ax.grid(axis="y", alpha=0.3)
    plt.setp(ax.get_xticklabels(), rotation=45, ha="right")
    return save(fig, out_dir, name)


def top_customers(df, out_dir, name):
    """Horizontal bars: traded value per client, biggest at the top."""
    fig, ax = plt.subplots(figsize=FIGURE_SIZE)
    df = df.iloc[::-1]   # reverse the rows, because matplotlib draws the first bar at the bottom
    ax.barh(shorten(df["client_name"]), df["notional"], color="#2ca02c")
    ax.set_xlabel("Notional traded (quantity x price)")
    ax.set_title(f"Top {len(df)} clients by traded value")
    show_dollars(ax.xaxis)
    ax.grid(axis="x", alpha=0.3)
    return save(fig, out_dir, name)


def volume_by_asset_class(df, out_dir, name):
    """Two side-by-side bar charts per asset class: traded value (left) and trade count (right)."""
    fig, (ax_value, ax_count) = plt.subplots(1, 2, figsize=(12, 6), sharey=True, width_ratios=[2, 1])
    df = df.iloc[::-1]

    ax_value.barh(df["asset_class"], df["notional"], color="#9467bd")
    ax_value.set_xlabel("Notional traded (quantity x price)")
    ax_value.set_title("Traded value")
    show_dollars(ax_value.xaxis)
    ax_value.grid(axis="x", alpha=0.3)

    ax_count.barh(df["asset_class"], df["trade_count"], color="#8c8c8c")
    ax_count.set_xlabel("Number of trades")
    ax_count.set_title("Trade count")
    ax_count.grid(axis="x", alpha=0.3)

    fig.suptitle("Trade volume by asset class")
    fig.tight_layout()
    return save(fig, out_dir, name)


def volume_by_trade_type(df, out_dir, name):
    """Pairs of bars per period: buy value (green) next to sell value (red)."""
    fig, ax = plt.subplots(figsize=FIGURE_SIZE)
    positions = list(range(len(df)))
    bar_width = 0.4

    # Shift buy bars a little left and sell bars a little right, so they sit side by side.
    buy_positions = [x - bar_width / 2 for x in positions]
    sell_positions = [x + bar_width / 2 for x in positions]
    ax.bar(buy_positions, df["BUY"], bar_width, label="BUY", color="#2ca02c")
    ax.bar(sell_positions, df["SELL"], bar_width, label="SELL", color="#d62728")

    ax.set_xticks(positions)
    ax.set_xticklabels(period_labels(df), rotation=45, ha="right")
    ax.set_ylabel("Notional traded (quantity x price)")
    ax.set_xlabel("Period")
    ax.set_title("Trade volume by buy / sell")
    show_dollars(ax.yaxis)
    ax.legend()
    ax.grid(axis="y", alpha=0.3)
    return save(fig, out_dir, name)
