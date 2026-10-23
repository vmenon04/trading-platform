import { Component, DestroyRef, ElementRef, effect, inject, input, viewChild } from '@angular/core';
import { Chart, ChartConfiguration } from 'chart.js';

/** Draws a Chart.js config, and redraws whenever the config changes. */
@Component({
  selector: 'app-chart',
  template: `<div class="chart" [style.height.px]="height()"><canvas #canvas></canvas></div>`,
  styles: `.chart { position: relative; }`,
})
export class ChartComponent {
  readonly config = input.required<ChartConfiguration<'line' | 'bar'> | null>();
  readonly height = input(280);

  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private chart?: Chart<'line' | 'bar'>;

  constructor() {
    effect(() => {
      const config = this.config();
      this.chart?.destroy();
      this.chart = config ? new Chart(this.canvas().nativeElement, config) : undefined;
    });
    inject(DestroyRef).onDestroy(() => this.chart?.destroy());
  }
}
