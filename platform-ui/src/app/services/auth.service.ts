import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

// Response of POST /auth/login and POST /auth/refresh (auth-service)
export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

// the roles auth-service puts in the access token. Registration only makes CLIENTs; ANALYST and ADMIN users
// are inserted into the database (src/main/resources/staff_users.sql)
export type Role = 'CLIENT' | 'ANALYST' | 'ADMIN';

// where each role lands after logging in, and what the "/" address opens
const HOME: Record<Role, string> = {
  CLIENT: '/dashboard',
  ANALYST: '/analyst/clients',
  ADMIN: '/admin/orders',
};

const STORAGE_KEY = 'ftrade.tokens';

// Logs in against auth-service and keeps the token pair. The access token lasts 15 minutes;
// authInterceptor attaches it to /v1 and /api calls and uses the refresh token when it expires.
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private tokens = signal<TokenPair | null>(loadTokens());

  readonly isLoggedIn = computed(() => this.tokens() !== null);

  // read from the access token, only to pick which pages to show: the backends check the token's signature
  // themselves, so editing it in the browser gets nothing but 401s and 403s
  readonly roles = computed(() => rolesIn(this.tokens()?.accessToken));

  // the one role the app is laid out for; a user with several gets the most senior one's pages
  readonly role = computed<Role | null>(() => {
    const roles = this.roles();
    return (['ADMIN', 'ANALYST', 'CLIENT'] as const).find((role) => roles.includes(role)) ?? null;
  });

  readonly isClient = computed(() => this.role() === 'CLIENT');

  // the first page for the logged-in user's role ("/login" when logged out)
  homeUrl(): string {
    const role = this.role();
    return role ? HOME[role] : '/login';
  }

  get accessToken(): string | null {
    return this.tokens()?.accessToken ?? null;
  }

  // POST /auth/register accepts only username + password (extra fields are rejected with a 400)
  async register(username: string, password: string): Promise<void> {
    await firstValueFrom(this.http.post('/auth/register', { username, password }));
  }

  async login(username: string, password: string): Promise<void> {
    const tokens = await firstValueFrom(this.http.post<TokenPair>('/auth/login', { username, password }));
    this.setTokens(tokens);
  }

  // Gets a new token pair; the old refresh token stops working (auth-service rotates it).
  // Resolves to the new access token, or null if the session can't be refreshed.
  async refresh(): Promise<string | null> {
    const refreshToken = this.tokens()?.refreshToken;
    if (!refreshToken) {
      return null;
    }
    try {
      const tokens = await firstValueFrom(this.http.post<TokenPair>('/auth/refresh', { refreshToken }));
      this.setTokens(tokens);
      return tokens.accessToken;
    } catch {
      return null;
    }
  }

  logout(): void {
    const refreshToken = this.tokens()?.refreshToken;
    if (refreshToken) {
      void firstValueFrom(this.http.post('/auth/logout', { refreshToken })).catch(() => {
        // ignore errors: the refresh token is invalidated on the server, so a 401 or 403 is fine
      });
    }
    this.setTokens(null);
    this.router.navigate(['/login']);
  }

  private setTokens(tokens: TokenPair | null): void {
    this.tokens.set(tokens);
    try {
      if (tokens) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(tokens));
      } else {
        localStorage.removeItem(STORAGE_KEY);
      }
    } catch {
      // storage unavailable (private window etc.): the session just won't survive a reload
    }
  }
}

// the "roles" claim of a JWT. A JWT is header.payload.signature, each part base64url-encoded JSON
function rolesIn(accessToken: string | undefined): Role[] {
  if (!accessToken) {
    return [];
  }
  try {
    const payload = accessToken.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const claims = JSON.parse(atob(payload)) as { roles?: unknown };
    return Array.isArray(claims.roles) ? (claims.roles as Role[]) : [];
  } catch {
    return [];
  }
}

function loadTokens(): TokenPair | null {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? (JSON.parse(stored) as TokenPair) : null;
  } catch {
    return null;
  }
}
