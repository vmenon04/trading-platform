import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LiveTrade } from '../../models/account.models';
import { AccountService } from '../../services/account.service';
import { Task } from './task';

const LIVE_URL = '/v1/accounts/acc-1/trades/live';
const AT = '2026-10-26T12:00:00Z';

function trade(id: string, changes: Partial<LiveTrade> = {}): LiveTrade {
  return {
    tradeId: id,
    ticker: 'AAPL',
    name: 'Apple Inc',
    side: 'BUY',
    quantity: 5,
    stage: 'SUBMITTED',
    submittedAt: AT,
    validatedAt: null,
    recordedAt: null,
    finishedAt: null,
    price: null,
    rejectionCode: null,
    ...changes,
  };
}

const fulfilled = (id: string) =>
  trade(id, { stage: 'FULFILLED', validatedAt: AT, recordedAt: AT, finishedAt: AT, price: 101.5 });

describe('Task', () => {
  let fixture: ComponentFixture<Task>;
  let http: HttpTestingController;
  const submissions = signal(0);

  beforeEach(async () => {
    submissions.set(0);
    await TestBed.configureTestingModule({
      imports: [Task],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AccountService, useValue: { accountId: signal('acc-1'), submissions } },
      ],
    }).compileComponents();

    vi.useFakeTimers();
    fixture = TestBed.createComponent(Task);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    vi.useRealTimers();
    http.verify();
  });

  // change detection is scheduled with timers, which are fake here, so it is run by hand
  async function settle(): Promise<void> {
    await vi.advanceTimersByTimeAsync(0);
    TestBed.tick();
    fixture.detectChanges();
  }

  async function respond(trades: LiveTrade[]): Promise<void> {
    fixture.detectChanges();
    TestBed.tick();
    http.expectOne(LIVE_URL).flush(trades);
    await settle();
  }

  function rows(): HTMLElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('.task-row'));
  }

  function status(row: HTMLElement): HTMLElement {
    return row.querySelector('.status') as HTMLElement;
  }

  it('labels the columns', async () => {
    await respond([trade('t1')]);

    const headers = Array.from(fixture.nativeElement.querySelectorAll('.task-head span')).map(
      (header) => (header as HTMLElement).textContent?.trim(),
    );
    expect(headers).toEqual(['Order', 'Side', 'Qty', 'Price', 'Status']);
  });

  it('shows the stage a trade going through has reached', async () => {
    await respond([trade('t1', { stage: 'VALIDATED', validatedAt: AT })]);

    const [row] = rows();
    expect(row.textContent).toContain('AAPL');
    expect(row.textContent).toContain('Buy');
    expect(status(row).textContent).toContain('Validated');
    expect(status(row).classList).toContain('moving');
  });

  it('shows a fulfilled trade with its price', async () => {
    await respond([trade('t1', { stage: 'PENDING', validatedAt: AT, recordedAt: AT })]);
    vi.advanceTimersByTime(1000);
    TestBed.tick();
    http.expectOne(LIVE_URL).flush([fulfilled('t1')]);
    await settle();

    const [row] = rows();
    expect(status(row).textContent).toContain('Fulfilled');
    expect(status(row).classList).not.toContain('moving');
    expect(row.textContent).toContain('$101.50');
  });

  it('shows why a trade was rejected', async () => {
    await respond([trade('t1', { stage: 'REJECTED', finishedAt: AT, rejectionCode: 'INSUFFICIENT_FUNDS' })]);

    const [row] = rows();
    expect(status(row).textContent).toContain('Rejected');
    // a tooltip on the badge, which keyboard users can reach too
    const reason = row.querySelector('.reason')!;
    expect(reason.textContent).toContain('Not enough cash');
    expect(reason.getAttribute('role')).toBe('tooltip');
    expect(status(row).getAttribute('tabindex')).toBe('0');
    expect(status(row).getAttribute('aria-describedby')).toBe(reason.id);
  });

  it('says why for each rejection code', async () => {
    await respond([
      trade('t1', { stage: 'REJECTED', rejectionCode: 'INSUFFICIENT_SHARES' }),
      trade('t2', { stage: 'REJECTED', rejectionCode: 'PRICE_UNAVAILABLE' }),
      trade('t3', { stage: 'REJECTED', rejectionCode: 'INVALID_ORDER' }),
      trade('t4', { stage: 'REJECTED', rejectionCode: 'UNKNOWN' }),
    ]);

    const text = rows().map((row) => row.textContent);
    expect(text[0]).toContain('Not enough shares');
    expect(text[1]).toContain('Price unavailable');
    expect(text[2]).toContain('Invalid order');
    expect(text[3]).toContain("Couldn't be processed");
  });

  it('keeps trades that were already finished when the page opened', async () => {
    await respond([fulfilled('t1'), trade('t2', { stage: 'REJECTED', rejectionCode: 'UNKNOWN' })]);

    expect(rows().length).toBe(2);
    expect(status(rows()[0]).textContent).toContain('Fulfilled');
    expect(status(rows()[1]).textContent).toContain('Rejected');
  });

  it('asks again every second while a trade is going through, and stops once they have all finished', async () => {
    await respond([trade('t1')]);

    vi.advanceTimersByTime(1000);
    TestBed.tick();
    http.expectOne(LIVE_URL).flush([fulfilled('t1')]);
    await settle();

    vi.advanceTimersByTime(5000);
    http.expectNone(LIVE_URL);
  });

  it('starts asking again after an order is submitted', async () => {
    await respond([]);
    vi.advanceTimersByTime(5000);
    http.expectNone(LIVE_URL);

    submissions.set(1);
    fixture.detectChanges();
    TestBed.tick();
    http.expectOne(LIVE_URL).flush([]);
    await settle();

    // the new trade can take a moment to reach the tracker
    vi.advanceTimersByTime(1000);
    TestBed.tick();
    http.expectOne(LIVE_URL).flush([trade('t1')]);
    await settle();
    expect(rows().length).toBe(1);
  });

  it('says so when there are no trades', async () => {
    await respond([]);

    expect(fixture.nativeElement.textContent).toContain('No recent orders');
  });
});
