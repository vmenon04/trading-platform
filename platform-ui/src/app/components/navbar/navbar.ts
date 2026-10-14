import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AccountSelector } from '../account-selector/account-selector';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive, AccountSelector],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class Navbar {
  constructor(private readonly router: Router) {}

  openProfile(): void {
    this.router.navigate(['/profile']);
  }
}
