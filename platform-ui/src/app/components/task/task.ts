import { Component, DestroyRef, computed, effect, inject, untracked } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { TickerLogo } from '../ticker-logo/ticker-logo';
import { LiveTrade, RejectionCode, TradeStage } from '../../models/account.models';
import { AccountService } from '../../services/account.service';

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

// how often to ask again while a trade is going through
const POLL_MS = 1000;
// after an order is submitted, keep asking for this long even before it shows up: it takes a moment to be tracked
const AFTER_SUBMIT_MS = 5000;

const STAGE_LABELS: Record<TradeStage, string> = {
  SUBMITTED: 'Submitted',
  VALIDATED: 'Validated',
  PENDING: 'Pending',
  FULFILLED: 'Fulfilled',
  REJECTED: 'Rejected',
};

const REASONS: Record<RejectionCode, string> = {
  INSUFFICIENT_FUNDS: 'Not enough cash',
  INSUFFICIENT_SHARES: 'Not enough shares',
  PRICE_UNAVAILABLE: 'No price right now',
  INVALID_ORDER: 'Invalid order',
  UNKNOWN: "Couldn't be processed",
};

export interface TaskOrder {
  key: string;
  ticker: string;
  quantity: number;
  side: 'Buy' | 'Sell';
  price: string;
  // lower-cased for the css class: submitted, validated, pending, fulfilled or rejected
  status: string;
  label: string;
  // still going through
  moving: boolean;
  reason: string | null;
}

const finished = (trade: LiveTrade) => trade.stage === 'FULFILLED' || trade.stage === 'REJECTED';

// The account's orders as they go through the pipeline (GET /v1/accounts/{id}/trades/live), newest first, asked for
// every second while one is still going through. Finished ones stay listed, fulfilled and rejected alike, for as long
// as the tracker keeps them
@Component({
  imports: [RouterLink, TickerLogo],
  selector: 'app-task',
  styleUrl: './task.css',
  templateUrl: './task.html',
})
export class Task {
  private account = inject(AccountService);

  protected readonly live = httpResource<LiveTrade[]>(() => {
    const accountId = this.account.accountId();
    return accountId ? `/v1/accounts/${accountId}/trades/live` : undefined;
  });

  private pollUntil = 0;
  private timer: ReturnType<typeof setTimeout> | undefined;

  readonly tasks = computed<TaskOrder[]>(() =>
    (this.live.value() ?? [])
      .map((trade) => ({
        key: trade.tradeId,
        ticker: trade.ticker ?? '—',
        quantity: trade.quantity,
        side: trade.side === 'BUY' ? 'Buy' : 'Sell',
        price: trade.price === null ? '—' : usd.format(trade.price),
        status: trade.stage.toLowerCase(),
        label: STAGE_LABELS[trade.stage],
        moving: !finished(trade),
        reason: trade.stage === 'REJECTED' ? REASONS[trade.rejectionCode ?? 'UNKNOWN'] : null,
      })),
  );

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.timer));

    // a new order: ask now, and keep asking until it has been tracked and finished
    effect(() => {
      if (this.account.submissions() > 0) {
        this.pollUntil = Date.now() + AFTER_SUBMIT_MS;
        untracked(() => this.live.reload());
      }
    });

    // each answer: ask again while anything is moving
    effect(() => {
      const trades = this.live.value();
      if (!trades) {
        return;
      }
      clearTimeout(this.timer);
      if (trades.some((trade) => !finished(trade)) || Date.now() < this.pollUntil) {
        this.timer = setTimeout(() => this.live.reload(), POLL_MS);
      }
    });
  }
}
