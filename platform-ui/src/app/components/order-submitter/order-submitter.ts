import { Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Instrument, InstrumentSummary } from '../instrument-summary/instrument-summary';
import { ErrorResponse, InstrumentQuote } from '../../models/account.models';
import { AccountService } from '../../services/account.service';
import { InstrumentService } from '../../services/instrument.service';

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const percent = new Intl.NumberFormat('en-US', { style: 'percent', maximumFractionDigits: 1 });

// the buy list shows at most this many matches; searching narrows it down
const MAX_LISTED = 50;

const ASSET_CLASS_LABELS: Record<string, string> = {
  STOCK: 'Stock', ETF: 'ETF', BOND: 'Bond', FOREX: 'Forex', CRYPTO: 'Crypto',
};

@Component({
  imports: [FormsModule, InstrumentSummary],
  selector: 'app-order-submitter',
  styleUrl: './order-submitter.css',
  templateUrl: './order-submitter.html',
})
export class OrderSubmitter {
  protected account = inject(AccountService);
  protected instrumentService = inject(InstrumentService);

  mode: 'buy' | 'sell' = 'buy';
  search = '';
  quantity = 0;
  selectedInstrument: Instrument | null = null;

  submitting = signal(false);
  result = signal<{ ok: boolean; message: string } | null>(null);

  // buy: the instrument catalogue (GET /v1/instruments) plus any symbols found by searching
  private instruments = computed<Instrument[]>(() =>
    this.instrumentService.instruments().map((quote) => this.toInstrument(quote)));

  // sell: the account's holdings; each comes with the instrument id the order needs
  private holdings = computed<Instrument[]>(() => {
    const byTicker = new Map(this.instrumentService.instruments().map((quote) => [quote.ticker, quote]));
    return (this.account.holdings.value() ?? []).map((holding) => {
      const quote = byTicker.get(holding.ticker);
      const priceValue = holding.price ?? quote?.price ?? null;
      return {
        instrumentId: holding.instrumentId,
        symbol: holding.ticker,
        name: holding.name,
        exchange: quote?.assetClass ? ASSET_CLASS_LABELS[quote.assetClass] ?? quote.assetClass : '',
        price: priceValue === null ? '—' : usd.format(priceValue),
        change: '',
        shares: holding.quantity,
        priceValue,
        assetClass: quote?.assetClass ?? null,
      };
    });
  });

  get visibleInstruments(): Instrument[] {
    const source = this.mode === 'sell' ? this.holdings() : this.instruments();
    const query = this.search.trim().toLowerCase();
    const matches = query
      ? source.filter((instrument) => `${instrument.symbol} ${instrument.name}`.toLowerCase().includes(query))
      : source;
    return this.mode === 'sell' ? matches : matches.slice(0, MAX_LISTED);
  }

  get listLoading(): boolean {
    return this.mode === 'sell' ? this.account.holdings.isLoading() : this.instrumentService.isLoading();
  }

  get listFailed(): boolean {
    return !!(this.mode === 'sell' ? this.account.holdings.error() : this.instrumentService.error());
  }

  // looking up a symbol that isn't listed yet (backend: Redis, then Fauxnance, then cached for next time)
  searching = signal(false);
  searchMiss = signal('');
  private searchTimer?: ReturnType<typeof setTimeout>;

  onSearchChange(value: string): void {
    this.searchMiss.set('');
    clearTimeout(this.searchTimer);
    const symbol = value.trim().toUpperCase();
    // symbols are letters, digits, '.', '-' and ':' (e.g. BRK.B, BTC-USD, FX:EURUSD)
    if (this.mode !== 'buy' || !/^[A-Z0-9.:\-]{1,15}$/.test(symbol)
        || this.instruments().some((instrument) => instrument.symbol === symbol)) {
      return;
    }
    this.searchTimer = setTimeout(() => this.lookUp(symbol), 500);
  }

  private async lookUp(symbol: string): Promise<void> {
    this.searching.set(true);
    try {
      const found = await this.instrumentService.search(symbol);
      if (!found && this.search.trim().toUpperCase() === symbol
          && !this.instruments().some((instrument) => `${instrument.symbol} ${instrument.name}`.toUpperCase().includes(symbol))) {
        this.searchMiss.set(`No instrument found for "${symbol}".`);
      }
    } catch {
      this.searchMiss.set(`Couldn't look up "${symbol}" right now.`);
    } finally {
      this.searching.set(false);
    }
  }

  // share of the account's value that is invested, from the account summary
  readonly invested = computed(() => {
    const summary = this.account.summary.value();
    const total = summary ? summary.investmentValue + summary.cashBalance : 0;
    return total > 0 ? summary!.investmentValue / total : 0;
  });

  readonly investedLabel = computed(() => `${percent.format(this.invested())} invested`);

  readonly cashLabel = computed(() => {
    const summary = this.account.summary.value();
    return summary ? `${usd.format(summary.cashBalance)} cash available` : '';
  });

  setMode(mode: 'buy' | 'sell'): void {
    this.mode = mode;
    this.search = '';
    this.searchMiss.set('');
    this.selectedInstrument = null;
    this.result.set(null);
  }

  selectInstrument(instrument: Instrument): void {
    this.selectedInstrument = instrument;
    this.result.set(null);
  }

  // selling more than you hold would be rejected by the backend, so it's stopped here first
  get overSelling(): boolean {
    const shares = this.selectedInstrument?.shares;
    return this.mode === 'sell' && shares !== undefined && this.quantity > shares;
  }

  async submitOrder(): Promise<void> {
    const instrument = this.selectedInstrument;
    if (!instrument || this.quantity <= 0 || this.overSelling || this.submitting()) {
      return;
    }
    if (instrument.instrumentId === undefined) {
      this.result.set({ ok: false, message: `${instrument.symbol} can't be traded right now.` });
      return;
    }

    this.submitting.set(true);
    this.result.set(null);
    try {
      await this.account.submitTrade(instrument.instrumentId, this.mode === 'buy' ? 'BUY' : 'SELL', this.quantity);
      this.result.set({ ok: true, message: 'Order submitted. It will appear in Recent orders in a few seconds.' });
    } catch (error) {
      this.result.set({ ok: false, message: this.errorMessage(error) });
    } finally {
      this.submitting.set(false);
    }
  }

  private toInstrument(quote: InstrumentQuote): Instrument {
    return {
      instrumentId: quote.instrumentId,
      symbol: quote.ticker,
      name: quote.name,
      exchange: quote.assetClass ? ASSET_CLASS_LABELS[quote.assetClass] ?? quote.assetClass : '',
      price: quote.price === null ? '—' : usd.format(quote.price),
      change: '',
      priceValue: quote.price,
      assetClass: quote.assetClass,
    };
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

  // estimate from the backend's current price; the order itself is priced by the backend when it executes
  get total(): string {
    const price = this.selectedInstrument?.priceValue;
    return price === null || price === undefined ? '—' : usd.format(price * this.quantity);
  }
}
