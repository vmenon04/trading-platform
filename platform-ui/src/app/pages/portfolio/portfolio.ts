import { Component, computed, inject } from '@angular/core';
import { FinancialCard } from '../../components/financial-card/financial-card';
import { HoldingsSummary } from '../../components/holdings-summary/holdings-summary';
import { AccountSummary } from '../../models/account.models';
import { AccountService } from '../../services/account.service';

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

@Component({
  imports: [FinancialCard, HoldingsSummary],
  selector: 'app-portfolio',
  styleUrl: './portfolio.css',
  templateUrl: './portfolio.html',
})
export class Portfolio {
  protected account = inject(AccountService);

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

  totalValue = this.format((s) => s.investmentValue + s.cashBalance);
  investments = this.format((s) => s.investmentValue);
  cashBalance = this.format((s) => s.cashBalance);
  costBasis = this.format((s) => s.costBasis);
}
