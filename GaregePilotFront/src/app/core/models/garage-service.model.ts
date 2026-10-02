export type ServiceCategory =
  'MAINTENANCE' | 'MECHANICAL' | 'TIRES' | 'DIAGNOSTIC' | 'BODYWORK' | 'ELECTRICAL' | 'OTHER';

export interface GarageService {
  id: string;
  name: string;
  category: ServiceCategory;
  description?: string;
  durationMinutes: number;
  price: number;
  vatRate: number;
  color?: string;
  active: boolean;
}

export type GarageServiceRequest = Omit<GarageService, 'id' | 'active'>;
