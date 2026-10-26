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

  // answers the buy list's request for `q` at `offset`, with `total` matches in all
  async function answerPage(q: string, page: InstrumentQuote[], offset = 0, total = page.length): Promise<void> {
    TestBed.tick();
    http.expectOne((req) => req.url === '/v1/instruments' && req.params.get('q') === q
      && req.params.get('offset') === String(offset))
      .flush(page, { headers: { 'X-Total-Count': String(total) } });
    await TestBed.inject(ApplicationRef).whenStable();
  }

  function pager(): HTMLElement | null {
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).querySelector('.pager');
  }

  function lookups(): number {
    return http.match((req) => req.params.has('search')).length;
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('reports the instrument picked, and null when switching between buy and sell clears it', () => {
    const picked: (string | null)[] = [];
    component.instrumentChange.subscribe((instrument) => picked.push(instrument?.symbol ?? null));
    const oracle = { instrumentId: 22, symbol: 'ORCL', name: 'Oracle Corporation', exchange: 'Stock', price: '$130.16', change: '' };

    component.selectInstrument(oracle);
    component.setMode('sell');

    expect(picked).toEqual(['ORCL', null]);
  });

  it('unpicks the instrument when it is clicked again', () => {
    const picked: (string | null)[] = [];
    component.instrumentChange.subscribe((instrument) => picked.push(instrument?.symbol ?? null));
    const oracle = { instrumentId: 22, symbol: 'ORCL', name: 'Oracle Corporation', exchange: 'Stock', price: '$130.16', change: '' };

    component.selectInstrument(oracle);
    component.selectInstrument({ ...oracle });

    expect(component.selectedInstrument).toBeNull();
    expect(picked).toEqual(['ORCL', null]);
  });

  it('looks up a typed symbol that the buy list for it does not have', async () => {
    await answerPage('', []);

    instruments.setQuery('ORCL');
    await answerPage('ORCL', []);

    expect(lookups()).toBe(1);
  });

  it('does not look up a symbol the buy list already has', async () => {
    await answerPage('', []);

    instruments.setQuery('ORCL');
    await answerPage('ORCL', [ORACLE]);

    expect(lookups()).toBe(0);
  });

  it('does not look up a search that cannot be a symbol', async () => {
    await answerPage('', []);

    instruments.setQuery('oracle corp');
    await answerPage('oracle corp', []);

    expect(lookups()).toBe(0);
  });

  it('does not look up the symbol from a later page, where its exact match would not be', async () => {
    await answerPage('', []);
    instruments.setQuery('MA');
    await answerPage('MA', [ORACLE], 0, 60);
    // the first page has no MA, so it was looked up there; clear that request
    http.match((req) => req.params.has('search'));

    instruments.nextPage();
    await answerPage('MA', [ORACLE], 50, 60);

    expect(lookups()).toBe(0);
  });

  it('shows where the page is in the matches, once there is more than one page', async () => {
    await answerPage('', [ORACLE], 0, 334);
    expect(pager()?.textContent).toContain('1–50 of 334');

    instruments.nextPage();
    await answerPage('', [ORACLE], 50, 334);
    expect(pager()?.textContent).toContain('51–100 of 334');
  });

  it('has no page controls when the matches fit on one page', async () => {
    await answerPage('', [ORACLE], 0, 1);
    expect(pager()).toBeNull();
  });
});
