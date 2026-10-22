import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrderSubmitter } from './order-submitter';

describe('OrderSubmitter', () => {
  let component: OrderSubmitter;
  let fixture: ComponentFixture<OrderSubmitter>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderSubmitter],
    }).compileComponents();

    fixture = TestBed.createComponent(OrderSubmitter);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
