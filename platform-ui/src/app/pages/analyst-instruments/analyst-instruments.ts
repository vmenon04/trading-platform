import { Component, computed, signal } from '@angular/core';

import { ChartComponent } from '../../analytics/chart';
import * as charts from '../../analytics/charts';
import { DataFreshness } from '../../analytics/data-freshness';
import { DateRangeComponent } from '../../analytics/date-range';
import { count, money, moneyExact } from '../../analytics/format';
import {
  AssetClassRow, BuySellRow, InstrumentMatch, PriceRow, TopInstrumentRow, TradeRow, VolumeRow,
} from '../../analytics/models';
import { dateRange, latest, problem, report } from '../../analytics/report';
import { SearchBoxComponent } from '../../analytics/search-box';
import { Tile, TilesComponent } from '../../analytics/tiles';
import { TradesTableComponent } from '../../analytics/trades-table';
import { TickerLogo } from '../../components/ticker-logo/ticker-logo';

const PERIODS = [1, 3, 6, 12];

/**
 * Instruments: everything that was traded (optionally one asset class), with the most traded ones listed;
 * pick one, from the list or the search box, to open its own view: price, volume, latest trades.
 */
@Component({
  selector: 'app-analyst-instruments',
  imports: [ChartComponent, DataFreshness, DateRangeComponent, SearchBoxComponent, TickerLogo, TilesComponent, TradesTableComponent],
  templateUrl: './analyst-instruments.html',
  styleUrl: './analyst-instruments.css',
})
export class AnalystInstruments {
  protected readonly periods = PERIODS;
  protected readonly moneyExact = moneyExact;
  protected readonly count = count;

  // ---------- filters ----------
  protected readonly dates = dateRange();
  protected readonly assetClass = signal<string | null>(null);
  protected readonly instrumentId = signal<number | null>(null);
  /** What the search box shows when an instrument is opened from the list. */
  protected readonly pickedText = signal<string | null>(null);
  protected readonly one = computed(() => this.instrumentId() !== null);

  protected readonly searchParams = computed(() => ({ asset_class: this.assetClass() }));   // search within the class
  protected readonly describe = (instrument: InstrumentMatch) =>
    `${instrument.ticker} · ${instrument.name} · ${instrument.asset_class} (#${instrument.instrument_id})`;

  // ---------- requests ----------
  private readonly params = computed(() => {
    const range = this.dates.range();
    return range && { ...range, asset_class: this.assetClass(), instrument_id: this.instrumentId() };
  });
  private readonly ranked = () => this.params() && { ...this.params(), limit: 10 };

  protected readonly classes = report<string[]>(() => '/api/asset-classes');
  private readonly volume = report<VolumeRow[]>(() => '/api/analyst/volume', this.params);
  private readonly buySell = report<BuySellRow[]>(() => '/api/analyst/buy-sell', this.params);
  private readonly price = report<PriceRow[]>(() => (this.one() ? '/api/analyst/price' : undefined), this.params);
  private readonly byClass = report<AssetClassRow[]>(
    () => (this.one() || this.assetClass() ? undefined : '/api/analyst/by-asset-class'), this.params);
  // the most traded instruments; with an instrument picked, just that one (its name, class and totals)
  private readonly topInstruments = report<TopInstrumentRow[]>(() => '/api/analyst/top-instruments', this.ranked);
  private readonly trades = report<TradeRow[]>(
    () => (this.one() ? '/api/analyst/trades' : undefined), () => ({ instrument_id: this.instrumentId(), limit: 20 }));

  protected readonly problem = problem(this.volume, this.buySell, this.topInstruments);

  private readonly volumeRows = latest(this.volume);
  private readonly buySellRows = latest(this.buySell);
  private readonly priceRows = latest(this.price);
  private readonly classRows = latest(this.byClass);
  private readonly instrumentLatest = latest(this.topInstruments);
  protected readonly instrumentRows = computed(() => this.instrumentLatest() ?? []);
  protected readonly tradeRows = computed(() => (this.trades.hasValue() ? this.trades.value() : []));

  /** The opened instrument: from its own totals, or from its trades if it had none in the period. */
  protected readonly instrument = computed(() => {
    if (!this.one()) return null;
    const row = this.instrumentRows().find((one) => one.instrument_id === this.instrumentId());
    if (row) return { ticker: row.ticker, name: row.name, asset_class: row.asset_class, avg_price: row.avg_price as number | null };
    const trade = this.tradeRows()[0];
    return trade ? { ticker: trade.ticker, name: '', asset_class: trade.asset_class, avg_price: null } : null;
  });

  /** What the charts are about: one ticker, one class, or everything. */
  protected readonly scopeName = computed(() =>
    this.one() ? (this.instrument()?.ticker ?? 'this instrument') : (this.assetClass() ?? 'all instruments'));

  protected readonly tiles = computed<Tile[]>(() => {
    const volume = this.volumeRows() ?? [];
    const total = volume.reduce((sum, row) => sum + row.notional, 0);
    const trades = volume.reduce((sum, row) => sum + row.trade_count, 0);
    const net = (this.buySellRows() ?? []).reduce((sum, row) => sum + row.net, 0);
    const accounts = Math.max(0, ...volume.map((row) => row.active_accounts));
    return [
      { label: 'Traded value', value: money.format(total) },
      { label: 'Trades (accepted or fulfilled)', value: count.format(trades) },
      { label: 'Average trade', value: trades ? money.format(total / trades) : '–' },
      { label: 'Net buying (buy − sell)', value: money.format(net) },
      { label: 'Most accounts in one period', value: count.format(accounts) },
    ];
  });

  // ---------- charts ----------
  protected readonly volumeChart = computed(() => {
    const rows = this.volumeRows(), range = this.dates.range();
    return rows && range ? charts.periodLine(rows, range, {
      label: 'Traded value', value: (row) => row.notional,
      afterLabel: (row) => ` Trades: ${count.format(row.trade_count)}`,
    }) : null;
  });
  protected readonly buySellChart = computed(() => {
    const rows = this.buySellRows(), range = this.dates.range();
    return rows && range ? charts.buySell(rows, range) : null;
  });
  protected readonly priceChart = computed(() => {
    const rows = this.priceRows(), range = this.dates.range();
    return rows && range ? charts.periodLine(rows, range, {
      label: 'Average traded price', value: (row) => row.avg_price, format: moneyExact, beginAtZero: false,
      afterLabel: (row) => row.avg_price === null
        ? ' No trades'
        : ` Range: ${moneyExact.format(row.low_price!)} – ${moneyExact.format(row.high_price!)}`,
    }) : null;
  });
  protected readonly classChart = computed(() => {
    const rows = this.classRows();
    return rows?.length ? charts.assetClasses(rows) : null;
  });

  // ---------- actions ----------
  protected pickClass(name: string | null): void {
    this.assetClass.set(name);
    this.instrumentId.set(null);   // a picked instrument may not be in the new class
  }

  protected open(row: TopInstrumentRow): void {
    this.pickedText.set(null);   // so opening the same instrument twice still refills the box
    this.pickedText.set(this.describe(row));
    this.instrumentId.set(row.instrument_id);
  }

  protected back(): void {
    this.instrumentId.set(null);
  }
}
