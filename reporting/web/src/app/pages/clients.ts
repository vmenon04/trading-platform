import { Component, computed, signal, WritableSignal } from '@angular/core';

import { count, money, moneyExact } from '../core/format';
import {
  AssetClassRow, BuySellRow, ClientMatch, PortfolioRow, PortfoliosPage, PortfolioTierRow, Quartile, TopClientRow,
  TradeRow, VolumeRow,
} from '../core/models';
import { dateRange, latest, problem, report } from '../core/report';
import { ChartComponent } from '../shared/chart';
import * as charts from '../shared/charts';
import { DateRangeComponent } from '../shared/date-range';
import { SearchBoxComponent } from '../shared/search-box';
import { Tile, TilesComponent } from '../shared/tiles';
import { TopNComponent } from '../shared/top-n';
import { TradesTableComponent } from '../shared/trades-table';

/**
 * Clients by what they hold (portfolio value, filterable to find high-net-worth clients)
 * and by what they trade, for everyone or the one client picked.
 */
@Component({
  selector: 'app-clients',
  imports: [ChartComponent, DateRangeComponent, SearchBoxComponent, TilesComponent, TopNComponent, TradesTableComponent],
  template: `
    <h2>Clients</h2>
    <p class="lead">Who holds the most, and how clients trade: everyone, or one client picked below.</p>

    <form class="filters" (submit)="$event.preventDefault()">
      <app-search-box label="Client" placeholder="All clients — search name, email or ID" url="/api/clients"
                      [describe]="describe" [pickedText]="pickedText()" [(selected)]="clientId" />
    </form>

    @if (problem(); as message) { <p class="problem">{{ message }}</p> }

    @if (clientId() === null) {
      <h3>Portfolio value</h3>
      <p class="lead">Holdings at their last traded price plus cash, as of today. Filter to find high-net-worth clients.</p>

      <form class="filters" (submit)="$event.preventDefault()">
        <label>Min value
          <input type="text" inputmode="decimal" placeholder="e.g. 1m or 250k" [value]="shown(minValue())"
                 (change)="setAmount(minValue, $event)">
        </label>
        <label>Max value
          <input type="text" inputmode="decimal" placeholder="no limit" [value]="shown(maxValue())"
                 (change)="setAmount(maxValue, $event)">
        </label>
        <label>Quartile
          <select (change)="pickQuartile($event)">
            <option value="" [selected]="quartile() === null">All clients</option>
            @for (q of quartiles; track q) {
              <option [value]="q" [selected]="q === quartile()">{{ quartileLabels[q] }}</option>
            }
          </select>
        </label>
        @if (filtered()) {
          <button type="button" (click)="clearPortfolioFilters()">Clear</button>
        }
      </form>
      @if (badAmount()) { <p class="problem">Enter amounts like 1000000, 1m, 250k or 2.5b.</p> }

      <app-tiles [tiles]="portfolioSummary()" />

      <main class="grid">
        <figure class="card wide">
          <figcaption>Clients by portfolio value — click a client to see their trading</figcaption>
          <div class="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Client</th><th class="num">Portfolio value</th><th class="num">Invested</th>
                  <th class="num">Cash</th><th>Quartile</th>
                </tr>
              </thead>
              <tbody>
                @for (row of portfolioPage()?.rows ?? []; track row.client_id) {
                  <tr class="pick" tabindex="0" (click)="pick(row)" (keydown.enter)="pick(row)">
                    <td>{{ row.client_name }} <small>#{{ row.client_id }}</small></td>
                    <td class="num">{{ moneyExact.format(row.portfolio_value) }}</td>
                    <td class="num">
                      {{ moneyExact.format(row.invested) }}
                      @if (row.unpriced_positions) { <small title="Holdings never traded here, counted as $0">({{ row.unpriced_positions }} unpriced)</small> }
                    </td>
                    <td class="num">{{ moneyExact.format(row.cash) }}</td>
                    <td>{{ quartileLabels[row.quartile] }}</td>
                  </tr>
                } @empty {
                  <tr><td colspan="5" class="empty">No clients match these filters.</td></tr>
                }
              </tbody>
            </table>
          </div>
          <nav class="pager" aria-label="Pages">
            <button type="button" [disabled]="page() <= 1" (click)="page.set(page() - 1)">‹ Previous</button>
            <span>Page {{ page() }} of {{ portfolioPage()?.pages ?? 1 }}</span>
            <button type="button" [disabled]="page() >= (portfolioPage()?.pages ?? 1)" (click)="page.set(page() + 1)">Next ›</button>
          </nav>
        </figure>
        <figure class="card">
          <figcaption>Portfolio value by quartile (all clients)</figcaption>
          <app-chart [config]="tierChart()" />
        </figure>
      </main>
    } @else {
      <h3>Portfolio</h3>
      <app-tiles [tiles]="portfolioTiles()" />
    }

    <h3>Trading</h3>
    <form class="filters" (submit)="$event.preventDefault()">
      <app-date-range [(start)]="dates.start" [(end)]="dates.end" [(grain)]="dates.grain" />
      @if (clientId() === null) {
        <app-top-n [(value)]="topN" label="Top clients" />
      }
    </form>

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
  styles: `
    h3 { margin: 28px 0 4px; font-size: 16px; }
    tr.pick { cursor: pointer; }
    tr.pick:hover, tr.pick:focus { background: var(--hover); outline: none; }
    small { color: var(--muted); }
    .pager { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; margin-top: 12px; }
  `,
})
export class ClientsPage {
  protected readonly dates = dateRange();
  protected readonly clientId = signal<number | null>(null);
  protected readonly topN = signal(10);
  protected readonly describe = (client: ClientMatch) => `${client.name} · ${client.email} (#${client.client_id})`;
  /** What the search box shows when a client is picked from the portfolio table. */
  protected readonly pickedText = signal<string | null>(null);

  // --- portfolio value ------------------------------------------------------

  protected readonly minValue = signal<number | null>(null);
  protected readonly maxValue = signal<number | null>(null);
  protected readonly quartile = signal<Quartile | null>(null);
  protected readonly page = signal(1);
  protected readonly badAmount = signal(false);
  protected readonly quartiles: Quartile[] = [4, 3, 2, 1];
  protected readonly quartileLabels = QUARTILE_LABELS;
  protected readonly moneyExact = moneyExact;

  protected readonly filtered = computed(
    () => this.minValue() !== null || this.maxValue() !== null || this.quartile() !== null);

  // Portfolios are "right now", so they ignore the date range.
  private readonly portfolios = report<PortfoliosPage>(
    () => (this.clientId() === null ? '/api/analyst/portfolios' : undefined),
    () => ({
      min_value: this.minValue(), max_value: this.maxValue(), quartile: this.quartile(), page: this.page(),
    }));
  private readonly onePortfolio = report<PortfoliosPage>(
    () => (this.clientId() === null ? undefined : '/api/analyst/portfolios'),
    () => ({ client_id: this.clientId(), page_size: 1 }));
  private readonly tiers = report<PortfolioTierRow[]>(
    () => (this.clientId() === null ? '/api/analyst/portfolio-tiers' : undefined));

  protected readonly portfolioPage = latest(this.portfolios);
  private readonly tierRows = latest(this.tiers);

  protected readonly portfolioSummary = computed<Tile[]>(() => {
    const result = this.portfolioPage();
    return [
      { label: this.filtered() ? 'Clients matching' : 'Clients with accounts', value: result ? count.format(result.total) : '–' },
      { label: 'Combined portfolio value', value: result ? money.format(result.total_value) : '–' },
      { label: 'Average portfolio', value: result?.total ? money.format(result.total_value / result.total) : '–' },
    ];
  });

  protected readonly tierChart = computed(() => {
    const rows = this.tierRows();
    return rows ? charts.ranking(rows, {
      measure: 'Portfolio value',
      label: (row) => QUARTILE_LABELS[row.quartile], value: (row) => row.total_value,
      afterLabel: (row) => ` ${count.format(row.clients)} clients · ${money.format(row.min_value)}–${money.format(row.max_value)} each`,
    }) : null;
  });

  /** The picked client's portfolio, or a dash per tile while it loads (or if they have no accounts). */
  protected readonly portfolioTiles = computed<Tile[]>(() => {
    const row = this.onePortfolio.hasValue() ? this.onePortfolio.value()?.rows[0] : undefined;
    const unpriced = row?.unpriced_positions ? ` (${row.unpriced_positions} unpriced)` : '';
    return [
      { label: 'Portfolio value', value: row ? money.format(row.portfolio_value) : '–' },
      { label: 'Invested' + unpriced, value: row ? money.format(row.invested) : '–' },
      { label: 'Cash', value: row ? money.format(row.cash) : '–' },
      { label: 'Quartile', value: row ? QUARTILE_LABELS[row.quartile] : '–' },
    ];
  });

  protected shown(amount: number | null): string {
    return amount === null ? '' : String(amount);
  }

  /** Reads "1m", "250k", "2.5b" or "1,000,000"; an empty box means no limit. */
  protected setAmount(target: WritableSignal<number | null>, event: Event): void {
    const text = (event.target as HTMLInputElement).value.trim().toLowerCase().replace(/[$,\s]/g, '');
    if (text === '') {
      this.badAmount.set(false);
      target.set(null);
    } else {
      const match = text.match(/^(\d+(?:\.\d+)?)([kmb]?)$/);
      this.badAmount.set(!match);
      if (!match) return;
      const scale = { '': 1, k: 1e3, m: 1e6, b: 1e9 }[match[2] as '' | 'k' | 'm' | 'b'];
      target.set(Number(match[1]) * scale);
    }
    this.page.set(1);
  }

  protected pickQuartile(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.quartile.set(value ? (Number(value) as Quartile) : null);
    this.page.set(1);
  }

  protected clearPortfolioFilters(): void {
    this.minValue.set(null);
    this.maxValue.set(null);
    this.quartile.set(null);
    this.badAmount.set(false);
    this.page.set(1);
  }

  protected pick(row: PortfolioRow): void {
    this.pickedText.set(null); // so picking the same client twice still refills the box
    this.pickedText.set(`${row.client_name} (#${row.client_id})`);
    this.clientId.set(row.client_id);
  }

  // --- trading ----------------------------------------------------------------

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

  protected readonly problem = problem(
    this.volume, this.buySell, this.byClass, this.portfolios, this.onePortfolio, this.tiers);

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

const QUARTILE_LABELS: Record<Quartile, string> = {
  4: 'Q4 · top 25%',
  3: 'Q3 · upper middle',
  2: 'Q2 · lower middle',
  1: 'Q1 · bottom 25%',
};

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
