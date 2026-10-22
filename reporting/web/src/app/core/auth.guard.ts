import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService, Role } from './auth.service';

/**
 * Lets the router into an area only with one of the `allowed` roles. Not logged in: to /login,
 * coming back here afterwards. Logged in without the role: to the area the user does have.
 * The API enforces the same rules; this just avoids showing a page that can't load.
 */
export function requireRole(...allowed: Role[]): CanActivateFn {
  return (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);
    // a session without either dashboard role has nowhere to go here, so it is treated as logged out
    if (!auth.loggedIn() || !auth.hasAnyRole(['ANALYST', 'ADMIN'])) {
      return router.createUrlTree(['/login'], { queryParams: { next: state.url } });
    }
    return auth.hasAnyRole(allowed) ? true : router.parseUrl(auth.home());
  };
}
