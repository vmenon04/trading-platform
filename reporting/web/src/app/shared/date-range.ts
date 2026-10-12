import { Component, input, model } from '@angular/core';

import { Grain } from '../core/models';

/** From / To date boxes (To is inclusive), and optionally the Group by select. */
@Component({
  selector: 'app-date-range',
  template: `
    <label>From <input type="date" [value]="start()" (change)="start.set(value($event))"></label>
    <label>To <input type="date" [value]="end()" (change)="end.set(value($event))"></label>
    @if (showGrain()) {
      <label>Group by
        <select (change)="grain.set($any(value($event)))">
          @for (option of grains; track option) {
            <option [value]="option" [selected]="option === grain()">{{ option[0].toUpperCase() + option.slice(1) }}</option>
          }
        </select>
      </label>
    }
  `,
  styles: `:host { display: contents; }`,
})
export class DateRangeComponent {
  readonly start = model.required<string>();
  readonly end = model.required<string>();
  readonly grain = model<Grain>('month');
  readonly showGrain = input(true);

  protected readonly grains: Grain[] = ['day', 'week', 'month', 'quarter', 'year'];

  protected value(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }
}
