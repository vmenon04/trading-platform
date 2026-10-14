import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrderBook } from './order-book';

describe('OrderBook', () => {
  let component: OrderBook;
  let fixture: ComponentFixture<OrderBook>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderBook],
    }).compileComponents();

    fixture = TestBed.createComponent(OrderBook);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
