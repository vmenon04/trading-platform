import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AccountHolding, AccountSummary, ClientProfile, Trade, TradeSide, TradeRequest } from '../models/account.models';
import { AuthService } from './auth.service';

// trades go through Kafka, so reload a few times after submitting until the new order has settled
const REFRESH_DELAYS_MS = [3000, 8000, 15000];

// Calls the Account & Trade Management Service. URLs are relative (/v1/...) and the
// dev server forwards them to Spring Boot via proxy.conf.json.
@Injectable({ providedIn: 'root' })
export class AccountService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  // the logged-in user's client and their trading accounts (GET /v1/clients/me); 404 = no accounts yet
  readonly client = httpResource<ClientProfile>(() => (this.auth.isLoggedIn() ? '/v1/clients/me' : undefined));

  // the account chosen in the account selector; null = the user's first account
  readonly selectedAccountId = signal<string | null>(null);

  // the account being viewed; every resource below re-fetches when it changes, and waits while it's null
  readonly accountId = computed(() => {
    const accounts = this.client.value()?.tradingAccounts ?? [];
    const selected = this.selectedAccountId();
    return accounts.find((account) => account.accountId === selected)?.accountId ?? accounts[0]?.accountId ?? null;
  });

  // true once we know the user has no trading account (only registered a login)
  readonly hasNoAccount = computed(() => {
    if (this.client.isLoading()) {
      return false;
    }
    const error = this.client.error() as { status?: number } | undefined;
    return error?.status === 404 || (this.client.hasValue() && this.client.value().tradingAccounts.length === 0);
  });

  // null = all orders
  readonly orderSide = signal<TradeSide | null>(null);

  readonly summary = httpResource<AccountSummary>(() => {
    const accountId = this.accountId();
    return accountId ? `/v1/accounts/${accountId}` : undefined;
  });

  readonly holdings = httpResource<AccountHolding[]>(() => {
    const accountId = this.accountId();
    return accountId ? `/v1/accounts/${accountId}/holdings` : undefined;
  });

  readonly orders = httpResource<Trade[]>(() => {
    const accountId = this.accountId();
    if (!accountId) {
      return undefined;
    }
    const side = this.orderSide();
    const params: Record<string, string> = side ? { side } : {};
    return { url: `/v1/accounts/${accountId}/orders`, params };
  });

  // POST /v1/accounts/{accountId}/trades -> 202 Accepted with an empty body.
  // Resolves once the order is accepted; rejects with the HttpErrorResponse otherwise.
  async submitTrade(instrumentId: number, side: TradeSide, quantity: number): Promise<void> {
    const accountId = this.accountId();
    if (!accountId) {
      throw new Error('No trading account to place the order on.');
    }
    const body: TradeRequest = { accountId, instrumentId, side, quantity };
    await firstValueFrom(this.http.post(`/v1/accounts/${accountId}/trades`, body));
    this.refreshAfterTrade();
  }

  // the resources are fetched once for the whole app, so pages call this when opened to avoid stale data
  refresh(): void {
    this.client.reload();
    this.orders.reload();
    this.summary.reload();
    this.holdings.reload();
  }

  private refreshAfterTrade(): void {
    for (const delay of REFRESH_DELAYS_MS) {
      setTimeout(() => this.refresh(), delay);
    }
  }
}
