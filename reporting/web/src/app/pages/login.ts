import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../core/auth.service';

/** Analyst and admin logins (from auth-service). `next` is the page to return to afterwards. */
@Component({
  selector: 'app-login',
  template: `
    <form class="card login" (submit)="submit($event)">
      <h1>F-Trade Analytics</h1>
      <p class="lead">Log in with an analyst or admin account.</p>
      <label class="field">
        Username
        <input name="username" autocomplete="username" required [value]="username()"
               (input)="username.set($any($event.target).value)" />
      </label>
      <label class="field">
        Password
        <input name="password" type="password" autocomplete="current-password" required [value]="password()"
               (input)="password.set($any($event.target).value)" />
      </label>
      @if (error()) {
        <p class="problem" role="alert">{{ error() }}</p>
      }
      <button type="submit" [disabled]="busy() || !username() || !password()">
        {{ busy() ? 'Logging in…' : 'Log in' }}
      </button>
    </form>
  `,
  styles: `
    .login { max-width: 360px; margin: 10vh auto 0; padding: 24px; display: flex; flex-direction: column; gap: 14px; }
    h1 { font-size: 22px; margin: 0; }
    .lead { margin: 0; }
    .problem { margin: 0; }
  `,
})
export class LoginPage {
  readonly next = input<string>();

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly username = signal('');
  protected readonly password = signal('');
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected async submit(event: Event): Promise<void> {
    event.preventDefault();
    this.busy.set(true);
    this.error.set(null);
    try {
      await this.auth.login(this.username().trim(), this.password());
      this.router.navigateByUrl(this.safeNext() ?? this.auth.home());
    } catch (error) {
      this.error.set(messageFor(error));
    } finally {
      this.busy.set(false);
    }
  }

  // only a page inside this app, never another site (e.g. ?next=//evil.example)
  private safeNext(): string | null {
    const next = this.next();
    return next && next.startsWith('/') && !next.startsWith('//') && next !== '/login' ? next : null;
  }
}

function messageFor(error: unknown): string {
  if (error instanceof HttpErrorResponse && typeof error.error?.detail === 'string') {
    return error.error.detail;   // the API's own message, e.g. "Wrong username or password"
  }
  return 'Could not log in. Is reporting/api.py running?';
}
