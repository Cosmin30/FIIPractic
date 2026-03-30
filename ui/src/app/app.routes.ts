import { Routes } from '@angular/router';
import { adminGuard } from './core/admin.guard';
import { authGuard } from './core/auth.guard';
import { AppShellComponent } from './layout/app-shell/app-shell.component';
import { AdminPortfoliosPageComponent } from './pages/admin-portfolios-page/admin-portfolios-page.component';
import { LoginPageComponent } from './pages/login-page/login-page.component';
import { PortfoliosPageComponent } from './pages/portfolios-page/portfolios-page.component';
import { StocksPageComponent } from './pages/stocks-page/stocks-page.component';
import { UserPageComponent } from './pages/user-page/user-page.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginPageComponent
  },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'stocks' },
      { path: 'stocks', component: StocksPageComponent },
      { path: 'portfolios', component: PortfoliosPageComponent },
      { path: 'utilizator', component: UserPageComponent },
      {
        path: 'admin/portfolios',
        component: AdminPortfoliosPageComponent,
        canActivate: [adminGuard]
      }
    ]
  },
  { path: '**', redirectTo: 'stocks' }
];
