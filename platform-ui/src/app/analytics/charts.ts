// Builders for every chart the analyst pages draw. Each returns a Chart.js config;
// <app-chart> renders it. Colours come from the --chart-* tokens in src/analytics.css.

import { Chart, ChartConfiguration, ChartDataset, TooltipItem, registerables } from 'chart.js';

import { count, money, moneyExact, partialPeriods, periodLabels, Range } from './format';
import { AssetClassRow, BuySellRow, PortfolioBandRow } from './models';

Chart.register(...registerables);
Chart.defaults.font.family = '"DM Sans", sans-serif';
Chart.defaults.font.size = 11;

function css(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

/**
 * The colour of the n-th picked client (0-based), in a fixed order so a client keeps its colour as others are
 * added or removed. Eight, checked for colour-blind separation as a set, which is why at most 8 can be picked.
 */
export function clientColor(slot: number): string {
  return css(`--chart-client-${slot + 1}`);
}

type Format = { format(value: number): string };
type Config = ChartConfiguration<'line' | 'bar'>;

function options({ horizontal = false, legend = false, stacked = false, format = money as Format } = {}): Config['options'] {
  const valueAxis = {
    stacked,
    grid: { color: css('--chart-grid') },
    border: { display: false },
    ticks: { color: css('--muted'), callback: (value: string | number) => format.format(Number(value)) },
  };
  const categoryAxis = {
    stacked,
    grid: { display: false },
    border: { color: css('--chart-axis') },
    ticks: { color: css('--muted') },
  };
  return {
    responsive: true,
    maintainAspectRatio: false,
    animation: false,
    indexAxis: horizontal ? 'y' : 'x',
    scales: horizontal ? { x: valueAxis, y: categoryAxis } : { x: categoryAxis, y: valueAxis },
    plugins: {
      legend: { display: legend, labels: { color: css('--ink'), usePointStyle: true, boxHeight: 7 } },
      tooltip: {
        backgroundColor: css('--nav'),
        padding: 10,
        cornerRadius: 8,
        callbacks: {
          label: (item: TooltipItem<'line' | 'bar'>) => ` ${item.dataset.label}: ${format.format(item.raw as number)}`,
        },
      },
    },
  };
}

// Typed as 'line' | 'bar' (not just 'bar') so it fits Config, which can hold either kind.
function bars(label: string, data: (number | null)[], color: string, stacked = false): ChartDataset<'line' | 'bar'> {
  return {
    label,
    data,
    backgroundColor: color,
    borderRadius: stacked ? 0 : 4,
    borderSkipped: 'start', // square at the baseline, rounded at the value end
    maxBarThickness: 24,
    // stacked segments get a 2px gap in the card colour between them
    borderColor: stacked ? css('--surface') : undefined,
    borderWidth: stacked ? { left: 2 } : undefined,
  };
}

function line(label: string, data: (number | null)[], color: string, partial: boolean[]): ChartDataset<'line' | 'bar'> {
  return {
    label,
    data,
    borderColor: color,
    backgroundColor: color,
    borderWidth: 2,
    pointRadius: 4,
    pointBorderWidth: 2,
    pointBorderColor: css('--surface'),
    spanGaps: true,
    segment: {
      // dashed into a partial period, and across a period with no value
      borderDash: (ctx) => {
        const gap = ctx.p1DataIndex - ctx.p0DataIndex > 1;
        return gap || partial[ctx.p0DataIndex] || partial[ctx.p1DataIndex] ? [4, 4] : undefined;
      },
    },
  };
}

/** One measure over time. */
export function periodLine<T extends { period: string }>(
  rows: T[], range: Range,
  { label, value, afterLabel, format = money as Format, beginAtZero = true }: {
    label: string; value: (row: T) => number | null; afterLabel?: (row: T) => string;
    format?: Format; beginAtZero?: boolean;
  },
): Config {
  const config = options({ format });
  (config!.scales!['y'] as { beginAtZero?: boolean }).beginAtZero = beginAtZero;
  config!.interaction = { mode: 'index', intersect: false };
  if (afterLabel) config!.plugins!.tooltip!.callbacks!.afterLabel = (item) => afterLabel(rows[item.dataIndex]);
  return {
    type: 'line',
    data: {
      labels: periodLabels(rows, range),
      datasets: [line(label, rows.map(value), css('--chart-primary'), partialPeriods(rows, range))],
    },
    options: config,
  };
}

/** One line per series over the same periods, e.g. each picked client's traded value. */
export function periodLines(
  periods: { period: string }[], range: Range,
  series: { label: string; color: string; values: number[] }[],
): Config {
  const config = options({ legend: true });
  config!.interaction = { mode: 'index', intersect: false };
  const partial = partialPeriods(periods, range);
  return {
    type: 'line',
    data: {
      labels: periodLabels(periods, range),
      datasets: series.map((one) => line(one.label, one.values, one.color, partial)),
    },
    options: config,
  };
}

export function buySell(rows: BuySellRow[], range: Range): Config {
  return {
    type: 'bar',
    data: {
      labels: periodLabels(rows, range),
      datasets: [
        bars('Buy', rows.map((row) => row.BUY), css('--chart-buy')),
        bars('Sell', rows.map((row) => row.SELL), css('--chart-sell')),
      ],
    },
    options: options({ legend: true }),
  };
}

/** A ranking as horizontal bars, biggest first. `color` picks each bar's colour (one colour if left out). */
export function ranking<T>(
  rows: T[],
  { label, value, afterLabel, measure = 'Traded value', color }: {
    label: (row: T) => string; value: (row: T) => number; afterLabel?: (row: T) => string; measure?: string;
    color?: (row: T) => string;
  },
): Config {
  const config = options({ horizontal: true });
  config!.plugins!.tooltip!.callbacks!.label = (item) => ` ${measure}: ${moneyExact.format(item.raw as number)}`;
  if (afterLabel) config!.plugins!.tooltip!.callbacks!.afterLabel = (item) => afterLabel(rows[item.dataIndex]);
  const dataset = bars(measure, rows.map(value), css('--chart-primary'));
  if (color) dataset.backgroundColor = rows.map(color);
  return {
    type: 'bar',
    data: { labels: rows.map(label), datasets: [dataset] },
    options: config,
  };
}

/** Cash and invested side by side in one bar per client: what each portfolio is made of. */
export function portfolioMix<T>(
  rows: T[], { label, cash, invested }: { label: (row: T) => string; cash: (row: T) => number; invested: (row: T) => number },
): Config {
  const config = options({ horizontal: true, legend: true, stacked: true });
  config!.interaction = { mode: 'index', intersect: false };
  config!.plugins!.tooltip!.callbacks!.label = (item) => ` ${item.dataset.label}: ${moneyExact.format(item.raw as number)}`;
  return {
    type: 'bar',
    data: {
      labels: rows.map(label),
      datasets: [
        bars('Invested', rows.map(invested), css('--chart-primary'), true),
        bars('Cash', rows.map(cash), css('--chart-secondary'), true),
      ],
    },
    options: config,
  };
}

/** How many clients are worth how much: one bar per value band, smallest band on the left. */
export function valueHistogram(rows: PortfolioBandRow[]): Config {
  const config = options({ format: count });
  config!.plugins!.tooltip!.callbacks!.title = (items) => {
    const row = rows[items[0].dataIndex];
    return `${moneyExact.format(row.low)} – ${moneyExact.format(row.high)}`;
  };
  config!.plugins!.tooltip!.callbacks!.label = (item) => ` Clients: ${count.format(item.raw as number)}`;
  config!.plugins!.tooltip!.callbacks!.afterLabel = (item) => ` Combined value: ${money.format(rows[item.dataIndex].total_value)}`;
  const dataset = bars('Clients', rows.map((row) => row.clients), css('--chart-primary'));
  // bars nearly touch, as a histogram's should, with the 2px gap between them
  Object.assign(dataset, { maxBarThickness: undefined, categoryPercentage: 1, barPercentage: 0.96 });
  return {
    type: 'bar',
    data: { labels: rows.map((row) => `${money.format(row.low)}–${money.format(row.high)}`), datasets: [dataset] },
    options: config,
  };
}

/** One 24px row per bar, so long rankings stay readable instead of squashed. */
export function rankingHeight(rows: number): number {
  return Math.max(260, rows * 28 + 40);
}

export function assetClasses(rows: AssetClassRow[]): Config {
  return ranking(rows, {
    label: (row) => row.asset_class,
    value: (row) => row.notional,
    afterLabel: (row) => ` ${row.pct_of_notional}% of value · ${count.format(row.trade_count)} trades`,
  });
}
