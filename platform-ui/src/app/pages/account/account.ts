import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { InvestmentAccount, investmentAccounts } from '../../components/account-selector/account-selector';
import { AccountService } from '../../services/account.service';

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

@Component({
  imports: [FormsModule],
  selector: 'app-account',
  styleUrl: './account.css',
  templateUrl: './account.html',
})
export class Account {
  protected account = inject(AccountService);
  private router = inject(Router);

  depositAmount = '';
  accounts = investmentAccounts;
  selectedAccount: InvestmentAccount = this.accounts[0];
  protected isDepositing = signal(false);
  protected depositConfirmed = signal(false);

  cashBalance = computed(() => {
    const summary = this.account.summary.value();
    return summary ? usd.format(summary.cashBalance) : '...';
  });

  deposit(): void {
    const amount = Number(this.depositAmount);
    if (!Number.isFinite(amount) || amount <= 0 || this.isDepositing()) {
      return;
    }

    this.depositConfirmed.set(false);
    this.isDepositing.set(true);
    setTimeout(() => {
      this.isDepositing.set(false);
      this.depositConfirmed.set(true);
      this.depositAmount = '';
    }, 800);
  }

  selectAccount(account: InvestmentAccount): void {
    this.selectedAccount = account;
    this.depositConfirmed.set(false);
  }

  logout(): void {
    this.router.navigate(['/login']);
  }
}
