import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthService } from '../../services/auth.service';
import { NewsArticle, NewsFeed } from './news-feed';

describe('NewsFeed', () => {
  let fixture: ComponentFixture<NewsFeed>;
  let http: HttpTestingController;

  const article: NewsArticle = {
    id: 7,
    headline: 'Stocks rise',
    summary: 'What happened today.',
    source: 'Reuters',
    url: 'https://example.com/7',
    publishedAt: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NewsFeed],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { isLoggedIn: () => true } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NewsFeed);
    http = TestBed.inject(HttpTestingController);
  });

  it('shows market news with its source and age', async () => {
    fixture.detectChanges();
    http.expectOne((request) => request.url === '/v1/news' && !request.params.has('symbol')).flush([article]);
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Market news');
    expect(text).toContain('Stocks rise');
    expect(text).toContain('Reuters');
    expect(text).toContain('5m ago');
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('https://example.com/7');
  });

  it('asks for one company when given a symbol', async () => {
    fixture.componentRef.setInput('symbol', 'AAPL');
    fixture.detectChanges();
    http.expectOne((request) => request.url === '/v1/news' && request.params.get('symbol') === 'AAPL').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('AAPL news');
    expect(fixture.nativeElement.textContent).toContain('No recent news about AAPL.');
  });

  it('says so when news is unavailable', async () => {
    fixture.detectChanges();
    http.expectOne('/v1/news?limit=6').flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("News isn't available right now.");
  });
});
