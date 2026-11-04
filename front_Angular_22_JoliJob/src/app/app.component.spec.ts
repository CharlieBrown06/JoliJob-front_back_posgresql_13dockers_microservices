import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app.component';
import { AuthService } from './auth/auth.service';
import { PageAuthentificationComponent } from './pages/page-authentification/page-authentification.component';

describe('Authentication UI', () => {
  const authenticated = signal(false);
  const busy = signal(false);
  const error = signal<string | null>(null);
  const auth = {
    authenticated,
    busy,
    error,
    username: signal('Camille'),
    login: vi.fn(),
    logout: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    authenticated.set(false);
    busy.set(false);
    error.set(null);
    TestBed.configureTestingModule({
      imports: [AppComponent, PageAuthentificationComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
  });

  it('offers Keycloak login without collecting credentials', () => {
    const fixture = TestBed.createComponent(PageAuthentificationComponent);
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('input')).toBeNull();
    element.querySelector('button')!.click();
    expect(auth.login).toHaveBeenCalledOnce();
  });

  it('shows the authenticated user and triggers logout', () => {
    authenticated.set(true);
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('Camille');
    const logout = Array.from(element.querySelectorAll('button'))
      .find(button => button.textContent?.includes('Se déconnecter'));
    expect(logout).toBeDefined();
    logout!.click();
    expect(auth.logout).toHaveBeenCalledOnce();
    expect(element.textContent).not.toContain('AUTH_MOCK');
  });

  it('displays errors and disables login while redirecting', () => {
    error.set('Keycloak indisponible');
    busy.set(true);
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Keycloak indisponible');
    const login = Array.from(element.querySelectorAll('button'))
      .find(button => button.textContent?.includes('Se connecter'));
    expect(login?.disabled).toBe(true);
  });
});
