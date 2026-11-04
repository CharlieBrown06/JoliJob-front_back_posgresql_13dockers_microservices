import { Component, Input, Output, EventEmitter } from '@angular/core';
import { ButtonComponent } from '../../atoms/button/button.component';

@Component({
  selector: 'app-auth-form',
  imports: [ButtonComponent],
  templateUrl: './auth-form.component.html',
  styleUrl: './auth-form.component.scss',
})
export class AuthFormComponent {
  @Input() busy = false;

  @Output() submitForm = new EventEmitter<void>();

  onSubmit(): void {
    if (!this.busy) {
      this.submitForm.emit();
    }
  }
}
