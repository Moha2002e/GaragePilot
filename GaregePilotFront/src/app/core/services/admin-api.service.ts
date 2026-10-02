import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { delay, Observable, of } from 'rxjs';
import {
  AdminGarageDetail,
  AdminOverview,
  AdminUser,
  AdminUserRequest,
} from '../models/admin.model';
import { ADMIN_MOCK_ENABLED, AdminMockStore } from '../mocks/admin.mock';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = '/api/admin';
  private readonly mock = new AdminMockStore();

  overview(page = 0, size = 10, search = ''): Observable<AdminOverview> {
    if (ADMIN_MOCK_ENABLED) return of(this.mock.overview(page, size, search)).pipe(delay(180));
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('search', search.trim());
    return this.http.get<AdminOverview>(`${this.endpoint}/overview`, { params });
  }

  garage(id: string): Observable<AdminGarageDetail> {
    if (ADMIN_MOCK_ENABLED) return of(this.mock.garage(id)).pipe(delay(120));
    return this.http.get<AdminGarageDetail>(`${this.endpoint}/garages/${id}`);
  }

  setGarageActive(id: string, active: boolean): Observable<void> {
    if (ADMIN_MOCK_ENABLED) {
      this.mock.setGarageActive(id, active);
      return of(undefined).pipe(delay(120));
    }
    return this.http.patch<void>(`${this.endpoint}/garages/${id}/status`, { active });
  }

  createUser(garageId: string, request: AdminUserRequest): Observable<AdminUser> {
    if (ADMIN_MOCK_ENABLED) return of(this.mock.createUser(garageId, request)).pipe(delay(120));
    return this.http.post<AdminUser>(`${this.endpoint}/garages/${garageId}/users`, request);
  }

  updateUser(userId: string, request: AdminUserRequest): Observable<AdminUser> {
    if (ADMIN_MOCK_ENABLED) return of(this.mock.updateUser(userId, request)).pipe(delay(120));
    return this.http.put<AdminUser>(`${this.endpoint}/users/${userId}`, request);
  }

  setUserActive(userId: string, active: boolean): Observable<void> {
    if (ADMIN_MOCK_ENABLED) {
      this.mock.setUserActive(userId, active);
      return of(undefined).pipe(delay(120));
    }
    return this.http.patch<void>(`${this.endpoint}/users/${userId}/status`, { active });
  }
}
