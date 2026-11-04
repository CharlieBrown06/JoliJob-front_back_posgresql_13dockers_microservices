import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let requests: HttpTestingController;
  const auth = {
    accessToken: vi.fn<() => Promise<string | undefined>>(),
    sessionRejected: vi.fn(),
  };

  beforeEach(() => {
    vi.resetAllMocks();
    auth.accessToken.mockResolvedValue('fresh-token');
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpClient);
    requests = TestBed.inject(HttpTestingController);
  });

  afterEach(() => requests.verify());

  it('attaches a refreshed token to gateway requests', async () => {
    const response = firstValueFrom(http.get('/api/v1/candidates/me'));
    await vi.waitFor(() => {
      const request = requests.expectOne('/api/v1/candidates/me');
      expect(request.request.headers.get('Authorization')).toBe('Bearer fresh-token');
      request.flush({});
    });
    await response;
  });

  it.each([
    'https://other.example/api/v1/candidates/me',
    '//other.example/api/v1/candidates/me',
    '/assets/config.json',
    '/api/v10/candidates/me',
    'http://localhost:8081/realms/jolijob',
    '/api/v1/../../outside',
  ])('does not leak tokens to %s', async url => {
    const response = firstValueFrom(http.get(url));
    const request = requests.expectOne(url);
    expect(request.request.headers.has('Authorization')).toBe(false);
    expect(auth.accessToken).not.toHaveBeenCalled();
    request.flush({});
    await response;
  });

  it('keeps public requests anonymous', async () => {
    auth.accessToken.mockResolvedValue(undefined);
    const response = firstValueFrom(http.get('/api/v1/search'));
    await vi.waitFor(() => {
      const request = requests.expectOne('/api/v1/search');
      expect(request.request.headers.has('Authorization')).toBe(false);
      request.flush({});
    });
    await response;
  });

  it('does not send an API request if refreshing failed', async () => {
    auth.accessToken.mockRejectedValue(new Error('expired'));
    await expect(firstValueFrom(http.get('/api/v1/candidates/me'))).rejects.toThrow('expired');
    requests.expectNone('/api/v1/candidates/me');
  });

  it.each([401, 403])('propagates HTTP %s without replaying the request', async status => {
    const response = firstValueFrom(http.post('/api/v1/applications', {}));
    const assertion = expect(response).rejects.toMatchObject({ status });
    await vi.waitFor(() => requests.expectOne('/api/v1/applications')
      .flush({}, { status, statusText: 'Denied' }));
    await assertion;
    expect(auth.sessionRejected).toHaveBeenCalledTimes(status === 401 ? 1 : 0);
  });
});
