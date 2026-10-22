import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { InstrumentQuote } from '../models/account.models';
import { AuthService } from './auth.service';

// The instrument catalogue from GET /v1/instruments: every instrument the backend has in Redis, with current
// prices. search() looks up a symbol that isn't in it yet (backend: Redis, then Fauxnance, then caches it).
@Injectable({ providedIn: 'root' })
export class InstrumentService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  private readonly catalogue = httpResource<InstrumentQuote[]>(() => (this.auth.isLoggedIn() ? '/v1/instruments' : undefined));

  // symbols found by search since the catalogue was loaded
  private readonly found = signal<InstrumentQuote[]>([]);

  readonly isLoading = computed(() => this.catalogue.isLoading());
  readonly error = computed(() => this.catalogue.error());

  // catalogue plus searched symbols, one entry per ticker, sorted by ticker
  readonly instruments = computed(() => {
    const byTicker = new Map((this.catalogue.value() ?? []).map((instrument) => [instrument.ticker, instrument]));
    for (const instrument of this.found()) {
      byTicker.set(instrument.ticker, instrument);
    }
    return [...byTicker.values()].sort((a, b) => a.ticker.localeCompare(b.ticker));
  });

  // GET /v1/instruments?search= -> the instrument, or null if the symbol is unknown
  async search(symbol: string): Promise<InstrumentQuote | null> {
    const results = await firstValueFrom(
      this.http.get<InstrumentQuote[]>('/v1/instruments', { params: { search: symbol.trim() } }));
    const instrument = results[0] ?? null;
    if (instrument) {
      this.found.update((found) => [...found.filter((other) => other.ticker !== instrument.ticker), instrument]);
    }
    return instrument;
  }

  refresh(): void {
    this.catalogue.reload();
  }
}
