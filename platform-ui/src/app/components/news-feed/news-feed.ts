import { Component, computed, inject, input } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

// a story from GET /v1/news (Finnhub, cached by the backend); publishedAt is an ISO date-time. The backend also sends
// a picture link, which isn't shown
export interface NewsArticle {
  id: number;
  headline: string;
  summary: string | null;
  source: string | null;
  url: string;
  publishedAt: string;
}

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;
const dateOnly = new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric' });

// "5m ago", "3h ago", then the date
function age(publishedAt: string, now: number): string {
  const time = new Date(publishedAt).getTime();
  const elapsed = now - time;
  if (elapsed < HOUR) {
    return `${Math.max(1, Math.round(elapsed / MINUTE))}m ago`;
  }
  if (elapsed < DAY) {
    return `${Math.round(elapsed / HOUR)}h ago`;
  }
  return dateOnly.format(time);
}

// The latest market news, or news about one company when `symbol` is set. Each story opens on the publisher's site
@Component({
  selector: 'app-news-feed',
  styleUrl: './news-feed.css',
  templateUrl: './news-feed.html',
})
export class NewsFeed {
  private auth = inject(AuthService);

  symbol = input<string | null>(null);
  limit = input(6);

  private readonly news = httpResource<NewsArticle[]>(() => {
    if (!this.auth.isLoggedIn()) {
      return undefined;
    }
    const params: Record<string, string | number> = { limit: this.limit() };
    const symbol = this.symbol();
    if (symbol) {
      params['symbol'] = symbol;
    }
    return { url: '/v1/news', params };
  });

  readonly isLoading = computed(() => this.news.isLoading());
  readonly failed = computed(() => !!this.news.error());

  readonly articles = computed(() => {
    const now = Date.now();
    return (this.news.value() ?? []).map((article) => ({ ...article, age: age(article.publishedAt, now) }));
  });
}
