import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FinancialCard } from '../../components/financial-card/financial-card';
import { HoldingsSummary } from '../../components/holdings-summary/holdings-summary';
import { AccountService } from '../../services/account.service';

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const usdCompact = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', notation: 'compact', maximumFractionDigits: 1 });
const percent = new Intl.NumberFormat('en-US', { style: 'percent', maximumFractionDigits: 1 });
// "+$12.34" / "-$12.34" for gains and losses
const signedUsd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', signDisplay: 'exceptZero' });
const signedPercent = new Intl.NumberFormat('en-US', { style: 'percent', maximumFractionDigits: 2, signDisplay: 'exceptZero' });

// allocation slices: the largest holdings, then "Other", then cash
const MAX_SLICES = 4;
const SLICE_COLORS = ['#168363', '#2fa57c', '#7cc0a9', '#a9d6c6', '#cfe5dc'];
const CASH_COLOR = '#e6ece9';

interface Slice {
  label: string;
  value: number;
  color: string;
  share: string;
}

// Everything shown here comes from the backend: the client (/v1/clients/me), the account summary
// (/v1/accounts/{id}) and the holdings with their market prices (/v1/accounts/{id}/holdings).
@Component({
  imports: [FinancialCard, HoldingsSummary, RouterLink],
  selector: 'app-dashboard',
  styleUrl: './dashboard.css',
  templateUrl: './dashboard.html',
})
export class Dashboard {
  protected account = inject(AccountService);

  constructor() {
    this.account.refresh();
  }

  readonly today = new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' });

  readonly greeting = (() => {
    const hour = new Date().getHours();
    return hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
  })();

  readonly firstName = computed(() => this.account.clientName().split(' ')[0] || 'investor');

  // the summary's numbers; 0 for a user with no account yet, null while still loading
  private summary = computed(() => {
    const summary = this.account.summary.value();
    if (summary) {
      return summary;
    }
    return this.account.hasNoAccount() ? { investmentValue: 0, cashBalance: 0, costBasis: 0 } : null;
  });

  private money(pick: (s: { investmentValue: number; cashBalance: number; costBasis: number }) => number) {
    return computed(() => {
      const summary = this.summary();
      return summary ? usd.format(pick(summary)) : '…';
    });
  }

  readonly totalValue = this.money((s) => s.investmentValue + s.cashBalance);
  readonly cashBalance = this.money((s) => s.cashBalance);
  // total return = market value of the holdings - what was paid for them (cost basis from the backend)
  private returnAmount = computed(() => {
    const summary = this.summary();
    return summary ? Math.round((summary.investmentValue - summary.costBasis) * 100) / 100 : null;
  });

  readonly totalReturn = computed(() => {
    const amount = this.returnAmount();
    return amount === null ? '…' : signedUsd.format(amount);
  });

  // green for a gain, red for a loss, neutral at exactly $0.00
  readonly totalReturnTone = computed(() => {
    const amount = this.returnAmount();
    return amount === null || amount === 0 ? null : amount > 0 ? 'positive' : 'negative';
  });

  readonly totalReturnChange = computed(() => {
    const summary = this.summary();
    const amount = this.returnAmount();
    if (!summary || amount === null || summary.costBasis <= 0) {
      return 'Market value minus cost basis';
    }
    return `${signedPercent.format(amount / summary.costBasis)} on ${usd.format(summary.costBasis)} invested`;
  });

  readonly investedChange = computed(() => {
    const summary = this.summary();
    return summary ? `${usd.format(summary.investmentValue)} invested` : '…';
  });

  // allocation of the total portfolio value across holdings and cash, from the backend's market prices
  readonly allocation = computed<Slice[]>(() => {
    const summary = this.summary();
    if (!summary) {
      return [];
    }
    const holdings = (this.account.holdings.value() ?? [])
      .map((holding) => ({ label: holding.ticker, value: holding.price === null ? 0 : holding.quantity * holding.price }))
      .filter((holding) => holding.value > 0)
      .sort((a, b) => b.value - a.value);

    const top = holdings.slice(0, MAX_SLICES);
    const other = holdings.slice(MAX_SLICES).reduce((sum, holding) => sum + holding.value, 0);
    const parts = [...top, ...(other > 0 ? [{ label: 'Other', value: other }] : [])]
      .map((part, index) => ({ ...part, color: SLICE_COLORS[index] }));
    if (summary.cashBalance > 0) {
      parts.push({ label: 'Cash', value: summary.cashBalance, color: CASH_COLOR });
    }

    const total = parts.reduce((sum, part) => sum + part.value, 0);
    return parts.map((part) => ({ ...part, share: total > 0 ? percent.format(part.value / total) : '0%' }));
  });

  // conic-gradient for the donut; an empty portfolio is one grey ring
  readonly donutBackground = computed(() => {
    const slices = this.allocation();
    const total = slices.reduce((sum, slice) => sum + slice.value, 0);
    if (total <= 0) {
      return `conic-gradient(${CASH_COLOR} 0 100%)`;
    }
    let start = 0;
    const stops = slices.map((slice) => {
      const end = start + (slice.value / total) * 100;
      const stop = `${slice.color} ${start}% ${end}%`;
      start = end;
      return stop;
    });
    return `conic-gradient(${stops.join(', ')})`;
  });

  readonly donutTotal = computed(() => {
    const summary = this.summary();
    return summary ? usdCompact.format(summary.investmentValue + summary.cashBalance) : '…';
  });
}
