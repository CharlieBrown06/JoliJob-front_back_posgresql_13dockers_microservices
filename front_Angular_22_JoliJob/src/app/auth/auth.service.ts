import { Injectable, inject, signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_FACTORY } from './auth.config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly createKeycloak = inject(KEYCLOAK_FACTORY);
  private client?: Keycloak;
  private initialization?: Promise<boolean>;
  private ready = false;
  private readonly authenticatedState = signal(false);
  private readonly usernameState = signal<string | null>(null);
  private readonly busyState = signal(false);
  private readonly errorState = signal<string | null>(null);

  readonly authenticated = this.authenticatedState.asReadonly();
  readonly username = this.usernameState.asReadonly();
  readonly busy = this.busyState.asReadonly();
  readonly error = this.errorState.asReadonly();

  initialize(): Promise<boolean> {
    if (this.ready) {
      return Promise.resolve(true);
    }
    if (this.initialization) {
      return this.initialization;
    }
    this.initialization = this.initializeClient().finally(() => {
      this.initialization = undefined;
    });
    return this.initialization;
  }

  private async initializeClient(): Promise<boolean> {
    this.busyState.set(true);
    this.errorState.set(null);
    try {
      const client = this.createKeycloak();
      this.client = client;
      client.onAuthSuccess = () => this.updateSession();
      client.onAuthRefreshSuccess = () => this.updateSession();
      client.onAuthLogout = () => this.updateSession();
      client.onTokenExpired = () => {
        // The rejected refresh is displayed by accessToken; no unhandled timer rejection.
        void this.accessToken().catch(() => undefined);
      };
      await client.init({
        onLoad: 'check-sso',
        flow: 'standard',
        pkceMethod: 'S256',
        redirectUri: `${window.location.origin}/callback`,
        checkLoginIframe: false,
        messageReceiveTimeout: 5000,
      });
      this.ready = true;
      this.updateSession();
      return true;
    } catch {
      this.errorState.set('Connexion à Keycloak impossible ou retour de connexion invalide. Vérifiez le service puis réessayez.');
      return false;
    } finally {
      this.busyState.set(false);
    }
  }

  async login(): Promise<void> {
    if (this.busy() || !await this.initialize() || !this.client) {
      return;
    }
    this.busyState.set(true);
    this.errorState.set(null);
    try {
      await this.client.login({ redirectUri: `${window.location.origin}/callback` });
    } catch {
      this.errorState.set('La redirection vers Keycloak a échoué. Réessayez de vous connecter.');
    } finally {
      this.busyState.set(false);
    }
  }

  async logout(): Promise<void> {
    if (this.busy() || !this.client || !this.authenticated()) {
      return;
    }
    this.busyState.set(true);
    this.errorState.set(null);
    try {
      await this.client.logout({ redirectUri: `${window.location.origin}/` });
      await this.client.clearToken();
      this.updateSession();
    } catch {
      this.errorState.set('La déconnexion Keycloak a échoué. Votre session peut être encore active ; réessayez.');
    } finally {
      this.busyState.set(false);
    }
  }

  async accessToken(): Promise<string | undefined> {
    if (!this.client?.authenticated) {
      return undefined;
    }
    try {
      await this.client.updateToken(30);
      const token = this.client.token;
      if (!token) {
        throw new Error('Missing access token');
      }
      return token;
    } catch (error) {
      await this.client.clearToken();
      this.updateSession();
      this.errorState.set('Votre session a expiré ou ne peut plus être renouvelée. Reconnectez-vous.');
      throw error;
    }
  }

  async sessionRejected(): Promise<void> {
    await this.client?.clearToken();
    this.updateSession();
    this.errorState.set('Votre session a été refusée par le serveur. Reconnectez-vous.');
  }

  private updateSession(): void {
    const authenticated = this.client?.authenticated === true;
    this.authenticatedState.set(authenticated);
    const claims = this.client?.tokenParsed;
    const names: unknown[] = [claims?.['name'], claims?.['preferred_username'], claims?.['email']];
    const username = names.find((value): value is string => typeof value === 'string' && value.length > 0);
    this.usernameState.set(authenticated
      ? username ?? 'Utilisateur'
      : null);
  }
}
