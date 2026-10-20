import { Component, computed, input } from '@angular/core';
import { AccountHolding } from '../../models/account.models';

interface HoldingRow {
  symbol: string;
  name: string;
  shares: string;
  price: string;
  change: string;
  value: string;
}

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const shares = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

@Component({
  imports: [],
  selector: 'app-holdings-summary',
  styleUrl: './holdings-summary.css',
  templateUrl: './holdings-summary.html',
})
export class HoldingsSummary {
  showAll = input(false);
  showLink = input(true);
  // holdings from the API; when not passed (Dashboard), the mock rows below are shown
  data = input<AccountHolding[] | null>(null);

  mockHoldings: HoldingRow[] = [
    { symbol: 'AAPL', name: 'Apple Inc.', shares: '42', price: '$228.42', change: '+1.24%', value: '$9,593.64' },
    { symbol: 'NVDA', name: 'NVIDIA Corp.', shares: '18', price: '$138.85', change: '+2.81%', value: '$2,499.30' },
    { symbol: 'MSFT', name: 'Microsoft Corp.', shares: '24', price: '$415.62', change: '-0.47%', value: '$9,974.88' },
    { symbol: 'AMZN', name: 'Amazon.com Inc.', shares: '16', price: '$198.32', change: '+0.68%', value: '$3,173.12' },
    { symbol: 'GOOGL', name: 'Alphabet Inc.', shares: '12', price: '$176.24', change: '+0.92%', value: '$2,114.88' },
    { symbol: 'TSLA', name: 'Tesla Inc.', shares: '8', price: '$342.56', change: '-1.18%', value: '$2,740.48' },
  ];

  holdings = computed<HoldingRow[]>(() => {
    const data = this.data();
    if (data === null) {
      return this.mockHoldings;
    }
    return data.map((holding) => ({
      symbol: holding.ticker,
      name: holding.name,
      shares: shares.format(holding.quantity),
      price: holding.price === null ? '—' : usd.format(holding.price),
      change: '—',
      value: holding.price === null ? '—' : usd.format(holding.quantity * holding.price),
    }));
  });

  visibleHoldings = computed(() => (this.showAll() ? this.holdings() : this.holdings().slice(0, 4)));
}
