import { Component, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonComponent } from '../../atoms/button/button.component';

export type CookieConsentAction = 'accept' | 'refuse' | 'configure';

@Component({
  selector: 'app-cookie-consent',
  imports: [CommonModule, ButtonComponent],
  templateUrl: './cookie-consent.component.html',
  styleUrl: './cookie-consent.component.scss',
})
export class CookieConsentComponent {
  @Output() consentAction = new EventEmitter<CookieConsentAction>();

  onConfigure(): void {
    this.consentAction.emit('configure');
  }

  onRefuse(): void {
    this.consentAction.emit('refuse');
  }

  onAccept(): void {
    this.consentAction.emit('accept');
  }
}
