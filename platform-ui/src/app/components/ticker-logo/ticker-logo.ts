import { Component, computed, effect, input, signal } from '@angular/core';
import { assetClassOf } from '../../models/instrument-format';

// The company or coin logo for a ticker from logo.dev (through the /logo dev proxy, which adds the key);
// falls back to the ticker's first letter when there is no logo for it. A forex pair has no logo, so it shows its two
// currency codes stacked instead (FX:AUDCAD: AUD over CAD).
@Component({
  selector: 'app-ticker-logo',
  template: `
    @if (pair(); as pair) {
      <span class="pair" aria-hidden="true"><span>{{ pair[0] }}</span><span>{{ pair[1] }}</span></span>
    } @else if (!failed()) {
      <img [src]="src()" alt="" loading="lazy" (error)="failed.set(true)">
    } @else {
      {{ ticker().charAt(0) }}
    }
  `,
  styles: `
    :host { display: contents; }
    img { display: block; width: 100%; height: 100%; border-radius: 6px; object-fit: contain; background: #fff; }
    /* sized from the box it's drawn in, so the codes fit a 24px table icon as well as a 40px card logo */
    .pair { display: grid; width: 100%; height: 100%; place-content: center; container-type: size; border-radius: 6px;
            background: var(--green-soft); color: var(--green); font-weight: 800; line-height: 1.05; text-align: center; }
    .pair span { font-size: 30cqh; letter-spacing: 0.02em; }
  `,
})
export class TickerLogo {
  ticker = input.required<string>();
  // e.g. CRYPTO, from the instruments API; without it, the class is read from the ticker (X:BTC-USD is crypto)
  assetClass = input<string | null>(null);

  protected failed = signal(false);

  private kind = computed(() => assetClassOf(this.ticker(), this.assetClass()));

  // the two currency codes of a forex pair, or null for anything else
  protected pair = computed<[string, string] | null>(() => {
    if (this.kind() !== 'FOREX') return null;
    const codes = this.ticker().toUpperCase().replace(/^FX:/, '');
    return codes.length === 6 ? [codes.slice(0, 3), codes.slice(3)] : null;
  });

  protected src = computed(() => {
    const ticker = this.ticker().toUpperCase();
    return this.kind() === 'CRYPTO'
      ? `/logo/crypto/${encodeURIComponent(coin(ticker))}`
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

// crypto tickers are Fauxnance symbols (X:BTC-USD); logo.dev wants the coin (btc)
function coin(ticker: string): string {
  return ticker.replace(/^X:/, '').replace(/-[A-Z]+$/, '').toLowerCase();
}
