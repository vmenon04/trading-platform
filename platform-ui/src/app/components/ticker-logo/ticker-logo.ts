import { Component, computed, effect, input, signal } from '@angular/core';

// The company or coin logo for a ticker from logo.dev (through the /logo dev proxy, which adds the key);
// falls back to the ticker's first letter when there is no logo for it.
@Component({
  selector: 'app-ticker-logo',
  template: `
    @if (!failed()) {
      <img [src]="src()" alt="" loading="lazy" (error)="failed.set(true)">
    } @else {
      {{ ticker().charAt(0) }}
    }
  `,
  styles: `
    :host { display: contents; }
    img { display: block; width: 100%; height: 100%; border-radius: 6px; object-fit: contain; background: #fff; }
  `,
})
export class TickerLogo {
  ticker = input.required<string>();
  // e.g. CRYPTO, from the instruments API; crypto logos are looked up by coin instead of stock ticker
  assetClass = input<string | null>(null);

  protected failed = signal(false);

  protected src = computed(() => {
    const ticker = this.ticker().toUpperCase();
    return this.assetClass() === 'CRYPTO'
      ? `/logo/crypto/${encodeURIComponent(ticker.toLowerCase())}`
      : `/logo/ticker/${encodeURIComponent(ticker)}`;
  });

  constructor() {
    // a different ticker gets a fresh try
    effect(() => {
      this.src();
      this.failed.set(false);
    });
  }
}
