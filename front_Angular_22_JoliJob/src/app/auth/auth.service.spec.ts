import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_FACTORY, AUTH_CONFIG } from './auth.config';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let client: Keycloak;
  let auth: AuthService;

  beforeEach(() => {
    client = new Keycloak(AUTH_CONFIG);
    vi.spyOn(client, 'init').mockResolvedValue(false);
    vi.spyOn(client, 'login').mockResolvedValue(undefined);
    vi.spyOn(client, 'logout').mockResolvedValue(undefined);
    vi.spyOn(client, 'updateToken').mockResolvedValue(false);
    vi.spyOn(client, 'clearToken').mockImplementation(async () => {
      client.authenticated = false;
      client.token = undefined;
      client.onAuthLogout?.();
    });
    TestBed.configureTestingModule({
      providers: [{ provide: KEYCLOAK_FACTORY, useValue: () => client }],
    });
    auth = TestBed.inject(AuthService);
  });

  async function signIn(): Promise<void> {
    client.authenticated = true;
    client.token = 'access-token';
    client.tokenParsed = { sub: 'user-id', preferred_username: 'Camille' };
    vi.mocked(client.init).mockResolvedValue(true);
    await auth.initialize();
  }

  it('initializes once with code flow, PKCE S256 and the registered callback', async () => {
    await Promise.all([auth.initialize(), auth.initialize()]);
    expect(client.init).toHaveBeenCalledExactlyOnceWith(expect.objectContaining({
      onLoad: 'check-sso',
      flow: 'standard',
      pkceMethod: 'S256',
      redirectUri: `${window.location.origin}/callback`,
      checkLoginIframe: false,
    }));
    expect(auth.authenticated()).toBe(false);
  });

  it('uses the existing SSO session and restores the displayed identity', async () => {
    await signIn();
    expect(auth.authenticated()).toBe(true);
    expect(auth.username()).toBe('Camille');
  });

  it('redirects login to the registered callback', async () => {
    await auth.login();
    expect(client.login).toHaveBeenCalledWith({ redirectUri: `${window.location.origin}/callback` });
  });

  it('ends the Keycloak session and redirects logout to the registered root', async () => {
    await signIn();
    await auth.logout();
    expect(client.logout).toHaveBeenCalledWith({ redirectUri: `${window.location.origin}/` });
    expect(client.clearToken).toHaveBeenCalled();
    expect(auth.authenticated()).toBe(false);
    expect(auth.username()).toBeNull();
  });

  it('refreshes the token before returning it', async () => {
    await signIn();
    vi.mocked(client.updateToken).mockImplementation(async () => {
      client.token = 'renewed-token';
      return true;
    });
    await expect(auth.accessToken()).resolves.toBe('renewed-token');
    expect(client.updateToken).toHaveBeenCalledWith(30);
  });

  it('does not request a token for an anonymous visitor', async () => {
    await auth.initialize();
    await expect(auth.accessToken()).resolves.toBeUndefined();
    expect(client.updateToken).not.toHaveBeenCalled();
  });

  it('clears an expired session and propagates refresh failures', async () => {
    await signIn();
    vi.mocked(client.updateToken).mockRejectedValue(new Error('refresh failed'));
    await expect(auth.accessToken()).rejects.toThrow('refresh failed');
    expect(auth.authenticated()).toBe(false);
    expect(auth.error()).toContain('Reconnectez-vous');
  });

  it('refreshes in the background when the token expires', async () => {
    await signIn();
    client.onTokenExpired?.();
    await vi.waitFor(() => expect(client.updateToken).toHaveBeenCalledWith(30));
  });

  it('reports an initialization failure and allows retrying with a new adapter', async () => {
    vi.mocked(client.init).mockRejectedValueOnce(new Error('unavailable'));
    await expect(auth.initialize()).resolves.toBe(false);
    expect(auth.error()).toContain('Keycloak');
    expect(auth.busy()).toBe(false);
    await auth.login();
    expect(client.init).toHaveBeenCalledTimes(2);
    expect(client.login).toHaveBeenCalledOnce();
  });

  it('reports a rejected login redirect', async () => {
    vi.mocked(client.login).mockRejectedValue(new Error('navigation failed'));
    await auth.login();
    expect(auth.error()).toContain('redirection');
    expect(auth.busy()).toBe(false);
  });

  it('does not claim to end the SSO session when logout fails', async () => {
    await signIn();
    vi.mocked(client.logout).mockRejectedValue(new Error('logout failed'));
    await auth.logout();
    expect(auth.error()).toContain('encore active');
    expect(auth.authenticated()).toBe(true);
  });

  it('clears the session when the API rejects its token', async () => {
    await signIn();
    await auth.sessionRejected();
    expect(auth.authenticated()).toBe(false);
    expect(auth.error()).toContain('refusée');
  });
});
