import { Component } from '@angular/core';
import { OrderItem, OrderItemData } from '../../components/order-item/order-item';

@Component({
  imports: [OrderItem],
  selector: 'app-orders',
  styleUrl: './orders.css',
  templateUrl: './orders.html',
})
export class Orders {
  activeFilter: 'All' | 'Buy' | 'Sell' = 'All';

  orders: OrderItemData[] = [
    { symbol: 'NVDA', name: 'NVIDIA Corp.', reference: 'FT-10482', date: 'Today, 10:42', side: 'Buy', quantity: '8 shares', price: '$138.85', status: 'Filled' },
    { symbol: 'AAPL', name: 'Apple Inc.', reference: 'FT-10476', date: 'Yesterday, 15:21', side: 'Sell', quantity: '10 shares', price: '$226.41', status: 'Filled' },
    { symbol: 'MSFT', name: 'Microsoft Corp.', reference: 'FT-10461', date: 'May 22, 11:09', side: 'Buy', quantity: '4 shares', price: '$412.18', status: 'Filled' },
  ];

  get visibleOrders(): OrderItemData[] {
    return this.activeFilter === 'All' ? this.orders : this.orders.filter((order) => order.side === this.activeFilter);
  }

  setFilter(filter: 'All' | 'Buy' | 'Sell'): void {
    this.activeFilter = filter;
  }
}
