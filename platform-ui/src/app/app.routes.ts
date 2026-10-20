import { Routes } from '@angular/router';
import { Dashboard } from './pages/dashboard/dashboard';
import { Trade } from './pages/trade/trade';
import { Login } from './pages/login/login';
import { Orders } from './pages/orders/orders';
import { Portfolio } from './pages/portfolio/portfolio';
import { Profile } from './pages/profile/profile';
import { Registration } from './pages/registration/registration';

export const routes: Routes = [ 
    { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    { path: 'dashboard', component: Dashboard },
    { path: 'trade', component: Trade },
    { path: 'login', component: Login },
    { path: 'orders', component: Orders },
    { path: 'portfolio', component: Portfolio },
    { path: 'profile', component: Profile },
    { path: 'registration', component: Registration },];
