import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Instrument, InstrumentSummary } from '../instrument-summary/instrument-summary';
import { ErrorResponse } from '../../models/account.models';
import { AccountService } from '../../services/account.service';

@Component({
  imports: [FormsModule, InstrumentSummary],
  selector: 'app-order-submitter',
  styleUrl: './order-submitter.css',
  templateUrl: './order-submitter.html',
})
export class OrderSubmitter {
  private account = inject(AccountService);

  mode: 'buy' | 'sell' = 'buy';
  search = '';
  quantity = 0;
  selectedInstrument: Instrument | null = null;

  submitting = signal(false);
  result = signal<{ ok: boolean; message: string } | null>(null);

  // hardcoded until GET /v1/instruments works; instrumentId values are the real database ids
  instruments: Instrument[] = [
    { instrumentId: 1, symbol: 'AAPL', name: 'Apple Inc.', exchange: 'NASDAQ', price: '$228.42', change: '+1.24%' },
    { instrumentId: 6, symbol: 'NVDA', name: 'NVIDIA Corp.', exchange: 'NASDAQ', price: '$138.85', change: '+2.81%' },
    { instrumentId: 2, symbol: 'MSFT', name: 'Microsoft Corp.', exchange: 'NASDAQ', price: '$415.62', change: '-0.47%' },
    { instrumentId: 3, symbol: 'AMZN', name: 'Amazon.com Inc.', exchange: 'NASDAQ', price: '$198.32', change: '+0.68%' },
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
    this.result.set(null);
  }

  selectInstrument(instrument: Instrument): void {
    this.selectedInstrument = instrument;
    this.result.set(null);
  }

  async submitOrder(): Promise<void> {
    const instrument = this.selectedInstrument;
    if (!instrument || this.quantity <= 0 || this.submitting()) {
      return;
    }
    if (instrument.instrumentId === undefined) {
      this.result.set({ ok: false, message: `${instrument.symbol} can't be traded yet (no instrument id).` });
      return;
    }

    this.submitting.set(true);
    this.result.set(null);
    try {
      await this.account.submitTrade(instrument.instrumentId, this.mode === 'buy' ? 'BUY' : 'SELL', this.quantity);
      this.result.set({ ok: true, message: 'Order submitted. It will appear on the Orders page in a few seconds.' });
    } catch (error) {
      this.result.set({ ok: false, message: this.errorMessage(error) });
    } finally {
      this.submitting.set(false);
    }
  }

  private errorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'Something went wrong submitting the order.';
    }
    if (error.status === 0) {
      return "Couldn't reach the server. Is the backend running?";
    }
    const body = error.error as Partial<ErrorResponse> | null;
    if (error.status === 400 && body?.fieldErrors?.length) {
      return body.fieldErrors.map((fieldError) => `${fieldError.field}: ${fieldError.message}`).join('; ');
    }
    return body?.message ?? `Order failed (HTTP ${error.status}).`;
  }

  adjustQuantity(amount: number): void {
    this.quantity = Math.max(1, this.quantity + amount);
  }

  get total(): string {
    const price = this.selectedInstrument?.price.replace(/[$,]/g, '') ?? '0';
    return `$${(Number(price) * this.quantity).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
}
