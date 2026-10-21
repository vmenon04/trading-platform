import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { InvestmentAccount, investmentAccounts } from '../../components/account-selector/account-selector';
import { AccountService } from '../../services/account.service';
import { AuthService } from '../../services/auth.service';

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

@Component({
  imports: [FormsModule],
  selector: 'app-account',
  styleUrl: './account.css',
  templateUrl: './account.html',
})
export class Account {
  protected account = inject(AccountService);
  private auth = inject(AuthService);

  depositAmount = '';
  accounts = investmentAccounts;
  selectedAccount: InvestmentAccount = this.accounts[0];
  protected isDepositing = signal(false);
  protected depositConfirmed = signal(false);

  cashBalance = computed(() => {
    const summary = this.account.summary.value();
    return summary ? usd.format(summary.cashBalance) : '...';
  });

  protected depositFailed = signal(false);

  // POST /v1/accounts/{accountId}/deposits through AccountService, which also refreshes the cash balance
  async deposit(): Promise<void> {
    const amount = Number(this.depositAmount);
    if (!Number.isFinite(amount) || amount <= 0 || this.isDepositing()) {
      return;
    }

    this.depositConfirmed.set(false);
    this.depositFailed.set(false);
    this.isDepositing.set(true);
    try {
      await this.account.deposit(Math.round(amount * 100) / 100);
      this.depositConfirmed.set(true);
      this.depositAmount = '';
    } catch {
      this.depositFailed.set(true);
    } finally {
      this.isDepositing.set(false);
    }
  }

  selectAccount(account: InvestmentAccount): void {
    this.selectedAccount = account;
    this.depositConfirmed.set(false);
  }

  // clears the login token, then goes to /login
  logout(): void {
    this.auth.logout();
  }
}
