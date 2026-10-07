import { Component, computed, signal } from '@angular/core';

import { count, money, percent } from '../core/format';
import {
  AssetClassRow, BuySellRow, StatusMixRow, TopClientRow, TopInstrumentRow, VolumeRow,
} from '../core/models';
import { dateRange, latest, problem, report } from '../core/report';
import { ChartComponent } from '../shared/chart';
import * as charts from '../shared/charts';
import { DateRangeComponent } from '../shared/date-range';
import { Tile, TilesComponent } from '../shared/tiles';
import { TopNComponent } from '../shared/top-n';

/** Platform performance over time: how much is traded, and how orders turn out. */
@Component({
  selector: 'app-analyst',
  imports: [ChartComponent, DateRangeComponent, TilesComponent, TopNComponent],
  templateUrl: './analyst.html',
})
export class AnalystPage {
  protected readonly dates = dateRange();
  protected readonly assetClass = signal<string | null>(null);
  protected readonly topN = signal(10);

  private readonly params = computed(() => {
    const range = this.dates.range();
    return range && { ...range, asset_class: this.assetClass() };
  });

  protected readonly classes = report<string[]>(() => '/api/asset-classes');
  private readonly volume = report<VolumeRow[]>(() => '/api/analyst/volume', this.params);
  private readonly buySell = report<BuySellRow[]>(() => '/api/analyst/buy-sell', this.params);
  private readonly mix = report<StatusMixRow[]>(() => '/api/analyst/status-mix', this.params);
  private readonly byClass = report<AssetClassRow[]>(
    () => (this.assetClass() ? undefined : '/api/analyst/by-asset-class'), this.params);
  private readonly topInstruments = report<TopInstrumentRow[]>(
    () => '/api/analyst/top-instruments', () => this.params() && { ...this.params(), limit: this.topN() });
  private readonly topClients = report<TopClientRow[]>(
    () => '/api/analyst/top-clients', () => this.params() && { ...this.params(), limit: this.topN() });

  protected readonly problem = problem(this.volume, this.buySell, this.mix, this.topInstruments, this.topClients);

  private readonly volumeRows = latest(this.volume);
  private readonly buySellRows = latest(this.buySell);
  private readonly mixRows = latest(this.mix);
  private readonly classRows = latest(this.byClass);
  private readonly instrumentRows = latest(this.topInstruments);
  private readonly clientRows = latest(this.topClients);

  protected readonly tiles = computed<Tile[]>(() => {
    const volume = this.volumeRows() ?? [];
    const mix = this.mixRows() ?? [];
    const buySell = this.buySellRows() ?? [];
    const sum = <T>(rows: T[], value: (row: T) => number) => rows.reduce((total, row) => total + value(row), 0);

    const placed = sum(mix, (row) => row.orders);
    const rate = (status: 'FULFILLED' | 'REJECTED') =>
      placed ? `${percent.format((sum(mix, (row) => row[status]) / placed) * 100)}%` : '–';
    return [
      { label: 'Traded value (accepted or fulfilled)', value: money.format(sum(volume, (row) => row.notional)) },
      { label: 'Orders placed (every status)', value: count.format(placed) },
      { label: 'Fill rate', value: rate('FULFILLED') },
      { label: 'Rejection rate', value: rate('REJECTED') },
      { label: 'Net buying (buy − sell)', value: money.format(sum(buySell, (row) => row.net)) },
    ];
  });

  // ---------- charts ----------

  protected readonly volumeChart = computed(() => {
    const rows = this.volumeRows(), range = this.dates.range();
    return rows && range ? charts.periodLine(rows, range, {
      label: 'Traded value',
      value: (row) => row.notional,
      afterLabel: (row) => ` Trades: ${count.format(row.trade_count)} · active accounts: ${count.format(row.active_accounts)}`,
    }) : null;
  });

  protected readonly ratesChart = computed(() => {
    const rows = this.mixRows(), range = this.dates.range();
    return rows && range ? charts.rates(rows, range) : null;
  });

  protected readonly outcomesChart = computed(() => {
    const rows = this.mixRows(), range = this.dates.range();
    return rows && range ? charts.outcomes(rows, range) : null;
  });

  protected readonly buySellChart = computed(() => {
    const rows = this.buySellRows(), range = this.dates.range();
    return rows && range ? charts.buySell(rows, range) : null;
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
      label: (row) => row.client_name,
      value: (row) => row.notional,
      afterLabel: (row) => ` ${count.format(row.trade_count)} trades`,
    }) : null;
  });

  protected readonly instrumentCount = computed(() => this.instrumentRows()?.length ?? 0);
  protected readonly clientCount = computed(() => this.clientRows()?.length ?? 0);
  protected readonly rankingHeight = charts.rankingHeight;

  protected pickClass(event: Event): void {
    this.assetClass.set((event.target as HTMLSelectElement).value || null);
  }
}
