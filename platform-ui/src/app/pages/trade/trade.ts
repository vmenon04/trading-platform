import { Component, inject } from '@angular/core';
import { OrderSubmitter } from '../../components/order-submitter/order-submitter';
import { Task } from '../../components/task/task';
import { AccountService } from '../../services/account.service';
import { InstrumentService } from '../../services/instrument.service';

@Component({
  imports: [OrderSubmitter, Task],
  selector: 'app-trade',
  styleUrl: './trade.css',
  templateUrl: './trade.html',
})
export class Trade {
  constructor() {
    // fresh orders, cash, holdings and instrument prices each time the page opens
    inject(AccountService).refresh();
    inject(InstrumentService).refresh();
  }
}
