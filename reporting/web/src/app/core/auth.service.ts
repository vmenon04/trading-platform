import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

export type Role = 'ANALYST' | 'ADMIN';

/** What POST /api/auth/login and /api/auth/refresh answer with. */
interface Session {
  accessToken: string;
  refreshToken: string;
  roles: string[];
}

const STORAGE_KEY = 'ftrade-reporting-session';

/**
 * The logged-in user's tokens and roles. The tokens are kept in localStorage so a reload
 * doesn't log the user out; the API checks every token, so the roles here only decide
 * which pages to offer.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly session = signal<Session | null>(loadSession());

  readonly loggedIn = computed(() => this.session() !== null);
  readonly roles = computed(() => this.session()?.roles ?? []);
  readonly accessToken = () => this.session()?.accessToken ?? null;

  hasAnyRole(allowed: readonly Role[]): boolean {
    return this.roles().some((role) => (allowed as readonly string[]).includes(role));
  }

  /** Where to go after logging in: admins to orders, analysts to their charts. */
  home(): string {
    return this.hasAnyRole(['ANALYST']) ? '/analyst' : '/admin';
  }

  async login(username: string, password: string): Promise<void> {
    this.setSession(await firstValueFrom(this.http.post<Session>('/api/auth/login', { username, password })));
  }

  /** Swaps the refresh token for a new pair; returns the new access token, or null if the session has ended. */
  async refresh(): Promise<string | null> {
    const refreshToken = this.session()?.refreshToken;
    if (!refreshToken) return null;
    try {
      const session = await firstValueFrom(this.http.post<Session>('/api/auth/refresh', { refreshToken }));
      this.setSession(session);
      return session.accessToken;
    } catch {
      this.setSession(null);
      return null;
    }
  }

  logout(): void {
    this.setSession(null);
    this.router.navigate(['/login']);
  }

  private setSession(session: Session | null): void {
    this.session.set(session);
    try {
      if (session) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
      } else {
        localStorage.removeItem(STORAGE_KEY);
      }
    } catch {
      // storage unavailable (private window etc.): the login just won't survive a reload
    }
  }
}

function loadSession(): Session | null {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? (JSON.parse(stored) as Session) : null;
  } catch {
    return null;
  }
}
