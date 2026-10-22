import { Component, computed, inject, input } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { dateTime, minutesAgo } from '../core/format';
import { StatusService } from '../core/status.service';

export interface AreaLink {
  path: string;
  label: string;
}

/**
 * The frame around one area of the app (analyst or admin): its title, its own
 * tabs, and how fresh the data is. Areas never link to each other.
 * `area` and `links` come from the route's data (see app.routes.ts).
 */
@Component({
  selector: 'app-area-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header>
      <h1>F-Trade {{ area() }}</h1>
      @if (links().length > 1) {
        <nav>
          @for (link of links(); track link.path) {
            <a [routerLink]="link.path" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }"
               ariaCurrentWhenActive="page">{{ link.label }}</a>
          }
        </nav>
      }
      <p class="freshness" [class.stale]="stale()">{{ freshness() }}</p>
    </header>
    <router-outlet />
  `,
  styles: `
    header { display: flex; flex-wrap: wrap; align-items: baseline; gap: 8px 16px; margin-bottom: 20px; }
    h1 { font-size: 22px; margin: 0; }
    nav { display: flex; flex-wrap: wrap; gap: 4px; }
    nav a {
      color: var(--text-secondary); text-decoration: none;
      padding: 4px 10px; border-radius: 6px; border: 1px solid transparent;
    }
    nav a:hover { background: var(--hover); }
    nav a.active { color: var(--text); background: var(--surface); border-color: var(--border); font-weight: 600; }
    .freshness { margin: 0 0 0 auto; color: var(--text-secondary); }
    .freshness::before { content: "●"; color: var(--good); margin-right: 6px; }
    .freshness.stale::before { content: "▲"; color: var(--warning); }
  `,
})
export class AreaLayout {
  readonly area = input.required<string>();
  // the router sets an input missing from the route data to undefined, so default it here
  readonly links = input<AreaLink[], AreaLink[] | undefined>([], { transform: (links) => links ?? [] });

  private readonly status = inject(StatusService).status;

  protected readonly stale = computed(() => !this.status.hasValue() || this.status.value().stale);

  protected readonly freshness = computed(() => {
    if (this.status.error()) return 'Could not reach the reporting API — is reporting/api.py running?';
    if (!this.status.hasValue()) return 'Checking data…';
    const sync = this.status.value().last_sync;
    if (!sync) return 'No data yet — run reporting/sync.py to fill the analytics database.';
    const asOf = `${dateTime(sync.finished_at)} (${minutesAgo(sync.finished_at)})`;
    return this.status.value().stale
      ? `Data may be out of date — last refreshed ${asOf}; it should refresh hourly`
      : `Data as of ${asOf} · refreshes hourly`;
  });
}
