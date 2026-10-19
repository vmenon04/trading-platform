import { Injectable, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { AccountHolding, AccountSummary, Trade, TradeSide } from '../models/account.models';

// Calls the Account & Trade Management Service. URLs are relative (/v1/...) and the
// dev server forwards them to Spring Boot via proxy.conf.json.
@Injectable({ providedIn: 'root' })
export class AccountService {
  // the account being viewed; every resource below re-fetches when it changes
  // TODO: set from login / AccountSelector instead of a sample-data account
  readonly accountId = signal('7f524097-8e4e-4c1d-9d6f-2e043f886b9d');

  // null = all orders
  readonly orderSide = signal<TradeSide | null>(null);

  readonly summary = httpResource<AccountSummary>(() => `/v1/accounts/${this.accountId()}`);

  readonly holdings = httpResource<AccountHolding[]>(() => `/v1/accounts/${this.accountId()}/holdings`);

  readonly orders = httpResource<Trade[]>(() => {
    const side = this.orderSide();
    const params: Record<string, string> = side ? { side } : {};
    return { url: `/v1/accounts/${this.accountId()}/orders`, params };
  });
}
