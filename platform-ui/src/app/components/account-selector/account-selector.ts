import { Component } from '@angular/core';

interface InvestmentAccount {
  name: string;
  brokerage: string;
  accountNumber: string;
}

@Component({
  selector: 'app-account-selector',
  imports: [],
  templateUrl: './account-selector.html',
  styleUrl: './account-selector.css'
})
export class AccountSelector {
  accounts: InvestmentAccount[] = [
    { name: 'Individual account', brokerage: 'Brokerage', accountNumber: '•••• 8024' },
    { name: 'Retirement account', brokerage: 'IRA', accountNumber: '•••• 1842' },
  ];

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
