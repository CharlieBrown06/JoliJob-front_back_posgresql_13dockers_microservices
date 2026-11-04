import { Routes } from '@angular/router';
import { PageAuthentificationComponent } from './pages/page-authentification/page-authentification.component';
import { PageRespectViePriveeComponent } from './pages/page-respect-vie-privee/page-respect-vie-privee.component';

export const routes: Routes = [
  { path: '', redirectTo: 'privacy', pathMatch: 'full' },
  { path: 'privacy', component: PageRespectViePriveeComponent },
  { path: 'auth', component: PageAuthentificationComponent },
  { path: 'callback', redirectTo: 'auth', pathMatch: 'full' },
];
