import { ApplicationRef, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { InstrumentQuote } from '../../models/account.models';
import { AuthService } from '../../services/auth.service';
import { InstrumentService } from '../../services/instrument.service';
import { OrderSubmitter } from './order-submitter';

const ORACLE: InstrumentQuote = { instrumentId: 22, ticker: 'ORCL', name: 'Oracle Corporation', price: 130.16, assetClass: 'STOCK' };

describe('OrderSubmitter', () => {
  let component: OrderSubmitter;
  let fixture: ComponentFixture<OrderSubmitter>;
  let http: HttpTestingController;
  let instruments: InstrumentService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderSubmitter],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { isLoggedIn: signal(true) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OrderSubmitter);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    instruments = TestBed.inject(InstrumentService);
    fixture.detectChanges();
  });

  // answers the buy list's request for `q`
  async function answerPage(q: string, page: InstrumentQuote[]): Promise<void> {
    TestBed.tick();
    http.expectOne((req) => req.url === '/v1/instruments' && req.params.get('q') === q).flush(page);
    await TestBed.inject(ApplicationRef).whenStable();
  }

  function lookups(): number {
    return http.match((req) => req.params.has('search')).length;
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('looks up a typed symbol that the buy list for it does not have', async () => {
    await answerPage('', []);

    instruments.query.set('ORCL');
    await answerPage('ORCL', []);

    expect(lookups()).toBe(1);
  });

  it('does not look up a symbol the buy list already has', async () => {
    await answerPage('', []);

    instruments.query.set('ORCL');
    await answerPage('ORCL', [ORACLE]);

    expect(lookups()).toBe(0);
  });

  it('does not look up a search that cannot be a symbol', async () => {
    await answerPage('', []);

    instruments.query.set('oracle corp');
    await answerPage('oracle corp', []);

    expect(lookups()).toBe(0);
  });
});
