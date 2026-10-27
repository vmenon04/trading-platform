import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { OrderItem, OrderItemData } from '../../components/order-item/order-item';
import { Trade } from '../../models/account.models';
import { AccountService } from '../../services/account.service';

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const shares = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const timestamp = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' });

function csvCell(value: string | number | null): string {
  if (value === null) {
    return '';
  }
  let text = String(value);
  if (typeof value === 'string' && /^[\s]*[=+@\-\t\r]/.test(text)) {
    text = `'${text}`;
  }
  return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

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
  exporting = signal(false);

  private orderedTrades = computed<Trade[]>(() =>
    [...(this.account.orders.value() ?? [])].sort((a, b) => {
      const parsedATime = a.tradeTime ? Date.parse(a.tradeTime) : Number.NaN;
      const parsedBTime = b.tradeTime ? Date.parse(b.tradeTime) : Number.NaN;
      const aTime = Number.isNaN(parsedATime) ? Number.NEGATIVE_INFINITY : parsedATime;
      const bTime = Number.isNaN(parsedBTime) ? Number.NEGATIVE_INFINITY : parsedBTime;
      return bTime - aTime;
    }),
  );

  // The backend filters by side; sort its results newest first for display and export.
  visibleOrders = computed<OrderItemData[]>(() =>
    this.orderedTrades().map((trade: Trade) => ({
      symbol: trade.ticker,
      name: trade.name,
      // tradeId is null until the backend can read UUIDs from account_trades
      reference: trade.tradeId ? trade.tradeId.slice(0, 8) : '—',
      timestamp: this.formatTimestamp(trade.tradeTime),
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

  async exportHistory(): Promise<void> {
    const accountId = this.account.accountId();
    const allTrades = this.orderedTrades();
    const side = this.activeFilter === 'Buy' ? 'BUY' : this.activeFilter === 'Sell' ? 'SELL' : null;
    const trades = side ? allTrades.filter((trade: Trade) => trade.side === side) : allTrades;
    if (!accountId || this.account.orders.isLoading() || this.account.orders.error() || trades.length === 0 || this.exporting()) {
      return;
    }

    this.exporting.set(true);
    try {
      // Yield once so the button can render its progress state before preparing the download.
      await new Promise<void>((resolve) => window.setTimeout(resolve, 0));
      const headers = ['Account', 'Order', 'Investment', 'Side', 'Quantity', 'Price', 'Status'];
      const rows = trades.map((trade: Trade) => [
        accountId,
        trade.tradeId,
        `${trade.ticker} - ${trade.name}`,
        trade.side,
        trade.quantity,
        trade.ppu,
        trade.status,
      ].map(csvCell).join(','));
      const csv = [headers.map(csvCell).join(','), ...rows].join('\r\n');
      const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      const date = new Date().toISOString().slice(0, 10);
      const safeAccountId = accountId.replace(/[^a-z0-9_-]/gi, '_');
      link.href = url;
      link.download = `${safeAccountId}_TRADE_SUMMARY_${date}.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 0);
      await new Promise<void>((resolve) => window.setTimeout(resolve, 500));
    } finally {
      this.exporting.set(false);
    }
  }

  private formatTimestamp(value: string | null): string {
    if (!value) {
      return '—';
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '—' : timestamp.format(date);
  }
}
