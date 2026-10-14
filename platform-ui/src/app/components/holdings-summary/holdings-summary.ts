import { Component } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-holdings-summary',
  styleUrl: './holdings-summary.css',
  templateUrl: './holdings-summary.html',
})
export class HoldingsSummary {
  holdings = [
    { symbol: 'AAPL', name: 'Apple Inc.', shares: '42', price: '$228.42', change: '+1.24%', value: '$9,593.64' },
    { symbol: 'NVDA', name: 'NVIDIA Corp.', shares: '18', price: '$138.85', change: '+2.81%', value: '$2,499.30' },
    { symbol: 'MSFT', name: 'Microsoft Corp.', shares: '24', price: '$415.62', change: '-0.47%', value: '$9,974.88' },
    { symbol: 'AMZN', name: 'Amazon.com Inc.', shares: '16', price: '$198.32', change: '+0.68%', value: '$3,173.12' },
  ];
}
