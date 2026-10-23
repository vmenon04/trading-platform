import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

// Adds "Authorization: Bearer <access token>" to every backend call: /v1 (the trade API) and /api (the
// reporting API behind the analyst and admin pages). On a 401 it refreshes the token once and retries; if
// that fails the user is logged out and sent to /login.
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/v1') && !request.url.startsWith('/api')) {
    return next(request);
  }
  const auth = inject(AuthService);

  return next(withToken(request, auth.accessToken)).pipe(
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

// several requests can fail with 401 at once; they share one refresh, since each refresh
// invalidates the previous refresh token
let refreshing: Promise<string | null> | null = null;

function refreshOnce(auth: AuthService): Promise<string | null> {
  refreshing ??= auth.refresh().finally(() => (refreshing = null));
  return refreshing;
}
