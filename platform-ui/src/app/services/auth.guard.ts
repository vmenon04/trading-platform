import { inject } from '@angular/core';
import { CanActivateFn, RedirectFunction, Router } from '@angular/router';
import { AuthService, Role } from './auth.service';

// Sends logged-out users to /login, remembering where they were going.
export const authGuard: CanActivateFn = (_route, state) => {
  if (inject(AuthService).isLoggedIn()) {
    return true;
  }
  return inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

// Lets in only users with `role`: a client never sees the analyst or admin pages, and staff never see the
// trading pages. Anyone else is sent to their own home page (logged-out users to /login, like authGuard).
export function roleGuard(role: Role): CanActivateFn {
  return (route, state) => {
    const auth = inject(AuthService);
    if (!auth.isLoggedIn()) {
      return authGuard(route, state);
    }
    return auth.role() === role ? true : inject(Router).parseUrl(auth.homeUrl());
  };
}

// "/" opens the logged-in user's home page: the dashboard for clients, the analyst or admin pages for staff
export const homeRedirect: RedirectFunction = () => inject(AuthService).homeUrl();

// "/" shows the landing page only to logged-out visitors; logged-in users go to their home page as before
export const landingGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() ? inject(Router).parseUrl(auth.homeUrl()) : true;
};
