import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, effect, inject, input, model, signal } from '@angular/core';

import { cleanParams } from '../core/format';

let nextId = 0;

/**
 * A type-ahead box. Suggestions come from the API; picking one sets `selected`
 * to its id. Options end in "(#123)", which is how a pick is told apart from typing.
 * Setting `selected` to null from outside empties the box.
 */
@Component({
  selector: 'app-search-box',
  template: `
    <label class="grow">{{ label() }}
      <span class="search">
        <input [attr.list]="listId" [placeholder]="placeholder()" [value]="text()" (input)="typed($event)">
        @if (selected() !== null) {
          <button type="button" (click)="selected.set(null)">Clear</button>
        }
      </span>
      <datalist [id]="listId">
        @for (option of options(); track option) {
          <option [value]="option"></option>
        }
      </datalist>
    </label>
  `,
  styles: `
    :host { display: contents; }
    .search { display: flex; gap: 6px; }
    .search input { flex: 1; min-width: 0; }
  `,
})
export class SearchBoxComponent<T> {
  readonly label = input.required<string>();
  readonly placeholder = input('');
  readonly url = input.required<string>();
  readonly params = input<Record<string, string | null>>({});
  readonly describe = input.required<(match: T) => string>();
  readonly selected = model<number | null>(null);

  protected readonly listId = `search-${nextId++}`;
  protected readonly text = signal('');
  protected readonly options = signal<string[]>([]);

  private readonly http = inject(HttpClient);
  private timer?: ReturnType<typeof setTimeout>;

  constructor() {
    effect(() => {
      if (this.selected() === null) this.text.set('');
    });
    inject(DestroyRef).onDestroy(() => clearTimeout(this.timer));
  }

  protected typed(event: Event): void {
    clearTimeout(this.timer);
    const text = (event.target as HTMLInputElement).value;
    this.text.set(text);

    const picked = text.match(/\(#(\d+)\)$/);
    if (picked) {
      this.selected.set(Number(picked[1]));
      return;
    }
    if (text.trim() === '') {
      this.selected.set(null);
      return;
    }
    this.timer = setTimeout(() => {
      this.http
        .get<T[]>(this.url(), { params: cleanParams({ q: text.trim(), limit: 20, ...this.params() }) })
        .subscribe((matches) => this.options.set(matches.map(this.describe())));
    }, 250);
  }
}
