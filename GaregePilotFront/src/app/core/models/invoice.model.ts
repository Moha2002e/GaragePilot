export type InvoiceStatus =
  'DRAFT' | 'ISSUED' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE' | 'CANCELLED';

export interface InvoiceItem {
  id: string;
  type: 'LABOR' | 'PART' | 'SERVICE' | 'OTHER';
  description: string;
  quantity: number;
  unitPrice: number;
  vatRate: number;
  totalExcludingVat: number;
}

export interface Invoice {
  id: string;
  garageName: string;
  garageVatNumber?: string;
  garageEmail: string;
  garagePhone?: string;
  garageAddress?: string;
  clientId: string;
  clientName: string;
  clientVatNumber?: string;
  clientEmail?: string;
  clientPhone?: string;
  clientAddress?: string;
  vehicleId: string;
  licensePlate: string;
  vehicleDescription: string;
  vin?: string;
  mileage?: number;
  workOrderId?: string;
  quoteId?: string;
  invoiceNumber: string;
  status: InvoiceStatus;
  issueDate: string;
  dueDate?: string;
  subtotal: number;
  vatAmount: number;
  total: number;
  amountPaid: number;
  balanceDue: number;
  items: InvoiceItem[];
}

export interface InvoiceItemRequest {
  type: InvoiceItem['type'];
  description: string;
  quantity: number;
  unitPrice: number;
  vatRate: number;
}

export interface InvoiceRequest {
  clientId: string;
  vehicleId: string;
  workOrderId?: string;
  quoteId?: string;
  issueDate: string;
  dueDate?: string;
  items: InvoiceItemRequest[];
}
