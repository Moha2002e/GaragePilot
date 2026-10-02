import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const accessToken = localStorage.getItem('garagepilot_access_token');
  if (!accessToken) return next(request);
  return next(request.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } }));
};
