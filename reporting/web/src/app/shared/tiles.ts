import { Component, input } from '@angular/core';

export interface Tile {
  label: string;
  value: string;
}

/** A row of headline numbers. */
@Component({
  selector: 'app-tiles',
  template: `
    <section class="tiles">
      @for (tile of tiles(); track tile.label) {
        <div class="tile"><span>{{ tile.label }}</span><strong>{{ tile.value }}</strong></div>
      }
    </section>
  `,
})
export class TilesComponent {
  readonly tiles = input.required<Tile[]>();
}
