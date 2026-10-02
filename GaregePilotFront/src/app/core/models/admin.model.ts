import { InvoiceStatus } from './invoice.model';
import { Page } from './client.model';

export interface AdminOverview {
  garages: number;
  activeGarages: number;
  users: number;
  clients?: number;
  vehicles?: number;
  invoices: number;
  platformRevenue: number;
  outstandingTotal: number;
  companies: Page<AdminGarageSummary>;
}

export interface AdminGarageSummary {
  id: string;
  name: string;
  vatNumber?: string;
  email: string;
  city?: string;
  active: boolean;
  users: number;
  clients: number;
  vehicles: number;
  invoices: number;
  revenue: number;
  outstanding: number;
  createdAt: string;
}

export interface AdminGarageDetail {
  id: string;
  name: string;
  vatNumber?: string;
  email: string;
  phone?: string;
  address?: string;
  active: boolean;
  users: AdminUser[];
  recentInvoices: AdminInvoice[];
}

export type ManagedUserRole = 'OWNER' | 'ADMIN' | 'MECHANIC' | 'RECEPTION';

export interface AdminUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  role: ManagedUserRole | 'SUPER_ADMIN';
  active: boolean;
  createdAt: string;
}

export interface AdminUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  role: ManagedUserRole;
  password?: string;
}

export interface AdminInvoice {
  id: string;
  invoiceNumber: string;
  clientName: string;
  licensePlate: string;
  issueDate: string;
  total: number;
  balanceDue: number;
  status: InvoiceStatus;
}
