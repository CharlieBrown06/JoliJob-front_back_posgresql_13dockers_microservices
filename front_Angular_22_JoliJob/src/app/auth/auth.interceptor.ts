import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, from, switchMap } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const url = new URL(request.url, window.location.origin);
  // Never send the access token to another origin or outside the gateway API.
  if (url.origin !== window.location.origin || !url.pathname.startsWith('/api/v1/')) {
    return next(request);
  }

  const auth = inject(AuthService);
  return from(auth.accessToken()).pipe(
    switchMap(token => next(token
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request).pipe(
        catchError(async (error: unknown) => {
          if (token && error instanceof HttpErrorResponse && error.status === 401) {
            await auth.sessionRejected();
          }
          throw error;
        }),
      )),
  );
};
