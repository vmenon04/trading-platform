import { Component, computed, input } from '@angular/core';
import { TickerLogo } from '../ticker-logo/ticker-logo';
import { AccountHolding } from '../../models/account.models';
import { formatHolding, formatPrice } from '../../models/instrument-format';

interface HoldingRow {
  symbol: string;
  name: string;
  shares: string;
  price: string;
}

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
      shares: formatHolding(holding.quantity, holding.assetClass),
      price: formatPrice(holding.price, holding.assetClass),
    }));
  });

  visibleHoldings = computed(() => (this.showAll() ? this.holdings() : this.holdings().slice(0, 4)));
}
