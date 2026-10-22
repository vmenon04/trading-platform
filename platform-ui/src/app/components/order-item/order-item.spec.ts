import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { OrderItem } from './order-item';

describe('OrderItem', () => {
  let component: OrderItem;
  let fixture: ComponentFixture<OrderItem>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderItem],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(OrderItem);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('order', {
      symbol: 'AAPL',
      name: 'Apple Inc.',
      reference: 'T-1',
      date: '2026-10-22',
      side: 'Buy',
      quantity: '10',
      price: '$150.00',
      status: 'fulfilled',
    });
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
