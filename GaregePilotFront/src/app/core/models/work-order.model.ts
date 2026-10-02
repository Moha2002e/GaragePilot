export type WorkOrderStatus =
  | 'DRAFT'
  | 'WAITING_FOR_APPROVAL'
  | 'APPROVED'
  | 'IN_PROGRESS'
  | 'WAITING_FOR_PARTS'
  | 'COMPLETED'
  | 'INVOICED'
  | 'CANCELLED';

export type WorkOrderItemType = 'LABOR' | 'PART' | 'SERVICE' | 'OTHER';

export interface WorkOrderItem {
  id: string;
  type: WorkOrderItemType;
  description: string;
  quantity: number;
  unitPrice: number;
  vatRate: number;
  totalExcludingVat: number;
}

export interface WorkOrder {
  id: string;
  clientId: string;
  clientName: string;
  vehicleId: string;
  licensePlate: string;
  vehicleDescription: string;
  appointmentId?: string;
  status: WorkOrderStatus;
  mileageIn?: number;
  mileageOut?: number;
  description: string;
  technicianNotes?: string;
  customerNotes?: string;
  startedAt?: string;
  completedAt?: string;
  subtotal: number;
  vatAmount: number;
  total: number;
  items: WorkOrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface WorkOrderItemRequest {
  type: WorkOrderItemType;
  description: string;
  quantity: number;
  unitPrice: number;
  vatRate: number;
}

export interface WorkOrderRequest {
  clientId: string;
  vehicleId: string;
  appointmentId?: string;
  mileageIn?: number;
  mileageOut?: number;
  description: string;
  technicianNotes?: string;
  customerNotes?: string;
  items: WorkOrderItemRequest[];
}
