import { Component, input, output } from '@angular/core';

export interface Instrument {
  // database id, needed to submit a trade
  instrumentId?: number;
  symbol: string;
  name: string;
  exchange: string;
  price: string;
  change: string;
  shares?: number;
}

@Component({
  imports: [],
  selector: 'app-instrument-summary',
  styleUrl: './instrument-summary.css',
  templateUrl: './instrument-summary.html',
})
export class InstrumentSummary {
  instrument = input.required<Instrument>();
  variant = input<'option' | 'selected'>('option');
  selected = output<Instrument>();

  choose(): void {
    this.selected.emit(this.instrument());
  }
}
