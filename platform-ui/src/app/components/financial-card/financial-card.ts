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
}
