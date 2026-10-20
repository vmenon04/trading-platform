import { Component, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AccountSelector } from '../account-selector/account-selector';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive, AccountSelector],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class Navbar {
  protected readonly showEasterEgg = signal(false);
  private logoClicks = 0;
  private logoClickTimer?: ReturnType<typeof setTimeout>;
  private easterEggTimer?: ReturnType<typeof setTimeout>;

  constructor(private readonly router: Router) {}

  openAccount(): void {
    this.router.navigate(['/account']);
  }

  logout(): void {
    this.router.navigate(['/login']);
  }

  onLogoClick(): void {
    clearTimeout(this.logoClickTimer);
    this.logoClicks++;

    if (this.logoClicks >= 5) {
      this.logoClicks = 0;
      this.showEasterEgg.set(true);
      clearTimeout(this.easterEggTimer);
      this.easterEggTimer = setTimeout(() => this.showEasterEgg.set(false), 4000);
      return;
    }

    this.logoClickTimer = setTimeout(() => (this.logoClicks = 0), 1500);
  }
}
