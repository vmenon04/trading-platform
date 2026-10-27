import { TestBed } from '@angular/core/testing';
import { TickerLogo } from './ticker-logo';

describe('TickerLogo', () => {
  function render(ticker: string, assetClass: string | null = null): HTMLElement {
    const fixture = TestBed.createComponent(TickerLogo);
    fixture.componentRef.setInput('ticker', ticker);
    fixture.componentRef.setInput('assetClass', assetClass);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows a forex pair as its two currency codes, without asking for a logo', () => {
    const logo = render('FX:AUDCAD', 'FOREX');

    expect(logo.querySelector('img')).toBeNull();
    expect([...logo.querySelectorAll('.pair span')].map((code) => code.textContent)).toEqual(['AUD', 'CAD']);
  });

  it('knows a forex pair from its ticker when no class is given', () => {
    expect(render('FX:EURUSD').querySelector('.pair')?.textContent).toBe('EURUSD');
  });

  it('looks up a crypto logo by coin, reading the class from the ticker when it is not given', () => {
    expect(render('X:BTC-USD').querySelector('img')?.getAttribute('src')).toBe('/logo/crypto/btc');
  });

  it('looks up a stock logo by ticker', () => {
    expect(render('AAPL', 'STOCK').querySelector('img')?.getAttribute('src')).toBe('/logo/ticker/AAPL');
  });
});
