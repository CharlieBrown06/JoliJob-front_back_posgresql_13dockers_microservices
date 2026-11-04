import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CookieConsentComponent, CookieConsentAction } from '../../components/organisms/cookie-consent/cookie-consent.component';

@Component({
  selector: 'app-page-respect-vie-privee',
  imports: [CookieConsentComponent],
  templateUrl: './page-respect-vie-privee.component.html',
  styleUrl: './page-respect-vie-privee.component.scss',
})
export class PageRespectViePriveeComponent {
  constructor(private router: Router) {}

  onConsentAction(action: CookieConsentAction): void {
    switch (action) {
      case 'configure':
        console.log('Paramétrer les cookies');
        break;
      case 'refuse':
        console.log('Cookies refusés');
        this.router.navigate(['/auth']);
        break;
      case 'accept':
        console.log('Cookies acceptés');
        this.router.navigate(['/auth']);
        break;
    }
  }
}
