import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../models/client.model';
import { Invoice, InvoiceRequest } from '../models/invoice.model';

@Injectable({ providedIn: 'root' })
export class InvoiceApiService {
  private readonly http = inject(HttpClient);

  findAll(page = 0, size = 20, search = '', status?: Invoice['status']): Observable<Page<Invoice>> {
    let params = new HttpParams().set('page', page).set('size', size).set('search', search.trim());
    if (status) params = params.set('status', status);
    return this.http.get<Page<Invoice>>('/api/invoices', { params });
  }

  findById(id: string): Observable<Invoice> {
    return this.http.get<Invoice>(`/api/invoices/${id}`);
  }

  create(request: InvoiceRequest): Observable<Invoice> {
    return this.http.post<Invoice>('/api/invoices', request);
  }

  update(id: string, request: InvoiceRequest): Observable<Invoice> {
    return this.http.put<Invoice>(`/api/invoices/${id}`, request);
  }

  downloadPdf(id: string): Observable<Blob> {
    return this.http.get(`/api/invoices/${id}/pdf`, { responseType: 'blob' });
  }
}
