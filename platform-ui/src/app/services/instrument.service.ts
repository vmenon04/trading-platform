import { Injectable, computed, inject, linkedSignal, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { InstrumentQuote } from '../models/account.models';
import { AuthService } from './auth.service';

// the buy list shows this many instruments a page; the backend prices only the page shown
export const PAGE_SIZE = 50;

// A page of the instrument catalogue from GET /v1/instruments, with current prices: the instruments whose ticker or
// name contains `query` (all of them when it's blank), PAGE_SIZE at a time. search() looks up a symbol that isn't in
// the database yet (backend: Redis, then Fauxnance, then adds it).
@Injectable({ providedIn: 'root' })
export class InstrumentService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  private readonly searchText = signal('');
  private readonly start = signal(0);

  readonly query = this.searchText.asReadonly();
  // how many matches come before this page
  readonly offset = this.start.asReadonly();

  // a new query or page cancels the request for the previous one, so an older, slower answer can't replace a newer one
  private readonly page = httpResource<InstrumentQuote[]>(() => (this.auth.isLoggedIn()
    ? { url: '/v1/instruments', params: { q: this.searchText(), limit: PAGE_SIZE, offset: this.start() } }
    : undefined));

  readonly instruments = computed(() => this.page.value() ?? []);
  readonly isLoading = computed(() => this.page.isLoading());
  readonly error = computed(() => this.page.error());

  // how many instruments match the query across every page (the X-Total-Count header); the last count is kept while
  // the next page loads, so the page controls don't disappear in between
  private readonly count = linkedSignal<string | null | undefined, number>({
    source: () => this.page.headers()?.get('X-Total-Count'),
    computation: (count, previous) => (count == null ? previous?.value ?? 0 : Number(count)),
  });
  readonly total = this.count.asReadonly();
  readonly hasPrevious = computed(() => this.start() > 0);
  readonly hasNext = computed(() => this.start() + PAGE_SIZE < this.total());

  // a new search starts again from its first page
  setQuery(query: string): void {
    this.searchText.set(query.trim());
    this.start.set(0);
  }

  nextPage(): void {
    if (this.hasNext()) {
      this.start.update((start) => start + PAGE_SIZE);
    }
  }

  previousPage(): void {
    this.start.update((start) => Math.max(0, start - PAGE_SIZE));
  }

  // GET /v1/instruments?search= -> the instrument, or null if the symbol is unknown. A symbol that's found is now in
  // the database, so the page is loaded again to include it
  async search(symbol: string): Promise<InstrumentQuote | null> {
    const results = await firstValueFrom(
      this.http.get<InstrumentQuote[]>('/v1/instruments', { params: { search: symbol.trim() } }));
    const instrument = results[0] ?? null;
    if (instrument) {
      this.page.reload();
    }
    return instrument;
  }

  // the first page again, with fresh prices
  refresh(): void {
    if (this.searchText() || this.start()) {
      this.setQuery('');
    } else {
      this.page.reload();
    }
  }
}
