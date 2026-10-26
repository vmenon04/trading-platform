import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withViewTransitions } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './services/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // pages slide and fade into each other (see the view-transition rules in styles.css)
    provideRouter(routes, withViewTransitions()),
    provideHttpClient(withInterceptors([authInterceptor]))
  ]
};
