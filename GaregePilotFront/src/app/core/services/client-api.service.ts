import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Client, Page } from '../models/client.model';

@Injectable({ providedIn: 'root' })
export class ClientApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = '/api/clients';

  findAll(page = 0, size = 20, search = ''): Observable<Page<Client>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('search', search.trim());
    return this.http.get<Page<Client>>(this.endpoint, { params });
  }

  findById(id: string): Observable<Client> {
    return this.http.get<Client>(`${this.endpoint}/${id}`);
  }
}
