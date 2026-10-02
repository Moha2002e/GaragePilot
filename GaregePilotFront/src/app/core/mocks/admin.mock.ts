import {
  AdminGarageDetail,
  AdminGarageSummary,
  AdminInvoice,
  AdminOverview,
  AdminUser,
  AdminUserRequest,
} from '../models/admin.model';

export const ADMIN_MOCK_ENABLED = true;

const garageNames = [
  'Garage Demo GaragePilot',
  'Atelier du Centre',
  'Garage Meuse Auto',
  'Mécanique des Hautes Fagnes',
  'Auto Service Hesbaye',
  'Garage du Condroz',
  'Atelier Mosan',
  'Auto Technique Liège',
  'Garage des Ardennes',
  'Mécanique du Pays de Herve',
];

const cities = [
  'Liège',
  'Liège',
  'Seraing',
  'Verviers',
  'Waremme',
  'Huy',
  'Visé',
  'Herstal',
  'Spa',
  'Herve',
];

const firstNames = ['Sophie', 'Thomas', 'Nadia', 'Julien', 'Emma', 'Lucas', 'Sarah', 'Mehdi'];
const lastNames = [
  'Dubois',
  'Lambert',
  'Benali',
  'Simon',
  'Leclercq',
  'Martin',
  'Jacobs',
  'Renard',
];
function isoDate(dayIndex: number): string {
  const year = new Date().getFullYear();
  const date = new Date(Date.UTC(year, 0, 1));
  date.setUTCDate(date.getUTCDate() + Math.round((dayIndex * 364) / 99));
  return date.toISOString();
}

function createInvoice(garageIndex: number, localIndex: number): AdminInvoice {
  const index = garageIndex * 10 + localIndex;
  const annualDayIndex = Math.round((localIndex * 99) / 9);
  const amount = 420 + ((index * 137) % 1700);
  const outstanding = index % 9 === 0 ? amount : index % 7 === 0 ? amount / 2 : 0;
  const status = outstanding === amount ? 'OVERDUE' : outstanding > 0 ? 'PARTIALLY_PAID' : 'PAID';

  return {
    id: `mock-invoice-${index + 1}`,
    invoiceNumber: `FAC-${new Date().getFullYear()}-${String(index + 1).padStart(4, '0')}`,
    clientName: `${firstNames[index % firstNames.length]} ${lastNames[index % lastNames.length]}`,
    licensePlate: `GP-${String(index + 1).padStart(3, '0')}-D`,
    issueDate: isoDate(annualDayIndex),
    total: amount,
    balanceDue: outstanding,
    status,
  };
}

function createUsers(garageIndex: number): AdminUser[] {
  return [
    {
      id: `mock-user-${garageIndex + 1}-1`,
      firstName: firstNames[garageIndex % firstNames.length],
      lastName: lastNames[garageIndex % lastNames.length],
      email: `responsable${garageIndex + 1}@mock.garagepilot.be`,
      phone: `+32 470 10 ${String(garageIndex + 1).padStart(2, '0')}`,
      role: garageIndex === 0 ? 'SUPER_ADMIN' : 'OWNER',
      active: true,
      createdAt: isoDate(garageIndex),
    },
    {
      id: `mock-user-${garageIndex + 1}-2`,
      firstName: firstNames[(garageIndex + 3) % firstNames.length],
      lastName: lastNames[(garageIndex + 5) % lastNames.length],
      email: `atelier${garageIndex + 1}@mock.garagepilot.be`,
      phone: `+32 470 20 ${String(garageIndex + 1).padStart(2, '0')}`,
      role: garageIndex % 2 === 0 ? 'MECHANIC' : 'RECEPTION',
      active: garageIndex !== 8,
      createdAt: isoDate(garageIndex + 10),
    },
  ];
}

const details: AdminGarageDetail[] = garageNames.map((name, garageIndex) => ({
  id: `mock-garage-${garageIndex + 1}`,
  name,
  vatNumber: `BE0${String(760000001 + garageIndex).padStart(9, '0')}`,
  email: `contact${garageIndex + 1}@mock.garagepilot.be`,
  phone: `+32 4 220 ${String(garageIndex + 10).padStart(2, '0')} 00`,
  address: `${garageIndex + 10} rue de l’Atelier, ${4000 + garageIndex * 10} ${cities[garageIndex]}`,
  active: garageIndex !== 8,
  users: createUsers(garageIndex),
  recentInvoices: Array.from({ length: 10 }, (_, localIndex) =>
    createInvoice(garageIndex, localIndex),
  ).reverse(),
}));

function summary(detail: AdminGarageDetail, index: number): AdminGarageSummary {
  const revenue = detail.recentInvoices.reduce((total, invoice) => total + invoice.total, 0);
  const outstanding = detail.recentInvoices.reduce(
    (total, invoice) => total + invoice.balanceDue,
    0,
  );
  return {
    id: detail.id,
    name: detail.name,
    vatNumber: detail.vatNumber,
    email: detail.email,
    city: cities[index],
    active: detail.active,
    users: detail.users.length,
    clients: 10,
    vehicles: 10,
    invoices: detail.recentInvoices.length,
    revenue,
    outstanding,
    createdAt: isoDate(index * 8),
  };
}

export class AdminMockStore {
  overview(page: number, size: number, search: string): AdminOverview {
    const needle = search.trim().toLocaleLowerCase('fr-BE');
    const companies = details
      .map(summary)
      .filter((company) =>
        [company.name, company.vatNumber, company.email, company.city]
          .filter(Boolean)
          .some((value) => value!.toLocaleLowerCase('fr-BE').includes(needle)),
      );
    const start = page * size;
    const content = companies.slice(start, start + size);
    const summaries = details.map(summary);

    return {
      garages: summaries.length,
      activeGarages: summaries.filter((garage) => garage.active).length,
      users: summaries.reduce((total, garage) => total + garage.users, 0),
      clients: summaries.reduce((total, garage) => total + garage.clients, 0),
      vehicles: summaries.reduce((total, garage) => total + garage.vehicles, 0),
      invoices: summaries.reduce((total, garage) => total + garage.invoices, 0),
      platformRevenue: summaries.reduce((total, garage) => total + garage.revenue, 0),
      outstandingTotal: summaries.reduce((total, garage) => total + garage.outstanding, 0),
      companies: {
        content,
        totalElements: companies.length,
        totalPages: Math.ceil(companies.length / size),
        number: page,
        size,
        numberOfElements: content.length,
        first: page === 0,
        last: start + size >= companies.length,
      },
    };
  }

  garage(id: string): AdminGarageDetail {
    const garage = details.find((candidate) => candidate.id === id);
    if (!garage) throw new Error('Mock garage not found');
    return structuredClone(garage);
  }

  setGarageActive(id: string, active: boolean): void {
    const garage = details.find((candidate) => candidate.id === id);
    if (!garage) throw new Error('Mock garage not found');
    garage.active = active;
  }

  createUser(garageId: string, request: AdminUserRequest): AdminUser {
    const garage = details.find((candidate) => candidate.id === garageId);
    if (!garage) throw new Error('Mock garage not found');
    const user: AdminUser = {
      id: `mock-user-created-${Date.now()}`,
      firstName: request.firstName,
      lastName: request.lastName,
      email: request.email,
      phone: request.phone,
      role: request.role,
      active: true,
      createdAt: new Date().toISOString(),
    };
    garage.users.push(user);
    return structuredClone(user);
  }

  updateUser(userId: string, request: AdminUserRequest): AdminUser {
    const user = details
      .flatMap((garage) => garage.users)
      .find((candidate) => candidate.id === userId);
    if (!user) throw new Error('Mock user not found');
    Object.assign(user, {
      firstName: request.firstName,
      lastName: request.lastName,
      email: request.email,
      phone: request.phone,
      role: request.role,
    });
    return structuredClone(user);
  }

  setUserActive(userId: string, active: boolean): void {
    const user = details
      .flatMap((garage) => garage.users)
      .find((candidate) => candidate.id === userId);
    if (!user) throw new Error('Mock user not found');
    user.active = active;
  }
}
