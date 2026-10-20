import { Component } from '@angular/core';

export interface InvestmentAccount {
  name: string;
  brokerage: string;
  accountNumber: string;
}

export const investmentAccounts: InvestmentAccount[] = [
  { name: 'Individual account', brokerage: 'Brokerage', accountNumber: '•••• 8024' },
  { name: 'Retirement account', brokerage: 'IRA', accountNumber: '•••• 1842' },
];

@Component({
  selector: 'app-account-selector',
  imports: [],
  templateUrl: './account-selector.html',
  styleUrl: './account-selector.css'
})
export class AccountSelector {
  accounts = investmentAccounts;

  selectedAccount = this.accounts[0];
  isOpen = false;

  toggle(): void {
    this.isOpen = !this.isOpen;
  }

  selectAccount(account: InvestmentAccount): void {
    this.selectedAccount = account;
    this.isOpen = false;
  }
}
