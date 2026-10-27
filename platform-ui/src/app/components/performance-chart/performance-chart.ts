import { Component, computed, inject, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { PerformancePoint, PerformanceRange } from '../../models/account.models';
import { AccountService } from '../../services/account.service';

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const signedUsd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', signDisplay: 'exceptZero' });
const shortDate = new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', timeZone: 'UTC' });
const longDate = new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric', timeZone: 'UTC' });

// the svg's own units; it stretches to the card's width
const WIDTH = 640;
const HEIGHT = 208;
const TOP = 14;
const BOTTOM = 204;

const RANGES: { range: PerformanceRange; label: string }[] = [
  { range: 'ONE_MONTH', label: '1M' },
  { range: 'THREE_MONTHS', label: '3M' },
  { range: 'ONE_YEAR', label: '1Y' },
  { range: 'ALL', label: 'All' },
];

interface Geometry {
  // changes with the data, so the lines are drawn again (see the @for in the template)
  key: string;
  points: PerformancePoint[];
  xs: number[];
  valueYs: number[];
  investedYs: number[];
  line: string;
  area: string;
  invested: string;
  labels: { x: number; text: string; anchor: string }[];
}

const dateOf = (point: PerformancePoint) => new Date(`${point.date}T00:00:00Z`);

// The dashboard's performance chart (GET /v1/accounts/{id}/performance): what the holdings were worth each day, as a
// line that draws itself in, against a dashed line of what was paid for them. Hovering shows a day's numbers.
@Component({
  selector: 'app-performance-chart',
  styleUrl: './performance-chart.css',
  templateUrl: './performance-chart.html',
})
export class PerformanceChart {
  private account = inject(AccountService);

  protected readonly ranges = RANGES;
  protected readonly range = signal<PerformanceRange>('THREE_MONTHS');
  protected readonly hovered = signal<number | null>(null);
  protected readonly width = WIDTH;
  protected readonly height = HEIGHT;
  protected readonly bottom = BOTTOM;

  protected readonly performance = httpResource<PerformancePoint[]>(() => {
    const accountId = this.account.accountId();
    return accountId ? { url: `/v1/accounts/${accountId}/performance`, params: { range: this.range() } } : undefined;
  });

  protected readonly points = computed(() => this.performance.value() ?? []);

  protected readonly geometry = computed<Geometry | null>(() => {
    const points = this.points();
    if (points.length === 0) {
      return null;
    }
    const all = points.flatMap((point) => [point.value, point.invested]);
    let low = Math.min(...all);
    let high = Math.max(...all);
    // a little room above and below the lines; a flat line sits in the middle
    const pad = high > low ? (high - low) * 0.12 : Math.max(1, high * 0.05);
    low -= pad;
    high += pad;

    const last = points.length - 1;
    const xs = points.map((_, i) => (last === 0 ? WIDTH / 2 : (i / last) * WIDTH));
    const y = (value: number) => TOP + (1 - (value - low) / (high - low)) * (BOTTOM - TOP);
    const valueYs = points.map((point) => y(point.value));
    const investedYs = points.map((point) => y(point.invested));
    const path = (ys: number[]) =>
      last === 0 ? `M0 ${ys[0]} H${WIDTH}` : ys.map((yy, i) => `${i === 0 ? 'M' : 'L'}${xs[i].toFixed(1)} ${yy.toFixed(1)}`).join(' ');
    const line = path(valueYs);
    const labelAt = [0, Math.floor(last / 2), last].filter((i, n, all) => all.indexOf(i) === n);
    return {
      key: `${this.range()}:${points[0].date}:${points.length}`,
      points,
      xs,
      valueYs,
      investedYs,
      line,
      area: `${line} L${last === 0 ? WIDTH : xs[last].toFixed(1)} ${BOTTOM} L0 ${BOTTOM} Z`,
      invested: path(investedYs),
      labels: labelAt.map((i) => ({
        x: xs[i],
        text: shortDate.format(dateOf(points[i])),
        anchor: last === 0 ? 'middle' : i === 0 ? 'start' : i === last ? 'end' : 'middle',
      })),
    };
  });

  // the latest value, and how much was gained or lost over the period (growth beyond the money put in)
  protected readonly current = computed(() => {
    const points = this.points();
    return points.length ? usd.format(points[points.length - 1].value) : '—';
  });
  protected readonly change = computed(() => {
    const points = this.points();
    if (points.length === 0) {
      return null;
    }
    const first = points[0];
    const last = points[points.length - 1];
    const amount = last.value - last.invested - (first.value - first.invested);
    return { text: signedUsd.format(amount), tone: amount > 0 ? 'up' : amount < 0 ? 'down' : 'flat' };
  });

  // the hovered day's numbers, and where to put the crosshair (in percent of the chart's width)
  protected readonly tip = computed(() => {
    const index = this.hovered();
    const geometry = this.geometry();
    if (index === null || !geometry) {
      return null;
    }
    const point = geometry.points[index];
    return {
      x: geometry.xs[index],
      left: (geometry.xs[index] / WIDTH) * 100,
      valueY: geometry.valueYs[index],
      investedY: geometry.investedYs[index],
      date: longDate.format(dateOf(point)),
      value: usd.format(point.value),
      invested: usd.format(point.invested),
    };
  });

  protected readonly summary = computed(() => {
    const change = this.change();
    return change ? `Holdings worth ${this.current()} now, ${change.text} over the period` : 'No performance yet';
  });

  protected pick(range: PerformanceRange): void {
    this.hovered.set(null);
    this.range.set(range);
  }

  protected hover(event: PointerEvent): void {
    const points = this.points();
    const target = event.currentTarget as Element;
    const box = target.getBoundingClientRect();
    if (points.length === 0 || box.width === 0) {
      return;
    }
    const ratio = Math.min(1, Math.max(0, (event.clientX - box.left) / box.width));
    this.hovered.set(Math.round(ratio * (points.length - 1)));
  }
}
