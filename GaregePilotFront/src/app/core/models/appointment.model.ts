export type AppointmentStatus =
  'PLANNED' | 'CONFIRMED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export interface Appointment {
  id: string;
  clientId: string;
  clientName: string;
  vehicleId: string;
  licensePlate: string;
  vehicleDescription: string;
  startDateTime: string;
  endDateTime: string;
  reason: string;
  notes?: string;
  status: AppointmentStatus;
}

export interface AppointmentRequest {
  clientId: string;
  vehicleId: string;
  startDateTime: string;
  endDateTime: string;
  reason: string;
  notes?: string;
  status: AppointmentStatus;
}
