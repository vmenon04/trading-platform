import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';

import { AuthService } from './auth.service';

/**
 * Adds "Authorization: Bearer <access token>" to every /api call. Access tokens last 15 minutes,
 * so on a 401 it refreshes once and retries; if that fails the user is sent back to /login.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api/') || request.url.startsWith('/api/auth/')) {
    return next(request);
  }
  const auth = inject(AuthService);

  return next(withToken(request, auth.accessToken())).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }
      return from(refreshOnce(auth)).pipe(
        switchMap((token) => {
          if (!token) {
            auth.logout();
            return throwError(() => error);
          }
          return next(withToken(request, token));
        }),
      );
    }),
  );
};

function withToken(request: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
}

// A page loads several charts at once, so several requests can get a 401 together. They share one
// refresh, because each refresh token works only once.
let refreshing: Promise<string | null> | null = null;

function refreshOnce(auth: AuthService): Promise<string | null> {
  refreshing ??= auth.refresh().finally(() => (refreshing = null));
  return refreshing;
}
