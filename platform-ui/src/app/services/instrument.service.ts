import { Injectable, computed, inject, linkedSignal, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { InstrumentQuote } from '../models/account.models';
import { AuthService } from './auth.service';

// the buy list loads this many instruments at a time; the backend prices only the ones asked for
export const PAGE_SIZE = 20;

// The instrument catalogue from GET /v1/instruments, with current prices: the instruments whose ticker or name
// contains `query` (all of them when it's blank), optionally of one asset class, PAGE_SIZE at a time. loadMore() adds
// the next PAGE_SIZE to the list. search() looks up a symbol that isn't in the database yet (backend: Redis, then
// Fauxnance, then adds it).
@Injectable({ providedIn: 'root' })
export class InstrumentService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  private readonly searchText = signal('');
  private readonly classFilter = signal<string | null>(null);
  private readonly start = signal(0);

  readonly query = this.searchText.asReadonly();
  // STOCK, ETF, FOREX or CRYPTO, or null for every class
  readonly assetClass = this.classFilter.asReadonly();
  // where the batch being loaded starts in the matches
  readonly offset = this.start.asReadonly();

  // a new query or batch cancels the request for the previous one, so an older, slower answer can't replace a newer one
  private readonly page = httpResource<InstrumentQuote[]>(() => {
    if (!this.auth.isLoggedIn()) return undefined;
    const params: Record<string, string | number> = { q: this.searchText(), limit: PAGE_SIZE, offset: this.start() };
    const assetClass = this.classFilter();
    if (assetClass) params['assetClass'] = assetClass;
    return { url: '/v1/instruments', params };
  });

  // every batch loaded so far for the current search: the first batch replaces the list, later ones are added to it
  // (skipping any already there); while a batch loads, the list loaded so far stays
  readonly instruments = linkedSignal<{ batch: InstrumentQuote[] | undefined; start: number }, InstrumentQuote[]>({
    source: () => ({ batch: this.page.value(), start: this.start() }),
    computation: ({ batch, start }, previous) => {
      const loaded = previous?.value ?? [];
      if (!batch) return start === 0 ? [] : loaded;
      if (start === 0) return batch;
      const known = new Set(loaded.map((quote) => quote.instrumentId));
      return [...loaded, ...batch.filter((quote) => !known.has(quote.instrumentId))];
    },
  });
  readonly isLoading = computed(() => this.page.isLoading());
  readonly error = computed(() => this.page.error());

  // how many instruments match the query across every batch (the X-Total-Count header); the last count is kept while
  // the next batch loads
  private readonly count = linkedSignal<string | null | undefined, number>({
    source: () => this.page.headers()?.get('X-Total-Count'),
    computation: (count, previous) => (count == null ? previous?.value ?? 0 : Number(count)),
  });
  readonly total = this.count.asReadonly();
  readonly hasMore = computed(() => this.instruments().length < this.total());

  // a new search starts again from the first batch
  setQuery(query: string): void {
    this.searchText.set(query.trim());
    this.start.set(0);
  }

  setAssetClass(assetClass: string | null): void {
    this.classFilter.set(assetClass);
    this.start.set(0);
  }

  loadMore(): void {
    if (this.hasMore() && !this.isLoading()) {
      this.start.set(this.instruments().length);
    }
  }

  // GET /v1/instruments?search= -> the instrument, or null if the symbol is unknown. A symbol that's found is now in
  // the database, so the list is loaded again from the start to include it
  async search(symbol: string): Promise<InstrumentQuote | null> {
    const results = await firstValueFrom(
      this.http.get<InstrumentQuote[]>('/v1/instruments', { params: { search: symbol.trim() } }));
    const instrument = results[0] ?? null;
    if (instrument) {
      this.reloadFromStart();
    }
    return instrument;
  }

  // the first batch of every instrument again, with fresh prices
  refresh(): void {
    if (this.searchText() || this.classFilter()) {
      this.searchText.set('');
      this.classFilter.set(null);
      this.start.set(0);
    } else {
      this.reloadFromStart();
    }
  }

  private reloadFromStart(): void {
    if (this.start() > 0) {
      this.start.set(0);
    } else {
      this.page.reload();
    }
  }
}
