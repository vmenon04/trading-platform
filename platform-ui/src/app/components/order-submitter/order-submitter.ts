import { Component, DestroyRef, computed, effect, inject, output, signal, untracked } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Instrument, InstrumentSummary } from '../instrument-summary/instrument-summary';
import { ErrorResponse, InstrumentQuote } from '../../models/account.models';
import { AccountService } from '../../services/account.service';
import { InstrumentService, PAGE_SIZE } from '../../services/instrument.service';

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const percent = new Intl.NumberFormat('en-US', { style: 'percent', maximumFractionDigits: 1 });

// symbols are letters, digits, '.', '-' and ':' (e.g. BRK.B, BTC-USD, FX:EURUSD)
const SYMBOL = /^[A-Z0-9.:\-]{1,15}$/;

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
  // the instrument picked for the order, or null when the choice is cleared (e.g. switching between buy and sell)
  readonly instrumentChange = output<Instrument | null>();

  submitting = signal(false);
  result = signal<{ ok: boolean; message: string } | null>(null);

  // buy: a page of the instrument catalogue (GET /v1/instruments), searched by the backend
  private instruments = computed<Instrument[]>(() =>
    this.instrumentService.instruments().map((quote) => this.toInstrument(quote)));

  // sell: the account's holdings; each comes with the instrument id the order needs
  private holdings = computed<Instrument[]>(() =>
    (this.account.holdings.value() ?? []).map((holding) => ({
      instrumentId: holding.instrumentId,
      symbol: holding.ticker,
      name: holding.name,
      exchange: holding.assetClass ? ASSET_CLASS_LABELS[holding.assetClass] ?? holding.assetClass : '',
      price: holding.price === null ? '—' : usd.format(holding.price),
      change: '',
      shares: holding.quantity,
      priceValue: holding.price,
      assetClass: holding.assetClass,
    })));

  get visibleInstruments(): Instrument[] {
    if (this.mode === 'buy') {
      return this.instruments();
    }
    const query = this.search.trim().toLowerCase();
    return query
      ? this.holdings().filter((instrument) => `${instrument.symbol} ${instrument.name}`.toLowerCase().includes(query))
      : this.holdings();
  }

  // "51–100 of 334" under the buy list
  get pageLabel(): string {
    const start = this.instrumentService.offset();
    const total = this.instrumentService.total();
    return `${start + 1}–${Math.min(start + PAGE_SIZE, total)} of ${total}`;
  }

  // only when the matches don't fit on one page
  get paged(): boolean {
    return this.mode === 'buy' && this.instrumentService.total() > PAGE_SIZE;
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

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.searchTimer));
    // once the buy list for a symbol-like search has loaded without that symbol, ask the backend to look it up
    effect(() => {
      const symbol = this.instrumentService.query().toUpperCase();
      // an exact ticker match comes first, so only the first page can tell that the symbol isn't listed
      if (this.instrumentService.isLoading() || this.instrumentService.offset() > 0 || !SYMBOL.test(symbol)
          || this.instrumentService.instruments().some((quote) => quote.ticker === symbol)) {
        return;
      }
      untracked(() => this.lookUp(symbol));
    });
  }

  // buy: the backend searches, once typing pauses; sell: the holdings are filtered as you type
  onSearchChange(value: string): void {
    this.searchMiss.set('');
    clearTimeout(this.searchTimer);
    if (this.mode === 'buy') {
      this.searchTimer = setTimeout(() => this.instrumentService.setQuery(value), 300);
    }
  }

  private async lookUp(symbol: string): Promise<void> {
    this.searching.set(true);
    try {
      const found = await this.instrumentService.search(symbol);
      if (!found && this.search.trim().toUpperCase() === symbol && !this.instruments().length) {
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
    clearTimeout(this.searchTimer);
    this.instrumentService.setQuery('');
    this.searchMiss.set('');
    this.selectedInstrument = null;
    this.instrumentChange.emit(null);
    this.result.set(null);
  }

  // clicking the instrument that's already picked unpicks it, so the order form and the news go back to their defaults
  selectInstrument(instrument: Instrument): void {
    if (this.selectedInstrument?.symbol === instrument.symbol) {
      this.selectedInstrument = null;
      this.instrumentChange.emit(null);
      this.result.set(null);
      return;
    }
    this.selectedInstrument = instrument;
    this.instrumentChange.emit(instrument);
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
