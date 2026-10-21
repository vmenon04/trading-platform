import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TickerLogo } from '../ticker-logo/ticker-logo';
import { AccountService } from '../../services/account.service';

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

// how many of the account's latest orders to show next to the order form
const RECENT_ORDERS = 5;

export interface TaskOrder {
  key: string;
  ticker: string;
  quantity: number;
  side: 'Buy' | 'Sell';
  price: string;
  // backend status, lower-cased for the css class: pending, accepted, rejected or fulfilled
  status: string;
}

// The account's latest orders from GET /v1/accounts/{id}/orders, newest first; AccountService reloads them
// after an order is submitted, so a new order appears here and moves from pending to fulfilled.
@Component({
  imports: [RouterLink, TickerLogo],
  selector: 'app-task',
  styleUrl: './task.css',
  templateUrl: './task.html',
})
export class Task {
  protected account = inject(AccountService);

  tasks = computed<TaskOrder[]>(() =>
    [...(this.account.orders.value() ?? [])]
      .reverse()
      .slice(0, RECENT_ORDERS)
      .map((trade, index) => ({
        key: trade.tradeId ?? `${trade.ticker}-${index}`,
        ticker: trade.ticker,
        quantity: trade.quantity,
        side: trade.side === 'BUY' ? 'Buy' : 'Sell',
        price: trade.ppu === null ? '—' : usd.format(trade.ppu),
        status: trade.status.toLowerCase(),
      })),
  );
}
