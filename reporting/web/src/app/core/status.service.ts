import { httpResource } from '@angular/common/http';
import { Injectable, computed } from '@angular/core';

import { addDays } from './format';
import { SyncStatus } from './models';

/** How fresh the analytics copy is, and the date range pages start with. Loaded once. */
@Injectable({ providedIn: 'root' })
export class StatusService {
  readonly status = httpResource<SyncStatus>(() => '/api/status');

  readonly maxRows = computed(() => this.status.value()?.max_rows ?? 50);

  /** The last 12 months of data, with an inclusive end date for the To box. */
  readonly defaultRange = computed(() => {
    const status = this.status.value();
    return status ? { start: status.default_start, end: addDays(status.default_end, -1) } : null;
  });
}
