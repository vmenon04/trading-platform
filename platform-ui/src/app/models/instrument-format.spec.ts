import { assetClassOf, formatHolding, formatPrice } from './instrument-format';

describe('instrument formatting', () => {
  it('shows stock and ETF prices in dollars', () => {
    expect(formatPrice(189.5, 'STOCK')).toBe('$189.50');
    expect(formatPrice(412.07, 'ETF')).toBe('$412.07');
  });

  it('shows a forex price as a rate, not dollars', () => {
    expect(formatPrice(1.084253, 'FOREX')).toBe('1.08425');
    expect(formatPrice(150.2349, 'FOREX')).toBe('150.235');
  });

  it('keeps enough digits for a coin worth less than a dollar', () => {
    expect(formatPrice(0.00001234, 'CRYPTO')).toBe('$0.00001234');
    expect(formatPrice(67012.4, 'CRYPTO')).toBe('$67,012.40');
  });

  it('shows a dash when there is no price', () => {
    expect(formatPrice(null, 'STOCK')).toBe('—');
  });

  it('counts shares for stocks and units for crypto and forex, fractions included', () => {
    expect(formatHolding(10, 'STOCK')).toBe('10 shares');
    expect(formatHolding(1, 'ETF')).toBe('1 share');
    expect(formatHolding(0.005, 'CRYPTO')).toBe('0.005 units');
    expect(formatHolding(1, 'FOREX')).toBe('1 unit');
  });

  it('reads the asset class from the ticker when it is not given', () => {
    expect(assetClassOf('FX:EURUSD')).toBe('FOREX');
    expect(assetClassOf('X:BTC-USD')).toBe('CRYPTO');
    expect(assetClassOf('AAPL')).toBeNull();
    expect(assetClassOf('AAPL', 'STOCK')).toBe('STOCK');
  });
});
