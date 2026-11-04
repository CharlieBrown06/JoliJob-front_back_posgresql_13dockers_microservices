import { Component, inject } from '@angular/core';
import { AuthService } from '../../auth/auth.service';
import { AuthFormComponent } from '../../components/organisms/auth-form/auth-form.component';
import { ButtonComponent } from '../../components/atoms/button/button.component';

@Component({
  selector: 'app-page-authentification',
  imports: [AuthFormComponent, ButtonComponent],
  templateUrl: './page-authentification.component.html',
  styleUrl: './page-authentification.component.scss',
})
export class PageAuthentificationComponent {
  readonly auth = inject(AuthService);
}
