import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';

@Component({
  selector: 'app-header-flipping',
  imports: [MatToolbarModule, MatButtonModule, MatIconModule, MatChipsModule, RouterLink],
  templateUrl: './header-flipping.component.html',
  styleUrl: './header-flipping.component.scss',
})
export class HeaderFlippingComponent {
  readonly auth = inject(AuthService);
  showTagsEnabled = false;

  toggleShowTags(): void {
    this.showTagsEnabled = !this.showTagsEnabled;
  }

}
