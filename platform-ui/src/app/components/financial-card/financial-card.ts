import { Component, input } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-financial-card',
  styleUrl: './financial-card.css',
  templateUrl: './financial-card.html',
})
export class FinancialCard {
  label = input('');
  value = input('');
  change = input('');
  // colours the value: green for a gain, red for a loss; null leaves it neutral
  tone = input<'positive' | 'negative' | null>(null);
}
