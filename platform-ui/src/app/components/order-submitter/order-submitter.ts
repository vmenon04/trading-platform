import { Component, DestroyRef, computed, effect, inject, output, signal, untracked } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Instrument, InstrumentSummary } from '../instrument-summary/instrument-summary';
import { InstrumentQuote } from '../../models/account.models';
import { AccountService } from '../../services/account.service';
import { InstrumentService } from '../../services/instrument.service';
import { formatPrice, unitName } from '../../models/instrument-format';

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const percent = new Intl.NumberFormat('en-US', { style: 'percent', maximumFractionDigits: 1 });

// symbols are letters, digits, '.', '-' and ':' (e.g. BRK.B, BTC-USD, FX:EURUSD)
const SYMBOL = /^[A-Z0-9.:\-]{1,15}$/;

const ASSET_CLASS_LABELS: Record<string, string> = {
  STOCK: 'Stock', ETF: 'ETF', FOREX: 'Forex', CRYPTO: 'Crypto',
};

// the tabs above the buy list; null shows every class
const CLASS_TABS: { value: string | null; label: string }[] = [
  { value: null, label: 'All' },
  { value: 'STOCK', label: 'Stocks' },
  { value: 'ETF', label: 'ETFs' },
  { value: 'FOREX', label: 'Forex' },
  { value: 'CRYPTO', label: 'Crypto' },
];

@Component({
  imports: [FormsModule, InstrumentSummary],
  selector: 'app-order-submitter',
  styleUrl: './order-submitter.css',
  templateUrl: './order-submitter.html',
})
export class OrderSubmitter {
  protected account = inject(AccountService);
  protected instrumentService = inject(InstrumentService);

  protected readonly classTabs = CLASS_TABS;

  mode: 'buy' | 'sell' = 'buy';
  search = '';
  quantity: number | null = null;
  selectedInstrument: Instrument | null = null;
  // the instrument picked for the order, or null when the choice is cleared (e.g. switching between buy and sell)
  readonly instrumentChange = output<Instrument | null>();

  submitting = signal(false);
  result = signal<{ ok: boolean; message: string } | null>(null);

  // buy: the instruments loaded so far from the catalogue (GET /v1/instruments), searched by the backend
  private instruments = computed<Instrument[]>(() =>
    this.instrumentService.instruments().map((quote) => this.toInstrument(quote)));

  // sell: the account's holdings; each comes with the instrument id the order needs
  private holdings = computed<Instrument[]>(() =>
    (this.account.holdings.value() ?? []).map((holding) => ({
      instrumentId: holding.instrumentId,
      symbol: holding.ticker,
      name: holding.name,
      exchange: holding.assetClass ? ASSET_CLASS_LABELS[holding.assetClass] ?? holding.assetClass : '',
      price: formatPrice(holding.price, holding.assetClass),
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

  // "Showing 20 of 334" above the Load more button
  get showingLabel(): string {
    return `Showing ${this.instruments().length} of ${this.instrumentService.total()}`;
  }

  // a later batch loading keeps the list on screen; only the first one shows the loading message
  get listLoading(): boolean {
    return this.mode === 'sell'
      ? this.account.holdings.isLoading()
      : this.instrumentService.isLoading() && this.instrumentService.offset() === 0;
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
      // within one asset class a new symbol wouldn't show anyway, so nothing is looked up there
      if (this.instrumentService.isLoading() || this.instrumentService.offset() > 0 || !SYMBOL.test(symbol)
          || this.instrumentService.assetClass() !== null
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

  pickClass(assetClass: string | null): void {
    this.searchMiss.set('');
    this.instrumentService.setAssetClass(assetClass);
  }

  // "share" or "unit" for the selected instrument, plural for any amount but 1
  unitOf(amount: number | null): string {
    return unitName(amount ?? 0, this.selectedInstrument?.assetClass ?? null);
  }

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
    return this.mode === 'sell' && shares !== undefined && this.quantity !== null && this.quantity > shares;
  }

  async submitOrder(): Promise<void> {
    const instrument = this.selectedInstrument;
    const quantity = this.quantity;
    if (!instrument || quantity === null || quantity <= 0 || this.overSelling || this.submitting()) {
      return;
    }
    if (instrument.instrumentId === undefined) {
      this.result.set({ ok: false, message: `${instrument.symbol} can't be traded right now.` });
      return;
    }

    this.submitting.set(true);
    this.result.set(null);
    try {
      await this.account.submitTrade(instrument.instrumentId, this.mode === 'buy' ? 'BUY' : 'SELL', quantity);
      this.result.set({ ok: true, message: 'Order submitted. Follow it in Recent orders.' });
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
      price: formatPrice(quote.price, quote.assetClass),
      change: '',
      priceValue: quote.price,
      assetClass: quote.assetClass,
    };
  }

  private errorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'Something went wrong submitting the order.';
    }
    // the response's own message is written for developers (ids, field names), so it isn't shown
    if (error.status === 0) {
      return "We couldn't connect. Check your internet connection and try again.";
    }
    if (error.status === 400) {
      return 'Please check your order and try again.';
    }
    if (error.status === 403) {
      return "You can't place orders on this account.";
    }
    return "Your order couldn't be placed. Please try again.";
  }

  adjustQuantity(amount: number): void {
    this.quantity = Math.max(1, (this.quantity ?? 0) + amount);
  }

  // estimate from the backend's current price; the order itself is priced by the backend when it executes
  get total(): string {
    const price = this.selectedInstrument?.priceValue;
    return price === null || price === undefined || this.quantity === null || this.quantity <= 0
      ? '—'
      : usd.format(price * this.quantity);
  }
}
