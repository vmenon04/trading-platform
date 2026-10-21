import { Component, computed, inject, signal } from '@angular/core';
import { AccountService } from '../../services/account.service';

// every trading account is an individual brokerage account, shown by the last 4 characters of its id
export const ACCOUNT_TYPE = 'Individual brokerage';

export function accountNumber(accountId: string): string {
  return `•••• ${accountId.slice(-4)}`;
}

// lists the logged-in user's trading accounts (GET /v1/clients/me); choosing one switches every page to it
@Component({
  selector: 'app-account-selector',
  imports: [],
  templateUrl: './account-selector.html',
  styleUrl: './account-selector.css'
})
export class AccountSelector {
  protected account = inject(AccountService);
  protected readonly accountType = ACCOUNT_TYPE;
  protected readonly accountNumber = accountNumber;

  isOpen = false;

  protected selectedLabel = computed(() => {
    const accountId = this.account.accountId();
    return accountId ? accountNumber(accountId) : 'No account yet';
  });

  toggle(): void {
    this.isOpen = !this.isOpen;
  }

  selectAccount(accountId: string): void {
    this.account.selectedAccountId.set(accountId);
    this.isOpen = false;
  }

  // signals, because they change after an await (the app runs without zone.js)
  protected confirming = signal(false);
  protected opening = signal(false);
  protected openError = signal('');

  // "Open new account" only asks; the account is created when the user confirms
  askToOpenAccount(): void {
    this.openError.set('');
    this.confirming.set(true);
    this.isOpen = false;
  }

  cancelOpenAccount(): void {
    if (!this.opening()) {
      this.confirming.set(false);
    }
  }

  async confirmOpenAccount(): Promise<void> {
    if (this.opening()) {
      return;
    }
    this.opening.set(true);
    this.openError.set('');
    try {
      await this.account.openAdditionalAccount();
      this.confirming.set(false);
    } catch {
      this.openError.set("Couldn't open a new account. Please try again.");
    } finally {
      this.opening.set(false);
    }
  }
}
