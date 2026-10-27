import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { InstrumentSummary } from './instrument-summary';

describe('InstrumentSummary', () => {
  let component: InstrumentSummary;
  let fixture: ComponentFixture<InstrumentSummary>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InstrumentSummary],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(InstrumentSummary);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('instrument', {
      symbol: 'AAPL',
      name: 'Apple Inc.',
      exchange: 'NASDAQ',
      price: '$150.00',
      change: '+1.2%',
    });
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
