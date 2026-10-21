import { Component, computed, inject } from '@angular/core';
import { FinancialCard } from '../../components/financial-card/financial-card';
import { HoldingsSummary } from '../../components/holdings-summary/holdings-summary';
import { AccountService } from '../../services/account.service';

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

@Component({
  imports: [FinancialCard, HoldingsSummary],
  selector: 'app-dashboard',
  styleUrl: './dashboard.css',
  templateUrl: './dashboard.html',
})
export class Dashboard {
  protected account = inject(AccountService);

  portfolioValue = computed(() => this.formatSummary((summary) => summary.portfolioValue));
  cashBalance = computed(() => this.formatSummary((summary) => summary.cashBalance));
  investmentValue = computed(() => this.formatSummary((summary) => summary.investmentValue));

  private formatSummary(pick: (summary: NonNullable<ReturnType<typeof this.account.summary.value>>) => number): string {
    const summary = this.account.summary.value();
    return summary ? usd.format(pick(summary)) : '...';
  }
}
