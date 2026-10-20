import { Component, inject } from '@angular/core';
import { FinancialCard } from '../../components/financial-card/financial-card';
import { HoldingsSummary } from '../../components/holdings-summary/holdings-summary';
import { AccountService } from '../../services/account.service';

@Component({
  imports: [FinancialCard, HoldingsSummary],
  selector: 'app-dashboard',
  styleUrl: './dashboard.css',
  templateUrl: './dashboard.html',
})
export class Dashboard {
  protected account = inject(AccountService);
}
