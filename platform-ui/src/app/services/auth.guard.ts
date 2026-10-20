import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

// Sends logged-out users to /login, remembering where they were going.
export const authGuard: CanActivateFn = (_route, state) => {
  if (inject(AuthService).isLoggedIn()) {
    return true;
  }
  return inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};
