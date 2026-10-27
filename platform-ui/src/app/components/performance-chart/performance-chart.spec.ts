import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PerformancePoint } from '../../models/account.models';
import { AccountService } from '../../services/account.service';
import { PerformanceChart } from './performance-chart';

const URL = '/v1/accounts/acc-1/performance';

const POINTS: PerformancePoint[] = [
  { date: '2026-10-22', value: 200, invested: 200 },
  { date: '2026-10-23', value: 220, invested: 200 },
  { date: '2026-10-26', value: 250, invested: 200 },
];

describe('PerformanceChart', () => {
  let fixture: ComponentFixture<PerformanceChart>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PerformanceChart],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AccountService, useValue: { accountId: signal('acc-1') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PerformanceChart);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function respond(range: string, body: PerformancePoint[]): Promise<void> {
    fixture.detectChanges();
    TestBed.tick();
    http.expectOne((request) => request.url === URL && request.params.get('range') === range).flush(body);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  const element = () => fixture.nativeElement as HTMLElement;
  const button = (label: string) =>
    Array.from(element().querySelectorAll<HTMLButtonElement>('.ranges button')).find((b) => b.textContent?.trim() === label)!;

  it('asks for three months by default and draws the value and invested lines', async () => {
    await respond('THREE_MONTHS', POINTS);

    expect(element().querySelector('path.value')?.getAttribute('d')).toMatch(/^M0\.0 [\d.]+ L/);
    expect(element().querySelector('path.invested')).not.toBeNull();
    expect(button('3M').getAttribute('aria-pressed')).toBe('true');
  });

  it('shows the latest value and the gain over the period', async () => {
    await respond('THREE_MONTHS', POINTS);

    expect(element().querySelector('.headline strong')?.textContent).toContain('$250.00');
    const change = element().querySelector('.change')!;
    expect(change.textContent).toContain('+$50.00');
    expect(change.classList).toContain('up');
  });

  // money put in isn't a gain: the value rising by the cost of a new buy is no change
  it('does not count new money as a gain', async () => {
    await respond('THREE_MONTHS', [
      { date: '2026-10-22', value: 200, invested: 200 },
      { date: '2026-10-23', value: 500, invested: 500 },
    ]);

    expect(element().querySelector('.change')?.classList).toContain('flat');
  });

  it('asks again for the picked range', async () => {
    await respond('THREE_MONTHS', POINTS);

    button('1Y').click();
    await respond('ONE_YEAR', POINTS);

    expect(button('1Y').getAttribute('aria-pressed')).toBe('true');
  });

  it("shows a day's numbers on hover", async () => {
    await respond('THREE_MONTHS', POINTS);
    const svg = element().querySelector('svg')!;
    svg.getBoundingClientRect = () => ({ left: 0, width: 600, top: 0, height: 208 }) as DOMRect;

    svg.dispatchEvent(new PointerEvent('pointermove', { clientX: 300 }));
    fixture.detectChanges();

    const tip = element().querySelector('.tip')!;
    expect(tip.textContent).toContain('Oct 23, 2026');
    expect(tip.textContent).toContain('$220.00');
    expect(tip.textContent).toContain('$200.00');

    svg.dispatchEvent(new PointerEvent('pointerleave'));
    fixture.detectChanges();
    expect(element().querySelector('.tip')).toBeNull();
  });

  it('says so before the first trade', async () => {
    await respond('THREE_MONTHS', []);

    expect(element().querySelector('.chart-message')?.textContent).toContain('after your first trade');
    expect(element().querySelector('svg')).toBeNull();
  });

  it('says so when the history is unavailable', async () => {
    fixture.detectChanges();
    TestBed.tick();
    http.expectOne((request) => request.url === URL).flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element().querySelector('.chart-message')?.textContent).toContain("isn't available");
  });
});
