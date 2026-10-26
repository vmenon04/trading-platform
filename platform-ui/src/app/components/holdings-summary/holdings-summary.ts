import { Component, computed, input } from '@angular/core';
import { TickerLogo } from '../ticker-logo/ticker-logo';
import { AccountHolding } from '../../models/account.models';

interface HoldingRow {
  symbol: string;
  name: string;
  shares: string;
  price: string;
}

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const shares = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

@Component({
  imports: [TickerLogo],
  selector: 'app-holdings-summary',
  styleUrl: './holdings-summary.css',
  templateUrl: './holdings-summary.html',
})
export class HoldingsSummary {
  showAll = input(false);
  showLink = input(true);
  // Holdings from the API; no data produces an empty table.
  data = input<AccountHolding[] | null>(null);

  holdings = computed<HoldingRow[]>(() => {
    return (this.data() ?? []).map((holding: AccountHolding) => ({
      symbol: holding.ticker,
      name: holding.name,
      shares: shares.format(holding.quantity),
      price: holding.price === null ? '—' : usd.format(holding.price),
    }));
  });

  visibleHoldings = computed(() => (this.showAll() ? this.holdings() : this.holdings().slice(0, 4)));
}
