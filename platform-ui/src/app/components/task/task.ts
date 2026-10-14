import { Component } from '@angular/core';

export type TaskStatus = 'accepted' | 'validated' | 'rejected' | 'fulfilled';

export interface TaskOrder {
  ticker: string;
  quantity: number;
  side: 'Buy' | 'Sell';
  quotedPrice: string;
  status: TaskStatus;
}

@Component({
  imports: [],
  selector: 'app-task',
  styleUrl: './task.css',
  templateUrl: './task.html',
})
export class Task {
  tasks: TaskOrder[] = [
    { ticker: 'AAPL', quantity: 5, side: 'Buy', quotedPrice: '$228.42', status: 'validated' },
    { ticker: 'NVDA', quantity: 8, side: 'Buy', quotedPrice: '$138.85', status: 'accepted' },
    { ticker: 'MSFT', quantity: 4, side: 'Sell', quotedPrice: '$415.62', status: 'fulfilled' },
  ];
}
