import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, AuthenticatedUser, LoginRequest } from '../models/auth.model';

const ACCESS_TOKEN_KEY = 'garagepilot_access_token';
const USER_KEY = 'garagepilot_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', request).pipe(
      tap((response) => {
        localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
        localStorage.setItem(USER_KEY, JSON.stringify(response.user));
      }),
    );
  }

  isAuthenticated(): boolean {
    return Boolean(localStorage.getItem(ACCESS_TOKEN_KEY));
  }

  currentUser(): AuthenticatedUser | null {
    const storedUser = localStorage.getItem(USER_KEY);
    if (!storedUser) return null;

    try {
      return JSON.parse(storedUser) as AuthenticatedUser;
    } catch {
      this.clearSession();
      return null;
    }
  }

  logout(): void {
    this.clearSession();
    void this.router.navigateByUrl('/login');
  }

  private clearSession(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }
}
