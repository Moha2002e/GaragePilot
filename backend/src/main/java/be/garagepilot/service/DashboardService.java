package be.garagepilot.service;

import be.garagepilot.dto.dashboard.DashboardResponse;
import be.garagepilot.dto.dashboard.DashboardResponse.MonthlyFinancialPoint;
import be.garagepilot.dto.dashboard.DashboardResponse.StatusSummary;
import be.garagepilot.model.InvoiceStatus;
import be.garagepilot.model.QuoteStatus;
import be.garagepilot.model.WorkOrderStatus;
import be.garagepilot.repository.AppointmentRepository;
import be.garagepilot.repository.InvoiceRepository;
import be.garagepilot.repository.QuoteRepository;
import be.garagepilot.repository.WorkOrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

  private static final ZoneId GARAGE_ZONE = ZoneId.of("Europe/Brussels");
  private static final DateTimeFormatter MONTH_LABEL =
      DateTimeFormatter.ofPattern("MMM", Locale.FRENCH);
  private static final EnumSet<InvoiceStatus> UNPAID_STATUSES =
      EnumSet.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.OVERDUE);
  private static final EnumSet<WorkOrderStatus> ACTIVE_WORK_ORDERS =
      EnumSet.of(
          WorkOrderStatus.APPROVED, WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.WAITING_FOR_PARTS);

  private final AppointmentRepository appointments;
  private final WorkOrderRepository workOrders;
  private final QuoteRepository quotes;
  private final InvoiceRepository invoices;

  public DashboardService(
      AppointmentRepository appointments,
      WorkOrderRepository workOrders,
      QuoteRepository quotes,
      InvoiceRepository invoices) {
    this.appointments = appointments;
    this.workOrders = workOrders;
    this.quotes = quotes;
    this.invoices = invoices;
  }

  public DashboardResponse get(UUID garageId) {
    LocalDate today = LocalDate.now(GARAGE_ZONE);
    OffsetDateTime startToday = today.atStartOfDay(GARAGE_ZONE).toOffsetDateTime();
    OffsetDateTime startTomorrow = today.plusDays(1).atStartOfDay(GARAGE_ZONE).toOffsetDateTime();
    YearMonth currentMonth = YearMonth.from(today);
    YearMonth previousMonth = currentMonth.minusMonths(1);
    BigDecimal currentRevenue =
        invoices.sumTotal(garageId, currentMonth.atDay(1), currentMonth.plusMonths(1).atDay(1));
    BigDecimal previousRevenue =
        invoices.sumTotal(garageId, previousMonth.atDay(1), previousMonth.plusMonths(1).atDay(1));

    return new DashboardResponse(
        appointments.countByGarageIdAndStartDateTimeGreaterThanEqualAndStartDateTimeLessThan(
            garageId, startToday, startTomorrow),
        workOrders.countByGarageIdAndStatusIn(garageId, ACTIVE_WORK_ORDERS),
        quotes.countByGarageIdAndStatusIn(garageId, EnumSet.of(QuoteStatus.SENT)),
        invoices.countByGarageIdAndStatusIn(garageId, UNPAID_STATUSES),
        money(currentRevenue),
        money(previousRevenue),
        changePercent(currentRevenue, previousRevenue),
        money(invoices.sumBalanceDue(garageId, UNPAID_STATUSES)),
        monthlyFinancials(garageId, currentMonth),
        invoiceStatuses(garageId),
        workshopPipeline(garageId));
  }

  private List<MonthlyFinancialPoint> monthlyFinancials(UUID garageId, YearMonth currentMonth) {
    YearMonth firstMonth = currentMonth.minusMonths(5);
    Map<YearMonth, Object[]> values = new HashMap<>();
    for (Object[] row : invoices.summarizeMonthly(garageId, firstMonth.atDay(1))) {
      YearMonth month = YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
      values.put(month, row);
    }

    List<MonthlyFinancialPoint> result = new ArrayList<>();
    for (int index = 0; index < 6; index++) {
      YearMonth month = firstMonth.plusMonths(index);
      Object[] row = values.get(month);
      result.add(
          new MonthlyFinancialPoint(
              capitalize(month.format(MONTH_LABEL)),
              row == null ? BigDecimal.ZERO : money((BigDecimal) row[2]),
              row == null ? BigDecimal.ZERO : money((BigDecimal) row[3])));
    }
    return result;
  }

  private List<StatusSummary> invoiceStatuses(UUID garageId) {
    return invoices.summarizeByStatus(garageId).stream()
        .map(
            row ->
                new StatusSummary(
                    ((InvoiceStatus) row[0]).name(),
                    ((Number) row[1]).longValue(),
                    money((BigDecimal) row[2])))
        .toList();
  }

  private List<StatusSummary> workshopPipeline(UUID garageId) {
    return workOrders.countByStatus(garageId).stream()
        .map(
            row ->
                new StatusSummary(
                    ((WorkOrderStatus) row[0]).name(),
                    ((Number) row[1]).longValue(),
                    BigDecimal.ZERO))
        .toList();
  }

  private BigDecimal changePercent(BigDecimal current, BigDecimal previous) {
    if (previous == null || previous.signum() == 0) {
      return current == null || current.signum() == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100);
    }
    return current
        .subtract(previous)
        .multiply(BigDecimal.valueOf(100))
        .divide(previous, 1, RoundingMode.HALF_UP);
  }

  private BigDecimal money(BigDecimal value) {
    return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
  }

  private String capitalize(String value) {
    return value.substring(0, 1).toUpperCase(Locale.FRENCH) + value.substring(1);
  }
}
