import { Component, computed, inject } from '@angular/core';

import { dateTime, minutesAgo } from './format';
import { StatusService } from './status.service';

/** How fresh the analytics copy is (sync.py refreshes it hourly), for the analyst and admin page headers. */
@Component({
  selector: 'app-data-freshness',
  template: `<span class="freshness" [class.stale]="stale()" [title]="detail()">{{ label() }}</span>`,
})
export class DataFreshness {
  private readonly status = inject(StatusService).status;

  protected readonly stale = computed(() => !this.status.hasValue() || this.status.value().stale);

  protected readonly label = computed(() => {
    if (this.status.error()) return '▲ Data unavailable';
    if (!this.status.hasValue()) return 'Checking data…';
    const sync = this.status.value().last_sync;
    if (!sync) return '▲ No data yet';
    return `${this.status.value().stale ? '▲ Out of date' : '● Data'} as of ${minutesAgo(sync.finished_at)}`;
  });

  protected readonly detail = computed(() => {
    if (this.status.error()) return 'Reporting data is unavailable right now. Please try again shortly.';
    const sync = this.status.hasValue() ? this.status.value().last_sync : null;
    if (!sync) return "Reporting data hasn't been loaded yet. It refreshes hourly.";
    return `Last refreshed ${dateTime(sync.finished_at)}; refreshes hourly`;
  });
}
