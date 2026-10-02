export interface DashboardData {
  appointmentsToday: number;
  vehiclesInWorkshop: number;
  quotesWaitingApproval: number;
  unpaidInvoices: number;
  revenueThisMonth: number;
  revenuePreviousMonth: number;
  revenueChangePercent: number;
  outstandingTotal: number;
  monthlyFinancials: MonthlyFinancialPoint[];
  invoiceStatuses: StatusSummary[];
  workshopPipeline: StatusSummary[];
}

export interface MonthlyFinancialPoint {
  month: string;
  invoiced: number;
  collected: number;
}

export interface StatusSummary {
  status: string;
  count: number;
  amount: number;
}
