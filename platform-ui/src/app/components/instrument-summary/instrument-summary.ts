import { Component, input, output } from '@angular/core';
import { TickerLogo } from '../ticker-logo/ticker-logo';

export interface Instrument {
  // database id, needed to submit a trade
  instrumentId?: number;
  symbol: string;
  name: string;
  exchange: string;
  price: string;
  change: string;
  shares?: number;
  // the price as a number for the estimated total; null when there's no current price
  priceValue?: number | null;
  // e.g. STOCK or CRYPTO, used to look up the right logo
  assetClass?: string | null;
}

@Component({
  imports: [TickerLogo],
  selector: 'app-instrument-summary',
  styleUrl: './instrument-summary.css',
  templateUrl: './instrument-summary.html',
})
export class InstrumentSummary {
  instrument = input.required<Instrument>();
  variant = input<'option' | 'selected'>('option');
  // an option that is the instrument picked for the order; clicking it again unpicks it
  active = input(false);
  selected = output<Instrument>();

  choose(): void {
    this.selected.emit(this.instrument());
  }
}
