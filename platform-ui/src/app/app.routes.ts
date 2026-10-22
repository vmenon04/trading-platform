import { Routes } from '@angular/router';
import { Dashboard } from './pages/dashboard/dashboard';
import { Login } from './pages/login/login';

import { Registration } from './pages/registration/registration';
import { homeRedirect, roleGuard } from './services/auth.guard';

// each role has its own pages: clients trade, analysts look at clients and instruments, admins at every order
const client = roleGuard('CLIENT');
const analyst = roleGuard('ANALYST');
const admin = roleGuard('ADMIN');

export const routes: Routes = [
    { path: '', redirectTo: homeRedirect, pathMatch: 'full' },
    { path: 'dashboard', component: Dashboard, canActivate: [client] },
    { path: 'trade', loadComponent: () => import('./pages/trade/trade').then((m) => m.Trade), canActivate: [client] },
    { path: 'login', component: Login },
    { path: 'orders', loadComponent: () => import('./pages/orders/orders').then((m) => m.Orders), canActivate: [client] },
    { path: 'portfolio', loadComponent: () => import('./pages/portfolio/portfolio').then((m) => m.Portfolio), canActivate: [client] },
    { path: 'profile', loadComponent: () => import('./pages/profile/profile').then((m) => m.Profile), canActivate: [client] },
    { path: 'account', loadComponent: () => import('./pages/account/account').then((m) => m.Account), canActivate: [client] },
    { path: 'registration', component: Registration },

    { path: 'analyst', redirectTo: 'analyst/clients', pathMatch: 'full' },
    {
        path: 'analyst/clients',
        loadComponent: () => import('./pages/analyst-clients/analyst-clients').then((m) => m.AnalystClients),
        canActivate: [analyst],
        title: 'Client analysis · F-Trade',
    },
    {
        path: 'analyst/instruments',
        loadComponent: () => import('./pages/analyst-instruments/analyst-instruments').then((m) => m.AnalystInstruments),
        canActivate: [analyst],
        title: 'Instruments · F-Trade',
    },

    { path: 'admin', redirectTo: 'admin/orders', pathMatch: 'full' },
    {
        path: 'admin/orders',
        loadComponent: () => import('./pages/admin-orders/admin-orders').then((m) => m.AdminOrders),
        canActivate: [admin],
        title: 'Order history · F-Trade',
    },

    { path: '**', redirectTo: homeRedirect },
];
