import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

// Response of POST /auth/login and POST /auth/refresh (auth-service)
export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

const STORAGE_KEY = 'ftrade.tokens';

// Logs in against auth-service and keeps the token pair. The access token lasts 15 minutes;
// authInterceptor attaches it to /v1 calls and uses the refresh token when it expires.
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private tokens = signal<TokenPair | null>(loadTokens());

  readonly isLoggedIn = computed(() => this.tokens() !== null);

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

function loadTokens(): TokenPair | null {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? (JSON.parse(stored) as TokenPair) : null;
  } catch {
    return null;
  }
}
