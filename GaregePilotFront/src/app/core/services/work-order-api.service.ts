import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../models/client.model';
import { WorkOrder, WorkOrderRequest, WorkOrderStatus } from '../models/work-order.model';

@Injectable({ providedIn: 'root' })
export class WorkOrderApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = '/api/work-orders';

  findAll(page = 0, size = 10, search = '', status?: WorkOrderStatus): Observable<Page<WorkOrder>> {
    let params = new HttpParams().set('page', page).set('size', size).set('search', search.trim());
    if (status) params = params.set('status', status);
    return this.http.get<Page<WorkOrder>>(this.endpoint, { params });
  }

  findById(id: string): Observable<WorkOrder> {
    return this.http.get<WorkOrder>(`${this.endpoint}/${id}`);
  }

  create(request: WorkOrderRequest): Observable<WorkOrder> {
    return this.http.post<WorkOrder>(this.endpoint, request);
  }

  update(id: string, request: WorkOrderRequest): Observable<WorkOrder> {
    return this.http.put<WorkOrder>(`${this.endpoint}/${id}`, request);
  }

  updateStatus(id: string, status: WorkOrderStatus): Observable<WorkOrder> {
    return this.http.patch<WorkOrder>(`${this.endpoint}/${id}/status`, { status });
  }
}
