import { httpResource, HttpResourceRef } from '@angular/common/http';
import { Signal, computed, inject, linkedSignal, signal } from '@angular/core';

import { addDays, cleanParams, Range } from './format';
import { Grain } from './models';
import { StatusService } from './status.service';

type Params = Record<string, string | number | null | undefined | readonly string[]>;

/**
 * Fetches `url` whenever `params()` changes; returns nothing while `params()` is
 * undefined (e.g. before the default dates are known).
 */
export function report<T>(url: () => string | undefined, params: () => Params | undefined = () => ({})) {
  return httpResource<T>(() => {
    const target = url();
    const query = params();
    return target && query ? { url: target, params: cleanParams(query) } : undefined;
  });
}

/** The latest value a resource loaded, kept while it loads the next one so charts don't blank out. */
export function latest<T>(resource: HttpResourceRef<T | undefined>): Signal<T | undefined> {
  return linkedSignal<T | undefined, T | undefined>({
    source: () => (resource.hasValue() ? resource.value() : undefined),
    computation: (value, previous) => value ?? previous?.value,
  });
}

/** The first error among some resources, as a message to show. */
export function problem(...resources: HttpResourceRef<unknown>[]): Signal<string | null> {
  return computed(() => {
    const failed = resources.find((resource) => resource.error());
    return failed ? 'Could not load some of the data. Is reporting/api.py running?' : null;
  });
}

/**
 * The From / To / Group by state a chart page keeps. It starts on the default
 * 12 months once those are known; `range()` is what the API wants (end exclusive).
 */
export function dateRange() {
  const status = inject(StatusService);
  const start = linkedSignal(() => status.defaultRange()?.start ?? '');
  const end = linkedSignal(() => status.defaultRange()?.end ?? '');
  const grain = signal<Grain>('month');
  const range = computed<Range | undefined>(() =>
    start() && end() ? { start: start(), end: addDays(end(), 1), grain: grain() } : undefined,
  );
  return { start, end, grain, range };
}
