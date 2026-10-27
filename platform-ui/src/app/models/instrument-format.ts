// Prices and quantities written the way investors expect for each kind of investment: stocks and ETFs trade in
// shares at dollar prices, crypto and forex trade in units, and a forex price is an exchange rate, not dollars.

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
// a coin worth less than a dollar keeps enough digits to show its price (e.g. $0.00001234)
const smallUsd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumSignificantDigits: 4 });
// rates near 1 (EUR/USD 1.08425) show five decimals, large ones (USD/JPY 150.235) three
const smallRate = new Intl.NumberFormat('en-US', { minimumFractionDigits: 5, maximumFractionDigits: 5 });
const largeRate = new Intl.NumberFormat('en-US', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
// quantities can be fractional (0.005 BTC), down to the 8 decimals an order may have
const quantity = new Intl.NumberFormat('en-US', { maximumFractionDigits: 8 });

/**
 * The asset class of an instrument: the one given, or else read from the ticker's prefix (FX:EURUSD is forex,
 * X:BTC-USD is crypto), for orders, which don't carry their class.
 */
export function assetClassOf(ticker: string | null | undefined, assetClass?: string | null): string | null {
  if (assetClass) return assetClass;
  if (ticker?.startsWith('FX:')) return 'FOREX';
  if (ticker?.startsWith('X:')) return 'CRYPTO';
  return null;
}

/** A unit price to show, or a dash when there's none. */
export function formatPrice(price: number | null | undefined, assetClass: string | null): string {
  if (price === null || price === undefined) return '—';
  if (assetClass === 'FOREX') return (price >= 10 ? largeRate : smallRate).format(price);
  if (assetClass === 'CRYPTO' && price < 1) return smallUsd.format(price);
  return usd.format(price);
}

export function formatQuantity(amount: number): string {
  return quantity.format(amount);
}

/** "share" or "unit", plural unless the amount is exactly 1. */
export function unitName(amount: number, assetClass: string | null): string {
  const unit = assetClass === 'CRYPTO' || assetClass === 'FOREX' ? 'unit' : 'share';
  return amount === 1 ? unit : `${unit}s`;
}

/** e.g. "10 shares", "0.005 units". */
export function formatHolding(amount: number, assetClass: string | null): string {
  return `${formatQuantity(amount)} ${unitName(amount, assetClass)}`;
}
