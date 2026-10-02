package be.garagepilot.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
    long appointmentsToday,
    long vehiclesInWorkshop,
    long quotesWaitingApproval,
    long unpaidInvoices,
    BigDecimal revenueThisMonth,
    BigDecimal revenuePreviousMonth,
    BigDecimal revenueChangePercent,
    BigDecimal outstandingTotal,
    List<MonthlyFinancialPoint> monthlyFinancials,
    List<StatusSummary> invoiceStatuses,
    List<StatusSummary> workshopPipeline) {

  public record MonthlyFinancialPoint(String month, BigDecimal invoiced, BigDecimal collected) {}

  public record StatusSummary(String status, long count, BigDecimal amount) {}
}
