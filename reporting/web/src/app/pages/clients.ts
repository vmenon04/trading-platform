import { Component, computed, signal } from '@angular/core';

import { count, money } from '../core/format';
import { AssetClassRow, BuySellRow, ClientMatch, TopClientRow, TradeRow, VolumeRow } from '../core/models';
import { dateRange, latest, problem, report } from '../core/report';
import { ChartComponent } from '../shared/chart';
import * as charts from '../shared/charts';
import { DateRangeComponent } from '../shared/date-range';
import { SearchBoxComponent } from '../shared/search-box';
import { Tile, TilesComponent } from '../shared/tiles';
import { TopNComponent } from '../shared/top-n';
import { TradesTableComponent } from '../shared/trades-table';

/** Every chart for all clients, or for the one picked in the search box. */
@Component({
  selector: 'app-clients',
  imports: [ChartComponent, DateRangeComponent, SearchBoxComponent, TilesComponent, TopNComponent, TradesTableComponent],
  template: `
    <h2>Clients</h2>
    <p class="lead">Trading by client: everyone, or one client picked below.</p>

    <form class="filters" (submit)="$event.preventDefault()">
      <app-search-box label="Client" placeholder="All clients — search name, email or ID" url="/api/clients"
                      [describe]="describe" [(selected)]="clientId" />
      <app-date-range [(start)]="dates.start" [(end)]="dates.end" [(grain)]="dates.grain" />
      @if (clientId() === null) {
        <app-top-n [(value)]="topN" label="Top clients" />
      }
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
      <figure class="card">
        <figcaption>Traded value by asset class</figcaption>
        <app-chart [config]="classChart()" />
      </figure>
      @if (clientId() === null) {
        <figure class="card">
          <figcaption>Top {{ clientCount() }} clients by traded value</figcaption>
          <app-chart [config]="clientChart()" [height]="rankingHeight(clientCount())" />
        </figure>
      } @else {
        <figure class="card wide">
          <figcaption>Latest {{ tradeRows().length }} trades</figcaption>
          <app-trades-table [rows]="tradeRows()" />
        </figure>
      }
    </main>
  `,
})
export class ClientsPage {
  protected readonly dates = dateRange();
  protected readonly clientId = signal<number | null>(null);
  protected readonly topN = signal(10);
  protected readonly describe = (client: ClientMatch) => `${client.name} · ${client.email} (#${client.client_id})`;

  private readonly params = computed(() => {
    const range = this.dates.range();
    return range && { ...range, client_id: this.clientId() };
  });

  private readonly volume = report<VolumeRow[]>(() => '/api/analyst/volume', this.params);
  private readonly buySell = report<BuySellRow[]>(() => '/api/analyst/buy-sell', this.params);
  private readonly byClass = report<AssetClassRow[]>(() => '/api/analyst/by-asset-class', this.params);
  private readonly topClients = report<TopClientRow[]>(
    () => (this.clientId() === null ? '/api/analyst/top-clients' : undefined),
    () => this.params() && { ...this.params(), limit: this.topN() });
  private readonly trades = report<TradeRow[]>(
    () => (this.clientId() === null ? undefined : '/api/analyst/trades'),
    () => ({ client_id: this.clientId(), limit: 50 }));

  protected readonly problem = problem(this.volume, this.buySell, this.byClass);

  private readonly volumeRows = latest(this.volume);
  private readonly buySellRows = latest(this.buySell);
  private readonly classRows = latest(this.byClass);
  private readonly clientRows = latest(this.topClients);
  protected readonly tradeRows = computed(() => (this.trades.hasValue() ? this.trades.value() : []));

  protected readonly tiles = computed<Tile[]>(() => tradingTiles(this.volumeRows() ?? [], this.buySellRows() ?? []));

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
  protected readonly classChart = computed(() => {
    const rows = this.classRows();
    return rows ? charts.assetClasses(rows) : null;
  });
  protected readonly clientChart = computed(() => {
    const rows = this.clientRows();
    return rows ? charts.ranking(rows, {
      label: (row) => row.client_name, value: (row) => row.notional,
      afterLabel: (row) => ` ${count.format(row.trade_count)} trades`,
    }) : null;
  });

  protected readonly clientCount = computed(() => this.clientRows()?.length ?? 0);
  protected readonly rankingHeight = charts.rankingHeight;
}

/** The four headline numbers the clients and instruments views share. */
export function tradingTiles(volume: VolumeRow[], buySell: BuySellRow[]): Tile[] {
  const total = volume.reduce((sum, row) => sum + row.notional, 0);
  const trades = volume.reduce((sum, row) => sum + row.trade_count, 0);
  const net = buySell.reduce((sum, row) => sum + row.net, 0);
  return [
    { label: 'Traded value', value: money.format(total) },
    { label: 'Trades (accepted or fulfilled)', value: count.format(trades) },
    { label: 'Average trade', value: trades ? money.format(total / trades) : '–' },
    { label: 'Net buying (buy − sell)', value: money.format(net) },
  ];
}
