import { Component, computed, inject, linkedSignal, signal } from '@angular/core';

import { addDays, count, dateTime, moneyExact } from '../core/format';
import { ClientMatch, InstrumentMatch, OrderRow, OrderStatus, OrdersPage, Side, StatusStep } from '../core/models';
import { latest, problem, report } from '../core/report';
import { StatusService } from '../core/status.service';
import { DateRangeComponent } from '../shared/date-range';
import { SearchBoxComponent } from '../shared/search-box';

type SortKey = 'id' | 'time' | 'status' | 'ticker' | 'quantity' | 'price' | 'value';

/** Every order on the platform, filtered, sorted and paged. */
@Component({
  selector: 'app-admin',
  imports: [DateRangeComponent, SearchBoxComponent],
  templateUrl: './admin.html',
  styleUrl: './admin.css',
})
export class AdminPage {
  protected readonly statuses: OrderStatus[] = ['PENDING', 'ACCEPTED', 'FULFILLED', 'REJECTED'];
  protected readonly pageSizes = [10, 25, 50];

  // ---------- filters (empty = not applied) ----------
  protected readonly tradeId = signal('');
  protected readonly clientId = signal<number | null>(null);
  protected readonly instrumentId = signal<number | null>(null);
  protected readonly assetClass = signal<string | null>(null);
  protected readonly status = signal<OrderStatus[]>([]);
  protected readonly side = signal<Side | null>(null);
  protected readonly start = signal('');
  protected readonly end = signal('');           // inclusive, like the To box
  protected readonly minValue = signal('');
  protected readonly maxValue = signal('');

  // ---------- table ----------
  protected readonly sort = signal<SortKey>('time');
  protected readonly descending = signal(true);
  protected readonly pageSize = signal(inject(StatusService).maxRows());

  private readonly filters = computed(() => ({
    trade_id: this.tradeId(),
    client_id: this.clientId(),
    instrument_id: this.instrumentId(),
    asset_class: this.assetClass(),
    status: this.status(),
    side: this.side(),
    start: this.start(),
    end: this.end() ? addDays(this.end(), 1) : '',
    min_value: this.minValue(),
    max_value: this.maxValue(),
  }));

  /** Back to page 1 whenever the filters, sort or page size change. */
  protected readonly page = linkedSignal({
    source: () => [this.filters(), this.sort(), this.descending(), this.pageSize()],
    computation: () => 1,
  });

  protected readonly classes = report<string[]>(() => '/api/asset-classes');
  private readonly orders = report<OrdersPage>(() => '/api/admin/orders', () => ({
    ...this.filters(),
    sort: this.sort(),
    order: this.descending() ? 'desc' : 'asc',
    page: this.page(),
    page_size: this.pageSize(),
  }));
  protected readonly result = latest(this.orders);
  protected readonly loading = computed(() => this.orders.isLoading());
  protected readonly problem = problem(this.orders);

  protected readonly shown = computed(() => {
    const result = this.result();
    if (!result || result.total === 0) return 'No orders match these filters.';
    const first = (result.page - 1) * result.page_size + 1;
    const last = first + result.rows.length - 1;
    return `Showing ${count.format(first)}–${count.format(last)} of ${count.format(result.total)} orders`;
  });

  // ---------- one order's status timeline ----------
  protected readonly expanded = signal<number | null>(null);
  protected readonly history = report<StatusStep[]>(
    () => (this.expanded() === null ? undefined : `/api/admin/orders/${this.expanded()}/history`));

  // ---------- search boxes ----------
  protected readonly describeClient = (client: ClientMatch) => `${client.name} · ${client.email} (#${client.client_id})`;
  protected readonly describeInstrument = (instrument: InstrumentMatch) =>
    `${instrument.ticker} · ${instrument.name} · ${instrument.asset_class} (#${instrument.instrument_id})`;
  protected readonly instrumentParams = computed(() => ({ asset_class: this.assetClass() }));

  protected readonly count = count;
  protected readonly moneyExact = moneyExact;
  protected readonly dateTime = dateTime;

  protected sortBy(key: SortKey): void {
    if (this.sort() === key) {
      this.descending.update((descending) => !descending);
    } else {
      this.sort.set(key);
      this.descending.set(key !== 'ticker' && key !== 'status');   // text sorts A→Z first
    }
  }

  protected sortMark(key: SortKey): string {
    return this.sort() === key ? (this.descending() ? ' ▼' : ' ▲') : '';
  }

  protected ariaSort(key: SortKey): string {
    return this.sort() === key ? (this.descending() ? 'descending' : 'ascending') : 'none';
  }

  protected toggleStatus(status: OrderStatus): void {
    this.status.update((picked) =>
      picked.includes(status) ? picked.filter((s) => s !== status) : [...picked, status]);
  }

  protected toggleRow(row: OrderRow): void {
    this.expanded.update((id) => (id === row.trade_id ? null : row.trade_id));
  }

  protected pickClass(value: string): void {
    this.assetClass.set(value || null);
    this.instrumentId.set(null);   // a picked instrument may not be in the new class
  }

  protected value(event: Event): string {
    return (event.target as HTMLInputElement).value.trim();
  }

  protected reset(): void {
    this.tradeId.set('');
    this.clientId.set(null);
    this.instrumentId.set(null);
    this.assetClass.set(null);
    this.status.set([]);
    this.side.set(null);
    this.start.set('');
    this.end.set('');
    this.minValue.set('');
    this.maxValue.set('');
  }
}
