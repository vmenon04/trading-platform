import { Component, WritableSignal, computed, signal } from '@angular/core';

import { ChartComponent } from '../../analytics/chart';
import * as charts from '../../analytics/charts';
import { ClientPicker, PickedClient } from '../../analytics/client-picker';
import { DataFreshness } from '../../analytics/data-freshness';
import { DateRangeComponent } from '../../analytics/date-range';
import { count, money } from '../../analytics/format';
import {
  AssetClassRow, BuySellRow, ClientVolumeRow, PortfolioBandRow, PortfoliosPage, TopClientRow, TradeRow, VolumeRow,
} from '../../analytics/models';
import { dateRange, latest, problem, report } from '../../analytics/report';
import { Tile, TilesComponent } from '../../analytics/tiles';
import { TradesTableComponent } from '../../analytics/trades-table';

/** Quick picks for the account value filter; null = no limit on that side. */
const VALUE_BANDS: { label: string; min: number | null; max: number | null }[] = [
  { label: 'Any value', min: null, max: null },
  { label: 'Under $1M', min: null, max: 1e6 },
  { label: '$1M – $10M', min: 1e6, max: 1e7 },
  { label: '$10M – $100M', min: 1e7, max: 1e8 },
  { label: '$100M – $250M', min: 1e8, max: 2.5e8 },
  { label: '$250M+', min: 2.5e8, max: null },
];

/** Quick picks for the time period: months back from the last trade. */
const PERIODS = [1, 3, 6, 12];

/**
 * Client analysis: narrow clients down by who they are (picked by name), what they hold (account value =
 * cash + holdings) and when they traded, and every chart below redraws for just those clients.
 */
@Component({
  selector: 'app-analyst-clients',
  imports: [ChartComponent, ClientPicker, DataFreshness, DateRangeComponent, TilesComponent, TradesTableComponent],
  templateUrl: './analyst-clients.html',
  styleUrl: './analyst-clients.css',
})
export class AnalystClients {
  protected readonly maxClients = 8;   // one chart colour each; the API refuses more
  protected readonly valueBands = VALUE_BANDS;
  protected readonly periods = PERIODS;
  protected readonly rankingHeight = charts.rankingHeight;

  // ---------- filters ----------
  protected readonly picked = signal<PickedClient[]>([]);
  protected readonly minValue = signal<number | null>(null);
  protected readonly maxValue = signal<number | null>(null);
  protected readonly badAmount = signal(false);
  protected readonly dates = dateRange();

  protected readonly anyPicked = computed(() => this.picked().length > 0);
  private readonly clientIds = computed(() => this.picked().map((client) => client.id));

  /** What the charts are about, for their captions. */
  protected readonly scope = computed(() => {
    const picked = this.picked().length;
    const who = picked ? `${picked} picked client${picked > 1 ? 's' : ''}` : 'All clients';
    const min = this.minValue(), max = this.maxValue();
    const worth = min !== null && max !== null ? `worth ${money.format(min)}–${money.format(max)}`
      : min !== null ? `worth ${money.format(min)}+` : max !== null ? `worth up to ${money.format(max)}` : '';
    return [who, worth].filter(Boolean).join(' ');
  });

  // ---------- requests ----------
  // what the clients hold right now: no dates
  private readonly holdingParams = computed(() => ({
    client_ids: this.clientIds(), min_value: this.minValue(), max_value: this.maxValue(),
  }));
  // how they traded: the dates, and the same clients
  private readonly tradingParams = computed(() => {
    const range = this.dates.range();
    return range && {
      ...range, client_ids: this.clientIds(), min_portfolio: this.minValue(), max_portfolio: this.maxValue(),
    };
  });

  private readonly portfolios = report<PortfoliosPage>(
    () => '/api/analyst/portfolios', () => ({ ...this.holdingParams(), page_size: this.maxClients }));
  private readonly distribution = report<PortfolioBandRow[]>(
    () => (this.anyPicked() ? undefined : '/api/analyst/portfolio-distribution'), this.holdingParams);
  private readonly volume = report<VolumeRow[]>(() => '/api/analyst/volume', this.tradingParams);
  private readonly buySell = report<BuySellRow[]>(() => '/api/analyst/buy-sell', this.tradingParams);
  private readonly byClass = report<AssetClassRow[]>(() => '/api/analyst/by-asset-class', this.tradingParams);
  private readonly byClient = report<ClientVolumeRow[]>(
    () => (this.anyPicked() ? '/api/analyst/volume-by-client' : undefined), this.tradingParams);
  private readonly perClient = report<TopClientRow[]>(
    () => (this.anyPicked() ? '/api/analyst/top-clients' : undefined),
    () => this.tradingParams() && { ...this.tradingParams(), limit: this.maxClients });
  private readonly trades = report<TradeRow[]>(
    () => (this.anyPicked() ? '/api/analyst/trades' : undefined), () => ({ client_ids: this.clientIds(), limit: 20 }));

  protected readonly problem = problem(this.portfolios, this.volume, this.buySell, this.byClass, this.distribution);

  private readonly portfolioPage = latest(this.portfolios);
  private readonly bandRows = latest(this.distribution);
  private readonly volumeRows = latest(this.volume);
  private readonly buySellRows = latest(this.buySell);
  private readonly classRows = latest(this.byClass);
  private readonly byClientRows = latest(this.byClient);
  private readonly perClientRows = latest(this.perClient);
  protected readonly tradeRows = computed(() => (this.trades.hasValue() ? this.trades.value() : []));

  // ---------- tiles ----------
  protected readonly tiles = computed<Tile[]>(() => {
    const page = this.portfolioPage();
    const volume = this.volumeRows() ?? [];
    const traded = volume.reduce((sum, row) => sum + row.notional, 0);
    const trades = volume.reduce((sum, row) => sum + row.trade_count, 0);
    const net = (this.buySellRows() ?? []).reduce((sum, row) => sum + row.net, 0);
    return [
      { label: 'Clients in scope', value: page ? count.format(page.total) : '–' },
      { label: 'Combined account value', value: page ? money.format(page.total_value) : '–' },
      { label: 'Average account value', value: page?.total ? money.format(page.total_value / page.total) : '–' },
      { label: 'Traded value', value: money.format(traded) },
      { label: 'Trades', value: count.format(trades) },
      { label: 'Net buying (buy − sell)', value: money.format(net) },
    ];
  });

  // ---------- charts ----------
  protected readonly volumeChart = computed(() => {
    const range = this.dates.range();
    if (!range) return null;
    if (this.anyPicked()) {
      const rows = this.byClientRows();
      if (!rows) return null;
      // one line per picked client, in the client's own colour
      const series = this.picked().map((client) => ({
        label: client.name,
        color: charts.clientColor(client.slot),
        values: rows.filter((row) => row.client_id === client.id).map((row) => row.notional),
      }));
      const periods = rows.filter((row) => row.client_id === this.picked()[0].id);
      return charts.periodLines(periods, range, series);
    }
    const rows = this.volumeRows();
    return rows ? charts.periodLine(rows, range, {
      label: 'Traded value', value: (row) => row.notional,
      afterLabel: (row) => ` Trades: ${count.format(row.trade_count)}`,
    }) : null;
  });

  protected readonly buySellChart = computed(() => {
    const rows = this.buySellRows(), range = this.dates.range();
    return rows && range ? charts.buySell(rows, range) : null;
  });

  protected readonly classChart = computed(() => {
    const rows = this.classRows();
    return rows?.length ? charts.assetClasses(rows) : null;
  });

  protected readonly histogram = computed(() => {
    const rows = this.bandRows();
    return rows?.length ? charts.valueHistogram(rows) : null;
  });

  /** The picked clients' rows, in the order they were picked; a client with no accounts has none. */
  private inPickOrder<T extends { client_id: number }>(rows: T[]): (T & { slot: number })[] {
    return this.picked().flatMap((client) => {
      const row = rows.find((one) => one.client_id === client.id);
      return row ? [{ ...row, slot: client.slot }] : [];
    });
  }

  protected readonly mixChart = computed(() => {
    const rows = this.inPickOrder(this.portfolioPage()?.rows ?? []);
    return rows.length ? charts.portfolioMix(rows, {
      label: (row) => row.client_name, cash: (row) => row.cash, invested: (row) => row.invested,
    }) : null;
  });

  protected readonly perClientChart = computed(() => {
    const rows = this.inPickOrder(this.perClientRows() ?? []);
    return rows.length ? charts.ranking(rows, {
      label: (row) => row.client_name, value: (row) => row.notional,
      color: (row) => charts.clientColor(row.slot),
      afterLabel: (row) => ` ${count.format(row.trade_count)} trades`,
    }) : null;
  });

  // ---------- filter actions ----------
  protected isBand(band: (typeof VALUE_BANDS)[number]): boolean {
    return band.min === this.minValue() && band.max === this.maxValue();
  }

  protected pickBand(band: (typeof VALUE_BANDS)[number]): void {
    this.badAmount.set(false);
    this.minValue.set(band.min);
    this.maxValue.set(band.max);
  }

  protected shown(amount: number | null): string {
    return amount === null ? '' : money.format(amount);
  }

  /** Reads "1m", "250k", "2.5b" or "1,000,000"; an empty box means no limit. */
  protected setAmount(target: WritableSignal<number | null>, event: Event): void {
    const text = (event.target as HTMLInputElement).value.trim().toLowerCase().replace(/[$,\s]/g, '');
    if (text === '') {
      this.badAmount.set(false);
      target.set(null);
      return;
    }
    const match = text.match(/^(\d+(?:\.\d+)?)([kmb]?)$/);
    this.badAmount.set(!match);
    if (match) {
      target.set(Number(match[1]) * { '': 1, k: 1e3, m: 1e6, b: 1e9 }[match[2] as '' | 'k' | 'm' | 'b']);
    }
  }


  protected clear(): void {
    this.picked.set([]);
    this.pickBand(VALUE_BANDS[0]);
    this.dates.lastMonths(12);
  }
}
