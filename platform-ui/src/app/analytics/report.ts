import { httpResource, HttpResourceRef } from '@angular/common/http';
import { Signal, computed, inject, linkedSignal, signal } from '@angular/core';

import { addDays, cleanParams, Range } from './format';
import { Grain } from './models';
import { StatusService } from './status.service';

type Params = Record<string, string | number | null | undefined | readonly (string | number)[]>;

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
    if (!failed) return null;
    const status = (failed.error() as { status?: number }).status;
    return status === 403
      ? "You don't have access to this page."
      : "Some data couldn't be loaded. Please try again shortly.";
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
  // the quick "last N months" picks: From moves back from To, which stays where it is
  const monthsBack = (months: number) => addDays(end(), -Math.round(months * 30.44) + 1);
  const lastMonths = (months: number) => {
    if (end()) start.set(monthsBack(months));
  };
  const isLastMonths = (months: number) => !!end() && start() === monthsBack(months);
  return { start, end, grain, range, lastMonths, isLastMonths };
}
