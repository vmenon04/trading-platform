import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { InstrumentQuote } from '../models/account.models';
import { AuthService } from './auth.service';
import { InstrumentService, PAGE_SIZE } from './instrument.service';

const APPLE: InstrumentQuote = { instrumentId: 1, ticker: 'AAPL', name: 'Apple Inc', price: 189.5, assetClass: 'STOCK' };
const ORACLE: InstrumentQuote = { instrumentId: 22, ticker: 'ORCL', name: 'Oracle Corporation', price: 130.16, assetClass: 'STOCK' };

describe('InstrumentService', () => {
  let service: InstrumentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { isLoggedIn: signal(true) } },
      ],
    });
    service = TestBed.inject(InstrumentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // the page the service asks for next, answered with `instruments`
  async function answerPage(q: string, instruments: InstrumentQuote[]): Promise<void> {
    TestBed.tick();
    const request = http.expectOne((req) => req.url === '/v1/instruments' && !req.params.has('search'));
    expect(request.request.params.get('q')).toBe(q);
    expect(request.request.params.get('limit')).toBe(String(PAGE_SIZE));
    request.flush(instruments);
    await TestBed.inject(ApplicationRef).whenStable();
  }

  it('asks for one page, not the whole catalogue', async () => {
    await answerPage('', [APPLE]);

    expect(service.instruments()).toEqual([APPLE]);
  });

  it('asks the backend for the instruments matching the query', async () => {
    await answerPage('', [APPLE]);

    service.query.set('oracle');
    await answerPage('oracle', [ORACLE]);

    expect(service.instruments()).toEqual([ORACLE]);
  });

  it('loads the page again once a looked-up symbol is found, since it is now in the database', async () => {
    await answerPage('', []);
    service.query.set('ORCL');
    await answerPage('ORCL', []);

    const found = service.search('ORCL');
    http.expectOne((req) => req.params.get('search') === 'ORCL').flush([ORACLE]);

    expect(await found).toEqual(ORACLE);
    await answerPage('ORCL', [ORACLE]);
  });

  it('does not load the page again when a looked-up symbol is unknown', async () => {
    await answerPage('', []);

    const found = service.search('NOPE');
    http.expectOne((req) => req.params.get('search') === 'NOPE').flush([]);

    expect(await found).toBeNull();
    TestBed.tick();
    http.expectNone('/v1/instruments');
  });

  it('refresh goes back to the first page', async () => {
    await answerPage('', [APPLE]);
    service.query.set('oracle');
    await answerPage('oracle', [ORACLE]);

    service.refresh();

    await answerPage('', [APPLE]);
  });
});
