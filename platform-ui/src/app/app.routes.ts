import { Routes } from '@angular/router';
import { Dashboard } from './pages/dashboard/dashboard';
import { Login } from './pages/login/login';

import { Registration } from './pages/registration/registration';

export const routes: Routes = [ 
    { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    { path: 'dashboard', component: Dashboard },
    { path: 'trade', loadComponent: () => import('./pages/trade/trade').then((m) => m.Trade) },
    { path: 'login', component: Login },
    { path: 'orders', loadComponent: () => import('./pages/orders/orders').then((m) => m.Orders) },
    { path: 'portfolio', loadComponent: () => import('./pages/portfolio/portfolio').then((m) => m.Portfolio) },
    { path: 'profile', loadComponent: () => import('./pages/profile/profile').then((m) => m.Profile)},
    { path: 'registration', component: Registration },];
