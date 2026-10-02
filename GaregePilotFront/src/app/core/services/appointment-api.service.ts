import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Appointment, AppointmentRequest, AppointmentStatus } from '../models/appointment.model';

@Injectable({ providedIn: 'root' })
export class AppointmentApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = '/api/appointments';

  findRange(from: string, to: string): Observable<Appointment[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<Appointment[]>(this.endpoint, { params });
  }

  create(request: AppointmentRequest): Observable<Appointment> {
    return this.http.post<Appointment>(this.endpoint, request);
  }

  update(id: string, request: AppointmentRequest): Observable<Appointment> {
    return this.http.put<Appointment>(`${this.endpoint}/${id}`, request);
  }

  updateStatus(id: string, status: AppointmentStatus): Observable<Appointment> {
    return this.http.patch<Appointment>(`${this.endpoint}/${id}/status`, { status });
  }
}
