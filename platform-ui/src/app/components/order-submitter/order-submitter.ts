import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Instrument, InstrumentSummary } from '../instrument-summary/instrument-summary';

@Component({
  imports: [FormsModule, InstrumentSummary],
  selector: 'app-order-submitter',
  styleUrl: './order-submitter.css',
  templateUrl: './order-submitter.html',
})
export class OrderSubmitter {
  mode: 'buy' | 'sell' = 'buy';
  search = '';
  quantity = 5;
  selectedInstrument: Instrument | null = null;

  instruments: Instrument[] = [
    { symbol: 'AAPL', name: 'Apple Inc.', exchange: 'NASDAQ', price: '$228.42', change: '+1.24%' },
    { symbol: 'NVDA', name: 'NVIDIA Corp.', exchange: 'NASDAQ', price: '$138.85', change: '+2.81%' },
    { symbol: 'MSFT', name: 'Microsoft Corp.', exchange: 'NASDAQ', price: '$415.62', change: '-0.47%' },
    { symbol: 'AMZN', name: 'Amazon.com Inc.', exchange: 'NASDAQ', price: '$198.32', change: '+0.68%' },
  ];

  holdings: Instrument[] = [
    { ...this.instruments[0], shares: 42 },
    { ...this.instruments[1], shares: 18 },
    { ...this.instruments[2], shares: 24 },
    { ...this.instruments[3], shares: 16 },
  ];

  get visibleInstruments(): Instrument[] {
    const source = this.mode === 'sell' ? this.holdings : this.instruments;
    const query = this.search.trim().toLowerCase();
    return query ? source.filter((instrument) => `${instrument.symbol} ${instrument.name}`.toLowerCase().includes(query)) : source;
  }

  setMode(mode: 'buy' | 'sell'): void {
    this.mode = mode;
    this.search = '';
    this.selectedInstrument = null;
  }

  selectInstrument(instrument: Instrument): void {
    this.selectedInstrument = instrument;
  }

  adjustQuantity(amount: number): void {
    this.quantity = Math.max(1, this.quantity + amount);
  }

  get total(): string {
    const price = this.selectedInstrument?.price.replace(/[$,]/g, '') ?? '0';
    return `$${(Number(price) * this.quantity).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
}
