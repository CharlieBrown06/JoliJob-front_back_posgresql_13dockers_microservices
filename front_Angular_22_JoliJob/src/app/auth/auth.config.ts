import { InjectionToken } from '@angular/core';
import Keycloak from 'keycloak-js';

export const AUTH_CONFIG = {
  url: 'http://localhost:8081',
  realm: 'jolijob',
  clientId: 'jolijob-web',
};

export const KEYCLOAK_FACTORY = new InjectionToken<() => Keycloak>('KEYCLOAK_FACTORY', {
  providedIn: 'root',
  factory: () => () => new Keycloak(AUTH_CONFIG),
});
