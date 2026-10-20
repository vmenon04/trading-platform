import { Routes } from '@angular/router';
import { Dashboard } from './pages/dashboard/dashboard';
import { Login } from './pages/login/login';

import { Registration } from './pages/registration/registration';
import { authGuard } from './services/auth.guard';

export const routes: Routes = [
    { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    { path: 'dashboard', component: Dashboard, canActivate: [authGuard] },
    { path: 'trade', loadComponent: () => import('./pages/trade/trade').then((m) => m.Trade), canActivate: [authGuard] },
    { path: 'login', component: Login },
    { path: 'orders', loadComponent: () => import('./pages/orders/orders').then((m) => m.Orders), canActivate: [authGuard] },
    { path: 'portfolio', loadComponent: () => import('./pages/portfolio/portfolio').then((m) => m.Portfolio), canActivate: [authGuard] },
    { path: 'profile', loadComponent: () => import('./pages/profile/profile').then((m) => m.Profile), canActivate: [authGuard] },
    { path: 'registration', component: Registration },];
