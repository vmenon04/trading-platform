import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AccountHolding, AccountSummary, ClientProfile, OpenAccountRequest, Trade, TradeSide, TradeRequest } from '../models/account.models';
import { AuthService } from './auth.service';

// trades go through Kafka, so reload a few times after submitting until the new order has settled
const REFRESH_DELAYS_MS = [3000, 8000, 15000];
const VALUATION_REFRESH_MS = 10 * 60 * 1000;

// upper-cases only the first letter of each word, so "mcDonald" stays "McDonald"
function capitalizeWords(name: string): string {
  return name.trim().split(/\s+/).map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
}

// Calls the Account & Trade Management Service. URLs are relative (/v1/...) and the
// dev server forwards them to Spring Boot via proxy.conf.json.
@Injectable({ providedIn: 'root' })
export class AccountService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  // the logged-in user's client and their trading accounts (GET /v1/clients/me); 404 = no accounts yet.
  // Only for CLIENT users: analysts and admins have no client of their own
  readonly client = httpResource<ClientProfile>(() => (this.auth.isClient() ? '/v1/clients/me' : undefined));

  // the user's name for display, each word capitalized ("jane doe" -> "Jane Doe"); the backend keeps it as typed
  readonly clientName = computed(() => capitalizeWords(this.client.value()?.clientName ?? ''));
  readonly accounts = computed(() => this.client.value()?.tradingAccounts ?? []);

  // the account chosen in the account selector; null = the user's first account
  readonly selectedAccountId = signal<string | null>(null);

  // the account being viewed; every resource below re-fetches when it changes, and waits while it's null
  readonly accountId = computed(() => {
    const accounts = this.accounts();
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

  // goes up each time an order is submitted, so the trade page's live list knows to look for it
  readonly submissions = signal(0);

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

  constructor() {
    setInterval(() => {
      this.summary.reload();
      this.holdings.reload();
    }, VALUATION_REFRESH_MS);
  }

  // POST /v1/accounts/{accountId}/trades -> 202 Accepted with an empty body.
  // Resolves once the order is accepted; rejects with the HttpErrorResponse otherwise.
  async submitTrade(instrumentId: number, side: TradeSide, quantity: number): Promise<void> {
    const accountId = this.accountId();
    if (!accountId) {
      throw new Error('No trading account to place the order on.');
    }
    const body: TradeRequest = { accountId, instrumentId, side, quantity };
    await firstValueFrom(this.http.post(`/v1/accounts/${accountId}/trades`, body));
    this.submissions.update((count) => count + 1);
    this.refreshAfterTrade();
  }

  // POST /v1/clients/me -> opens the logged-in user's first trading account, with no cash
  async openAccount(details: OpenAccountRequest): Promise<void> {
    await firstValueFrom(this.http.post('/v1/clients/me', details));
    this.client.reload();
  }

  // POST /v1/clients/me/accounts -> 201 with all of the user's accounts; opens another individual brokerage
  // account (with no cash) and switches the app to it
  async openAdditionalAccount(): Promise<void> {
    const before = new Set(this.accounts().map((account) => account.accountId));
    const profile = await firstValueFrom(this.http.post<ClientProfile>('/v1/clients/me/accounts', {}));
    const opened = profile.tradingAccounts.find((account) => !before.has(account.accountId));
    this.client.set(profile);
    if (opened) {
      this.selectedAccountId.set(opened.accountId);
    }
  }

  // POST /v1/accounts/{accountId}/deposits -> 204; adds practice cash to the account
  async deposit(amount: number): Promise<void> {
    const accountId = this.accountId();
    if (!accountId) {
      throw new Error('No trading account to deposit into.');
    }
    await firstValueFrom(this.http.post(`/v1/accounts/${accountId}/deposits`, { amount }));
    this.summary.reload();
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
