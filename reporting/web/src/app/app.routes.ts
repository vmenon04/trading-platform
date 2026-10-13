import { Component } from '@angular/core';
import { Routes } from '@angular/router';

import { AdminPage } from './pages/admin';
import { AnalystPage } from './pages/analyst';
import { ClientsPage } from './pages/clients';
import { InstrumentsPage } from './pages/instruments';
import { AreaLayout } from './shared/area-layout';

/** Any address outside the two areas. Deliberately links to neither. */
@Component({
  selector: 'app-not-found',
  template: `<h1>Page not found</h1><p class="lead">This address isn't part of the dashboard.</p>`,
})
class NotFoundPage {}

// Two separate areas. Each has its own layout and tabs, and neither links to the other.
export const routes: Routes = [
  {
    path: 'analyst',
    component: AreaLayout,
    data: {
      area: 'Analyst',
      links: [
        { path: '/analyst', label: 'Platform' },
        { path: '/analyst/clients', label: 'Clients' },
        { path: '/analyst/instruments', label: 'Instruments' },
      ],
    },
    children: [
      { path: '', component: AnalystPage, title: 'Platform performance · F-Trade Analyst' },
      { path: 'clients', component: ClientsPage, title: 'Clients · F-Trade Analyst' },
      { path: 'instruments', component: InstrumentsPage, title: 'Instruments · F-Trade Analyst' },
    ],
  },
  {
    path: 'admin',
    component: AreaLayout,
    data: { area: 'Admin' },
    children: [
      { path: '', component: AdminPage, title: 'Order history · F-Trade Admin' },
    ],
  },
  { path: '**', component: NotFoundPage, title: 'Page not found' },
];
