import { Injectable, computed, inject } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { InstrumentQuote } from '../models/account.models';
import { AuthService } from './auth.service';

// Every tradable instrument with its current price, from GET /v1/instruments. The backend reads prices from
// Redis first and only asks Fauxnance (in batches) for the ones it doesn't have cached.
@Injectable({ providedIn: 'root' })
export class InstrumentService {
  private auth = inject(AuthService);

  readonly instruments = httpResource<InstrumentQuote[]>(() => (this.auth.isLoggedIn() ? '/v1/instruments' : undefined));

  // instruments by ticker, e.g. to find the id of a holding (holdings come back with a ticker, not an id)
  readonly byTicker = computed(() => new Map((this.instruments.value() ?? []).map((instrument) => [instrument.ticker, instrument])));
}
