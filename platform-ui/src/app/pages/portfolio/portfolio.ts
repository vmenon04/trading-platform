import { Component } from '@angular/core';
import { FinancialCard } from '../../components/financial-card/financial-card';
import { HoldingsSummary } from '../../components/holdings-summary/holdings-summary';

@Component({
  imports: [FinancialCard, HoldingsSummary],
  selector: 'app-portfolio',
  styleUrl: './portfolio.css',
  templateUrl: './portfolio.html',
})
export class Portfolio {}
