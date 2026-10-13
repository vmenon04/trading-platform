import { Component, computed, signal } from '@angular/core';

import { count, moneyExact } from '../core/format';
import {
  AssetClassRow, BuySellRow, InstrumentMatch, PriceRow, TopClientRow, TopInstrumentRow, TradeRow, VolumeRow,
} from '../core/models';
import { dateRange, latest, problem, report } from '../core/report';
import { ChartComponent } from '../shared/chart';
import * as charts from '../shared/charts';
import { DateRangeComponent } from '../shared/date-range';
import { SearchBoxComponent } from '../shared/search-box';
import { TilesComponent } from '../shared/tiles';
import { TopNComponent } from '../shared/top-n';
import { TradesTableComponent } from '../shared/trades-table';
import { tradingTiles } from './clients';

/** Every chart for all instruments, one asset class, or one instrument. */
@Component({
  selector: 'app-instruments',
  imports: [ChartComponent, DateRangeComponent, SearchBoxComponent, TilesComponent, TopNComponent, TradesTableComponent],
  template: `
    <h2>Instruments</h2>
    <p class="lead">Trading by what was traded: everything, one asset class, or one instrument.</p>

    <form class="filters" (submit)="$event.preventDefault()">
      <label>Asset class
        <select (change)="pickClass($event)">
          <option value="" [selected]="!assetClass()">All classes</option>
          @for (name of classes.hasValue() ? classes.value() : []; track name) {
            <option [value]="name" [selected]="name === assetClass()">{{ name }}</option>
          }
        </select>
      </label>
      <app-search-box label="Instrument" placeholder="All instruments — search ticker or name" url="/api/instruments"
                      [params]="searchParams()" [describe]="describe" [(selected)]="instrumentId" />
      <app-date-range [(start)]="dates.start" [(end)]="dates.end" [(grain)]="dates.grain" />
      <app-top-n [(value)]="topN" label="Top N" />
    </form>

    @if (problem(); as message) { <p class="problem">{{ message }}</p> }

    <app-tiles [tiles]="tiles()" />

    <main class="grid">
      <figure class="card">
        <figcaption>Traded value over time</figcaption>
        <app-chart [config]="volumeChart()" />
      </figure>
      <figure class="card">
        <figcaption>Buy vs sell</figcaption>
        <app-chart [config]="buySellChart()" />
      </figure>
      @if (one()) {
        <figure class="card">
          <figcaption>Average traded price (volume-weighted)</figcaption>
          <app-chart [config]="priceChart()" />
        </figure>
      } @else {
        @if (!assetClass()) {
          <figure class="card">
            <figcaption>Traded value by asset class</figcaption>
            <app-chart [config]="classChart()" />
          </figure>
        }
        <figure class="card">
          <figcaption>Top {{ instrumentCount() }} {{ assetClass() ?? '' }} instruments by traded value</figcaption>
          <app-chart [config]="instrumentChart()" [height]="rankingHeight(instrumentCount())" />
        </figure>
      }
      <figure class="card">
        <figcaption>Top {{ clientCount() }} clients trading {{ scopeName() }}</figcaption>
        <app-chart [config]="clientChart()" [height]="rankingHeight(clientCount())" />
      </figure>
      @if (one()) {
        <figure class="card wide">
          <figcaption>Latest {{ tradeRows().length }} trades</figcaption>
          <app-trades-table [rows]="tradeRows()" />
        </figure>
      }
    </main>
  `,
})
export class InstrumentsPage {
  protected readonly dates = dateRange();
  protected readonly assetClass = signal<string | null>(null);
  protected readonly instrumentId = signal<number | null>(null);
  protected readonly topN = signal(10);
  protected readonly one = computed(() => this.instrumentId() !== null);

  protected readonly searchParams = computed(() => ({ asset_class: this.assetClass() }));   // search within the class
  protected readonly describe = (instrument: InstrumentMatch) =>
    `${instrument.ticker} · ${instrument.name} · ${instrument.asset_class} (#${instrument.instrument_id})`;

  private readonly params = computed(() => {
    const range = this.dates.range();
    return range && { ...range, asset_class: this.assetClass(), instrument_id: this.instrumentId() };
  });
  private readonly ranked = () => this.params() && { ...this.params(), limit: this.topN() };

  protected readonly classes = report<string[]>(() => '/api/asset-classes');
  private readonly volume = report<VolumeRow[]>(() => '/api/analyst/volume', this.params);
  private readonly buySell = report<BuySellRow[]>(() => '/api/analyst/buy-sell', this.params);
  private readonly price = report<PriceRow[]>(() => (this.one() ? '/api/analyst/price' : undefined), this.params);
  private readonly byClass = report<AssetClassRow[]>(
    () => (this.one() || this.assetClass() ? undefined : '/api/analyst/by-asset-class'), this.params);
  private readonly topInstruments = report<TopInstrumentRow[]>(
    () => (this.one() ? undefined : '/api/analyst/top-instruments'), this.ranked);
  private readonly topClients = report<TopClientRow[]>(() => '/api/analyst/top-clients', this.ranked);
  private readonly trades = report<TradeRow[]>(
    () => (this.one() ? '/api/analyst/trades' : undefined), () => ({ instrument_id: this.instrumentId(), limit: 50 }));

  protected readonly problem = problem(this.volume, this.buySell, this.topClients);

  private readonly volumeRows = latest(this.volume);
  private readonly buySellRows = latest(this.buySell);
  private readonly priceRows = latest(this.price);
  private readonly classRows = latest(this.byClass);
  private readonly instrumentRows = latest(this.topInstruments);
  private readonly clientRows = latest(this.topClients);
  protected readonly tradeRows = computed(() => (this.trades.hasValue() ? this.trades.value() : []));

  protected readonly tiles = computed(() => tradingTiles(this.volumeRows() ?? [], this.buySellRows() ?? []));

  /** What the top-clients chart is about: one ticker, one class, or everything. */
  protected readonly scopeName = computed(() => {
    const ticker = this.tradeRows()[0]?.ticker;
    return this.one() ? (ticker ?? 'this instrument') : (this.assetClass() ?? 'all instruments');
  });

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
    return rows ? charts.assetClasses(rows) : null;
  });
  protected readonly instrumentChart = computed(() => {
    const rows = this.instrumentRows();
    // a ticker is only unique within its class (CVX is a stock and a crypto)
    return rows ? charts.ranking(rows, {
      label: (row) => (this.assetClass() ? row.ticker : `${row.ticker} · ${row.asset_class}`),
      value: (row) => row.notional,
      afterLabel: (row) => ` ${row.name} · ${count.format(row.trade_count)} trades`,
    }) : null;
  });
  protected readonly clientChart = computed(() => {
    const rows = this.clientRows();
    return rows ? charts.ranking(rows, {
      label: (row) => row.client_name, value: (row) => row.notional,
      afterLabel: (row) => ` ${count.format(row.trade_count)} trades`,
    }) : null;
  });

  protected readonly instrumentCount = computed(() => this.instrumentRows()?.length ?? 0);
  protected readonly clientCount = computed(() => this.clientRows()?.length ?? 0);
  protected readonly rankingHeight = charts.rankingHeight;

  protected pickClass(event: Event): void {
    this.assetClass.set((event.target as HTMLSelectElement).value || null);
    this.instrumentId.set(null);   // a picked instrument may not be in the new class
  }
}
