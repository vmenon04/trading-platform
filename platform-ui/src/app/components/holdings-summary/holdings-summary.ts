import { Component, input } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-holdings-summary',
  styleUrl: './holdings-summary.css',
  templateUrl: './holdings-summary.html',
})
export class HoldingsSummary {
  showAll = input(false);
  showLink = input(true);

  holdings = [
    { symbol: 'AAPL', name: 'Apple Inc.', shares: '42', price: '$228.42', change: '+1.24%', value: '$9,593.64' },
    { symbol: 'NVDA', name: 'NVIDIA Corp.', shares: '18', price: '$138.85', change: '+2.81%', value: '$2,499.30' },
    { symbol: 'MSFT', name: 'Microsoft Corp.', shares: '24', price: '$415.62', change: '-0.47%', value: '$9,974.88' },
    { symbol: 'AMZN', name: 'Amazon.com Inc.', shares: '16', price: '$198.32', change: '+0.68%', value: '$3,173.12' },
    { symbol: 'GOOGL', name: 'Alphabet Inc.', shares: '12', price: '$176.24', change: '+0.92%', value: '$2,114.88' },
    { symbol: 'TSLA', name: 'Tesla Inc.', shares: '8', price: '$342.56', change: '-1.18%', value: '$2,740.48' },
  ];

  get visibleHoldings() {
    return this.showAll() ? this.holdings : this.holdings.slice(0, 4);
  }
}
