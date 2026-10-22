import { Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FinancialCard } from '../../components/financial-card/financial-card';
import { HoldingsSummary } from '../../components/holdings-summary/holdings-summary';
import { AccountSummary } from '../../models/account.models';
import { AccountService } from '../../services/account.service';

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

// practice money: one deposit can't exceed this
const MAX_DEPOSIT = 1_000_000;

@Component({
  imports: [FinancialCard, FormsModule, HoldingsSummary, RouterLink],
  selector: 'app-portfolio',
  styleUrl: './portfolio.css',
  templateUrl: './portfolio.html',
})
export class Portfolio {
  protected account = inject(AccountService);

  readonly quickAmounts = [1_000, 10_000, 100_000];
  readonly maxDeposit = MAX_DEPOSIT;

  // deposit window
  depositOpen = signal(false);
  depositAmount: number | null = null;
  depositing = signal(false);
  depositError = signal('');
  depositSuccess = signal('');

  // open-account form, for users who registered a login before accounts were opened automatically
  firstName = '';
  lastName = '';
  email = '';
  birthDate = '';
  opening = signal(false);
  openError = signal('');

  constructor() {
    this.account.refresh();
  }

  // card values as display strings; '…' while the summary is loading
  private format(pick: (s: AccountSummary) => number) {
    return computed(() => {
      const summary = this.account.summary.value();
      return summary ? usd.format(pick(summary)) : '…';
    });
  }

  totalValue = this.format((s) => s.portfolioValue);
  investments = this.format((s) => s.investmentValue);
  cashBalance = this.format((s) => s.cashBalance);
  costBasis = this.format((s) => s.costBasis);

  openDeposit(): void {
    this.depositAmount = null;
    this.depositError.set('');
    this.depositSuccess.set('');
    this.depositOpen.set(true);
  }

  closeDeposit(): void {
    if (!this.depositing()) {
      this.depositOpen.set(false);
    }
  }

  async submitDeposit(): Promise<void> {
    const amount = Number(this.depositAmount);
    if (!Number.isFinite(amount) || amount <= 0) {
      this.depositError.set('Enter an amount greater than $0.');
      return;
    }
    if (amount > MAX_DEPOSIT) {
      this.depositError.set(`You can deposit up to ${usd.format(MAX_DEPOSIT)} at a time.`);
      return;
    }
    // cents only
    const rounded = Math.round(amount * 100) / 100;

    this.depositing.set(true);
    this.depositError.set('');
    try {
      await this.account.deposit(rounded);
      this.depositOpen.set(false);
      this.depositSuccess.set(`${usd.format(rounded)} added to your cash balance.`);
    } catch (error) {
      this.depositError.set(error instanceof HttpErrorResponse && error.status === 403
        ? "You can only deposit into your own account."
        : 'The deposit failed. Please try again.');
    } finally {
      this.depositing.set(false);
    }
  }

  async openAccount(): Promise<void> {
    if (!this.firstName.trim() || !this.lastName.trim() || !this.email.trim() || !this.birthDate) {
      this.openError.set('Please fill in every field.');
      return;
    }
    this.opening.set(true);
    this.openError.set('');
    try {
      await this.account.openAccount({
        firstName: this.firstName,
        lastName: this.lastName,
        email: this.email,
        birthDate: this.birthDate,
      });
    } catch (error) {
      const message = error instanceof HttpErrorResponse ? error.error?.message : null;
      this.openError.set(error instanceof HttpErrorResponse && error.status === 400
        ? 'Please check your details (a valid email and a birth date in the past).'
        : message ?? "Couldn't open your account. Please try again.");
    } finally {
      this.opening.set(false);
    }
  }
}
