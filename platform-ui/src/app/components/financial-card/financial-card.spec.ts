import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FinancialCard } from './financial-card';

describe('FinancialCard', () => {
  let component: FinancialCard;
  let fixture: ComponentFixture<FinancialCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FinancialCard],
    }).compileComponents();

    fixture = TestBed.createComponent(FinancialCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
