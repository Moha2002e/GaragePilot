import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { platformAdminGuard } from './core/guards/platform-admin.guard';
import { MainLayout } from './layout/main-layout/main-layout';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'planning',
        loadComponent: () => import('./pages/planning/planning').then((m) => m.Planning),
      },
      {
        path: 'services',
        loadComponent: () => import('./pages/services/services').then((m) => m.Services),
      },
      {
        path: 'work-orders',
        loadComponent: () => import('./pages/work-orders/work-orders').then((m) => m.WorkOrders),
      },
      {
        path: 'admin',
        canActivate: [platformAdminGuard],
        loadComponent: () => import('./pages/admin/admin').then((m) => m.Admin),
      },
      {
        path: 'clients',
        loadComponent: () => import('./pages/clients/clients').then((m) => m.Clients),
      },
      {
        path: 'vehicles',
        loadComponent: () => import('./pages/vehicles/vehicles').then((m) => m.Vehicles),
      },
      {
        path: 'invoices',
        loadComponent: () => import('./pages/invoices/invoices').then((m) => m.Invoices),
      },
      {
        path: 'invoices/new',
        loadComponent: () => import('./pages/invoice-form/invoice-form').then((m) => m.InvoiceForm),
      },
      {
        path: 'invoices/:id/edit',
        loadComponent: () => import('./pages/invoice-form/invoice-form').then((m) => m.InvoiceForm),
      },
      {
        path: 'invoices/:id',
        loadComponent: () =>
          import('./pages/invoice-detail/invoice-detail').then((m) => m.InvoiceDetail),
      },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
