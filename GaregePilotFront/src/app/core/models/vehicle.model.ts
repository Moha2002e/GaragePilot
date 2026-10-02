export type FuelType = 'PETROL' | 'DIESEL' | 'HYBRID' | 'ELECTRIC' | 'LPG' | 'OTHER';

export interface Vehicle {
  id: string;
  clientId: string;
  licensePlate: string;
  vin?: string;
  brand: string;
  model: string;
  mileage?: number;
  fuelType?: FuelType;
  active: boolean;
}
