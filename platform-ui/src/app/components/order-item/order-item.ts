import { Component, input } from '@angular/core';
import { TickerLogo } from '../ticker-logo/ticker-logo';

export interface OrderItemData {
  symbol: string;
  name: string;
  reference: string;
  date: string;
  side: 'Buy' | 'Sell';
  quantity: string;
  price: string;
  status: string;
}

@Component({
  imports: [TickerLogo],
  selector: 'app-order-item',
  styleUrl: './order-item.css',
  templateUrl: './order-item.html',
})
export class OrderItem {
  order = input.required<OrderItemData>();
}
