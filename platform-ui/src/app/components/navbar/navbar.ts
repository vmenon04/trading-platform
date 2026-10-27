import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AccountSelector } from '../account-selector/account-selector';
import { AccountService } from '../../services/account.service';
import { AuthService } from '../../services/auth.service';

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

  protected readonly auth = inject(AuthService);
  protected readonly account = inject(AccountService);

  // e.g. "Jane Doe" -> "JD"
  protected readonly initials = computed(() =>
    this.account.clientName().split(' ').filter(Boolean).map((part) => part[0].toUpperCase()).slice(0, 2).join('') || '?');

  // analysts and admins have no client, so the profile corner shows their role instead of a name
  protected readonly staffTitle = computed(() => (this.auth.role() === 'ADMIN' ? 'Administrator' : 'Analyst'));
  protected readonly staffInitials = computed(() => (this.auth.role() === 'ADMIN' ? 'AD' : 'AN'));

  protected homeUrl(): string {
    return this.auth.homeUrl();
  }

  constructor(private readonly router: Router) {}

  openAccount(): void {
    this.router.navigate(['/account']);
  }

  // clears the login token, then goes to /login
  logout(): void {
    this.auth.logout();
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
