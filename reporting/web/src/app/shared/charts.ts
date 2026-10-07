// Builders for every chart the pages draw. Each returns a Chart.js config;
// <app-chart> renders it. Colours come from the CSS tokens in styles.css.

import { Chart, ChartConfiguration, ChartDataset, TooltipItem, registerables } from 'chart.js';
import { signal } from '@angular/core';

import { count, money, moneyExact, partialPeriods, percent, periodLabels, Range } from '../core/format';
import { AssetClassRow, BuySellRow, OrderStatus, StatusMixRow } from '../core/models';

Chart.register(...registerables);

/** Flips when the OS switches light/dark; builders read it so charts redraw in the new colours. */
const darkMode = matchMedia('(prefers-color-scheme: dark)');
export const colorScheme = signal(darkMode.matches ? 'dark' : 'light');
darkMode.addEventListener('change', (event) => colorScheme.set(event.matches ? 'dark' : 'light'));

function css(name: string): string {
  colorScheme(); // a dependency, so computed charts rebuild on a theme change
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

type Format = { format(value: number): string };
type Config = ChartConfiguration<'line' | 'bar'>;

function options({ horizontal = false, legend = false, stacked = false, format = money as Format } = {}): Config['options'] {
  const valueAxis = {
    stacked,
    grid: { color: css('--grid') },
    border: { display: false },
    ticks: { color: css('--muted'), callback: (value: string | number) => format.format(Number(value)) },
  };
  const categoryAxis = {
    stacked,
    grid: { display: false },
    border: { color: css('--axis') },
    ticks: { color: css('--muted') },
  };
  return {
    responsive: true,
    maintainAspectRatio: false,
    animation: false,
    indexAxis: horizontal ? 'y' : 'x',
    scales: horizontal ? { x: valueAxis, y: categoryAxis } : { x: categoryAxis, y: valueAxis },
    plugins: {
      legend: { display: legend, labels: { color: css('--text-secondary'), usePointStyle: true } },
      tooltip: {
        callbacks: {
          label: (item: TooltipItem<'line' | 'bar'>) => ` ${item.dataset.label}: ${format.format(item.raw as number)}`,
        },
      },
    },
  };
}

function bars(label: string, data: (number | null)[], color: string): ChartDataset<'bar'> {
  return {
    label,
    data,
    backgroundColor: color,
    borderRadius: 4,
    borderSkipped: 'start', // square at the baseline, rounded at the value end
    maxBarThickness: 24,
  };
}

function line(label: string, data: (number | null)[], color: string, partial: boolean[]): ChartDataset<'line'> {
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
      datasets: [line(label, rows.map(value), css('--series-1'), partialPeriods(rows, range))],
    },
    options: config,
  };
}

/** Fill rate and rejection rate over time, as percentages on one axis. */
export function rates(rows: StatusMixRow[], range: Range): Config {
  const partial = partialPeriods(rows, range);
  const percentFormat = { format: (value: number) => `${percent.format(value)}%` };
  const config = options({ legend: true, format: percentFormat });
  config!.interaction = { mode: 'index', intersect: false };
  config!.plugins!.tooltip!.callbacks!.afterBody = (items) => {
    const row = rows[items[0].dataIndex];
    return `${count.format(row.orders)} orders placed`;
  };
  return {
    type: 'line',
    data: {
      labels: periodLabels(rows, range),
      datasets: [
        line('Fill rate', rows.map((row) => row.fill_rate), css('--series-1'), partial),
        line('Rejection rate', rows.map((row) => row.reject_rate), css('--series-2'), partial),
      ],
    },
    options: config,
  };
}

/** Orders placed per period, stacked by their current status. */
export function outcomes(rows: StatusMixRow[], range: Range): Config {
  const series: [OrderStatus, string, string][] = [
    ['FULFILLED', 'Fulfilled', '--series-1'],
    ['REJECTED', 'Rejected', '--series-2'],
    ['ACCEPTED', 'Accepted', '--series-3'],
    ['PENDING', 'Pending', '--series-4'],
  ];
  return {
    type: 'bar',
    data: {
      labels: periodLabels(rows, range),
      datasets: series.map(([status, label, color]) => ({
        ...bars(label, rows.map((row) => row[status]), css(color)),
        borderRadius: 0,
        borderColor: css('--surface'),
        borderWidth: { top: 2 }, // the 2px surface gap between stacked segments
      })),
    },
    options: options({ legend: true, stacked: true, format: count }),
  };
}

export function buySell(rows: BuySellRow[], range: Range): Config {
  return {
    type: 'bar',
    data: {
      labels: periodLabels(rows, range),
      datasets: [
        bars('Buy', rows.map((row) => row.BUY), css('--series-1')),
        bars('Sell', rows.map((row) => row.SELL), css('--series-2')),
      ],
    },
    options: options({ legend: true }),
  };
}

/** A ranking as horizontal bars, biggest first. */
export function ranking<T>(
  rows: T[],
  { label, value, afterLabel }: { label: (row: T) => string; value: (row: T) => number; afterLabel?: (row: T) => string },
): Config {
  const config = options({ horizontal: true });
  config!.plugins!.tooltip!.callbacks!.label = (item) => ` Traded value: ${moneyExact.format(item.raw as number)}`;
  if (afterLabel) config!.plugins!.tooltip!.callbacks!.afterLabel = (item) => afterLabel(rows[item.dataIndex]);
  return {
    type: 'bar',
    data: { labels: rows.map(label), datasets: [bars('Traded value', rows.map(value), css('--series-1'))] },
    options: config,
  };
}

/** One 24px row per bar, so 50 rows stay readable instead of squashed. */
export function rankingHeight(rows: number): number {
  return Math.max(280, rows * 24 + 40);
}

export function assetClasses(rows: AssetClassRow[]): Config {
  return ranking(rows, {
    label: (row) => row.asset_class,
    value: (row) => row.notional,
    afterLabel: (row) => ` ${row.pct_of_notional}% of value · ${count.format(row.trade_count)} trades`,
  });
}
