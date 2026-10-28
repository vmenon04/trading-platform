import { Component, inject, signal } from '@angular/core';
import { NewsFeed } from '../../components/news-feed/news-feed';
import { OrderSubmitter } from '../../components/order-submitter/order-submitter';
import { Task } from '../../components/task/task';
import { AccountService } from '../../services/account.service';
import { InstrumentService } from '../../services/instrument.service';

@Component({
  imports: [NewsFeed, OrderSubmitter, Task],
  selector: 'app-trade',
  styleUrl: './trade.css',
  templateUrl: './trade.html',
})
export class Trade {
  // news about the instrument picked in the order form; market news until one is picked
  readonly newsSymbol = signal<string | null>(null);

  constructor() {
    // fresh orders, cash, holdings and instrument prices each time the page opens
    inject(AccountService).refresh();
    inject(InstrumentService).refresh();
  }
}
