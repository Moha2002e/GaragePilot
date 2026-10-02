import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  GarageService,
  GarageServiceRequest,
  ServiceCategory,
} from '../models/garage-service.model';

@Injectable({ providedIn: 'root' })
export class GarageServiceApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = '/api/services';

  findAll(search = '', category?: ServiceCategory): Observable<GarageService[]> {
    let params = new HttpParams().set('search', search.trim());
    if (category) {
      params = params.set('category', category);
    }
    return this.http.get<GarageService[]>(this.endpoint, { params });
  }

  create(request: GarageServiceRequest): Observable<GarageService> {
    return this.http.post<GarageService>(this.endpoint, request);
  }

  update(id: string, request: GarageServiceRequest): Observable<GarageService> {
    return this.http.put<GarageService>(`${this.endpoint}/${id}`, request);
  }

  deactivate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }
}
