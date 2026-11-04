import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  imports: [],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss',
})
export class FooterComponent {
  socialLinks = [
    { name: 'Facebook', icon: 'facebook', url: '#' },
    { name: 'YouTube', icon: 'youtube', url: '#' },
    { name: 'X', icon: 'x-twitter', url: '#' },
    { name: 'TikTok', icon: 'tiktok', url: '#' },
    { name: 'Instagram', icon: 'instagram', url: '#' },
    { name: 'LinkedIn', icon: 'linkedin', url: '#' },
    { name: 'Google', icon: 'google', url: '#' },
    { name: 'Pinterest', icon: 'pinterest', url: '#' },
  ];

  legalLinks = [
    { label: 'Informations légales', url: '/legal' },
    { label: 'CGU', url: '/cgu' },
    { label: 'Politique de confidentialité', url: '/privacy' },
    { label: 'Gérer les traceurs', url: '/cookies' },
    { label: 'Accessibilité : non conforme', url: '/accessibility' },
    { label: 'Aide et contact', url: '/contact' },
  ];
}
