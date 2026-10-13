// Number, date and period formatting shared by every page.

import { Grain } from './models';

export const money = new Intl.NumberFormat('en-US', {
  style: 'currency', currency: 'USD', notation: 'compact', maximumFractionDigits: 1,
});
export const moneyExact = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
export const count = new Intl.NumberFormat('en-US');
export const percent = new Intl.NumberFormat('en-US', { maximumFractionDigits: 1 });

/** "2025-10-01T00:00:00.000" -> a local Date, without the UTC shift new Date() would add. */
export function day(iso: string): Date {
  const [y, m, d] = iso.slice(0, 10).split('-').map(Number);
  return new Date(y, m - 1, d);
}

/** A Date as YYYY-MM-DD, the format date inputs and the API use. */
export function isoDay(date: Date): string {
  return date.toLocaleDateString('en-CA');
}

export function addDays(iso: string, days: number): string {
  const date = day(iso);
  date.setDate(date.getDate() + days);
  return isoDay(date);
}

export function dateTime(iso: string): string {
  return new Date(iso).toLocaleString('en-US', { dateStyle: 'medium', timeStyle: 'short' });
}

export function minutesAgo(iso: string): string {
  const minutes = Math.round((Date.now() - new Date(iso).getTime()) / 60000);
  return minutes < 60 ? `${minutes} min ago` : `${Math.round(minutes / 60)} h ago`;
}

export function periodLabel(iso: string, grain: Grain): string {
  const date = day(iso);
  if (grain === 'year') return String(date.getFullYear());
  if (grain === 'quarter') return `Q${Math.floor(date.getMonth() / 3) + 1} ${date.getFullYear()}`;
  if (grain === 'month') return date.toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

function periodEnd(start: Date, grain: Grain): Date {
  const end = new Date(start);
  if (grain === 'day') end.setDate(end.getDate() + 1);
  if (grain === 'week') end.setDate(end.getDate() + 7);
  if (grain === 'month') end.setMonth(end.getMonth() + 1);
  if (grain === 'quarter') end.setMonth(end.getMonth() + 3);
  if (grain === 'year') end.setFullYear(end.getFullYear() + 1);
  return end;
}

/** The date range a chart covers: end is exclusive, like the API's. */
export interface Range {
  start: string;
  end: string;
  grain: Grain;
}

/**
 * Periods the range only partly covers (like the current month) hold less data
 * than a full one, so they get labelled rather than left to look like a drop.
 */
export function partialPeriods(rows: { period: string }[], range: Range): boolean[] {
  return rows.map(({ period }) => {
    const start = day(period);
    return start < day(range.start) || periodEnd(start, range.grain) > day(range.end);
  });
}

export function periodLabels(rows: { period: string }[], range: Range): string[] {
  const partial = partialPeriods(rows, range);
  return rows.map((row, i) => periodLabel(row.period, range.grain) + (partial[i] ? ' (partial)' : ''));
}

/** Drops empty values so they are left out of a request's query string. */
export function cleanParams(params: Record<string, string | number | null | undefined | readonly string[]>) {
  const clean: Record<string, string | number | readonly string[]> = {};
  for (const [key, value] of Object.entries(params)) {
    if (value !== null && value !== undefined && value !== '' && !(Array.isArray(value) && value.length === 0)) {
      clean[key] = value;
    }
  }
  return clean;
}
