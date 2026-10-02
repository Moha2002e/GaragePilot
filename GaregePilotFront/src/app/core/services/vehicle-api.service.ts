import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../models/client.model';
import { Vehicle } from '../models/vehicle.model';

@Injectable({ providedIn: 'root' })
export class VehicleApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = '/api/vehicles';

  findAll(page = 0, size = 20, search = '', clientId?: string): Observable<Page<Vehicle>> {
    let params = new HttpParams().set('page', page).set('size', size).set('search', search.trim());
    if (clientId) {
      params = params.set('clientId', clientId);
    }
    return this.http.get<Page<Vehicle>>(this.endpoint, { params });
  }

  findById(id: string): Observable<Vehicle> {
    return this.http.get<Vehicle>(`${this.endpoint}/${id}`);
  }
}
