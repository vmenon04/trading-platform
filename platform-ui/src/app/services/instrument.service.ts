import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { InstrumentQuote } from '../models/account.models';
import { AuthService } from './auth.service';

// the buy list shows at most this many instruments; the backend prices only these, so searching narrows it down
export const PAGE_SIZE = 50;

// A page of the instrument catalogue from GET /v1/instruments, with current prices: the instruments whose ticker or
// name contains `query` (all of them when it's blank). search() looks up a symbol that isn't in the database yet
// (backend: Redis, then Fauxnance, then adds it).
@Injectable({ providedIn: 'root' })
export class InstrumentService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  readonly query = signal('');

  // a new query cancels the request for the previous one, so an older, slower answer can't replace a newer one
  private readonly page = httpResource<InstrumentQuote[]>(() => (this.auth.isLoggedIn()
    ? { url: '/v1/instruments', params: { q: this.query().trim(), limit: PAGE_SIZE } }
    : undefined));

  readonly instruments = computed(() => this.page.value() ?? []);
  readonly isLoading = computed(() => this.page.isLoading());
  readonly error = computed(() => this.page.error());

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
    if (this.query()) {
      this.query.set('');
    } else {
      this.page.reload();
    }
  }
}
