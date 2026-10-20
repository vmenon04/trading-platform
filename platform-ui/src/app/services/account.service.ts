import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AccountHolding, AccountSummary, Trade, TradeSide, TradeRequest } from '../models/account.models';

// trades go through Kafka, so reload a few times after submitting until the new order has settled
const REFRESH_DELAYS_MS = [3000, 8000, 15000];

// Calls the Account & Trade Management Service. URLs are relative (/v1/...) and the
// dev server forwards them to Spring Boot via proxy.conf.json.
@Injectable({ providedIn: 'root' })
export class AccountService {
  private http = inject(HttpClient);

  // the account being viewed; every resource below re-fetches when it changes
  // TODO: set from login / AccountSelector instead of a sample-data account
  readonly accountId = signal('00000000-0000-0000-0000-000000000001');

  // null = all orders
  readonly orderSide = signal<TradeSide | null>(null);

  readonly summary = httpResource<AccountSummary>(() => `/v1/accounts/${this.accountId()}`);

  readonly holdings = httpResource<AccountHolding[]>(() => `/v1/accounts/${this.accountId()}/holdings`);

  readonly orders = httpResource<Trade[]>(() => {
    const side = this.orderSide();
    const params: Record<string, string> = side ? { side } : {};
    return { url: `/v1/accounts/${this.accountId()}/orders`, params };
  });

  // POST /v1/accounts/{accountId}/trades -> 202 Accepted with an empty body.
  // Resolves once the order is accepted; rejects with the HttpErrorResponse otherwise.
  async submitTrade(instrumentId: number, side: TradeSide, quantity: number): Promise<void> {
    const accountId = this.accountId();
    const body: TradeRequest = { accountId, instrumentId, side, quantity };
    await firstValueFrom(this.http.post(`/v1/accounts/${accountId}/trades`, body));
    this.refreshAfterTrade();
  }

  private refreshAfterTrade(): void {
    for (const delay of REFRESH_DELAYS_MS) {
      setTimeout(() => {
        this.orders.reload();
        this.summary.reload();
        this.holdings.reload();
      }, delay);
    }
  }
}
