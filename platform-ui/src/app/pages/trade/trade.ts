import { Component } from '@angular/core';
import { OrderSubmitter } from '../../components/order-submitter/order-submitter';
import { Task } from '../../components/task/task';

@Component({
  imports: [OrderSubmitter, Task],
  selector: 'app-trade',
  styleUrl: './trade.css',
  templateUrl: './trade.html',
})
export class Trade {}
