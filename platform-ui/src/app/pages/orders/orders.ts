import { Component, DestroyRef, computed, inject } from '@angular/core';
import { OrderItem, OrderItemData } from '../../components/order-item/order-item';
import { AccountService } from '../../services/account.service';

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const shares = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

@Component({
  imports: [OrderItem],
  selector: 'app-orders',
  styleUrl: './orders.css',
  templateUrl: './orders.html',
})
export class Orders {
  protected account = inject(AccountService);

  constructor() {
    // the page opens on "All"; the side filter is shared with the Trade page's order list, so clear it on leaving
    this.account.orderSide.set(null);
    this.account.refresh();
    inject(DestroyRef).onDestroy(() => this.account.orderSide.set(null));
  }

  activeFilter: 'All' | 'Buy' | 'Sell' = 'All';

  // the backend filters by side, so these are already the orders to show
  visibleOrders = computed<OrderItemData[]>(() =>
    (this.account.orders.value() ?? []).map((trade) => ({
      symbol: trade.ticker,
      name: trade.name,
      // tradeId is null until the backend can read UUIDs from account_trades
      reference: trade.tradeId ? trade.tradeId.slice(0, 8) : '—',
      date: '',
      side: trade.side === 'BUY' ? 'Buy' : 'Sell',
      quantity: `${shares.format(trade.quantity)} shares`,
      price: trade.ppu === null ? '—' : usd.format(trade.ppu),
      status: trade.status,
    })),
  );

  setFilter(filter: 'All' | 'Buy' | 'Sell'): void {
    this.activeFilter = filter;
    this.account.orderSide.set(filter === 'All' ? null : filter === 'Buy' ? 'BUY' : 'SELL');
  }
}
