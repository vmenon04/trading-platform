import { Component, input, model, signal } from '@angular/core';

/** The "Top N" box: clamps to 1..max and says so when it had to. */
@Component({
  selector: 'app-top-n',
  template: `
    <label>
      <span>{{ label() }} <small [class.capped]="capped()">({{ capped() ? 'capped at' : 'max' }} {{ max() }})</small></span>
      <input type="number" min="1" [max]="max()" [value]="value()" (change)="pick($event)">
    </label>
  `,
  styles: `:host { display: contents; } input { width: 90px; }`,
})
export class TopNComponent {
  readonly value = model(10);
  readonly label = input('Top N');
  readonly max = input(50);

  protected readonly capped = signal(false);

  protected pick(event: Event): void {
    const box = event.target as HTMLInputElement;
    const asked = Number(box.value) || 10;
    const used = Math.min(Math.max(asked, 1), this.max());
    this.capped.set(asked > this.max());
    box.value = String(used);
    this.value.set(used);
  }
}
