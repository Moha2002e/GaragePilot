package be.garagepilot.repository;

import be.garagepilot.model.Invoice;
import be.garagepilot.model.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

  @EntityGraph(attributePaths = {"garage", "client", "vehicle"})
  @Query(
      """
      select i from Invoice i
      where i.garage.id = :garageId
        and (:status is null or i.status = :status)
        and (
          :search = ''
          or lower(i.invoiceNumber) like lower(concat('%', :search, '%'))
          or lower(coalesce(i.client.firstName, '')) like lower(concat('%', :search, '%'))
          or lower(coalesce(i.client.lastName, '')) like lower(concat('%', :search, '%'))
          or lower(coalesce(i.client.companyName, '')) like lower(concat('%', :search, '%'))
          or lower(i.vehicle.licensePlate) like lower(concat('%', :search, '%'))
        )
      """)
  Page<Invoice> search(
      @Param("garageId") UUID garageId,
      @Param("search") String search,
      @Param("status") InvoiceStatus status,
      Pageable pageable);

  @EntityGraph(attributePaths = {"garage", "client", "vehicle"})
  Page<Invoice> findByGarageId(UUID garageId, Pageable pageable);

  @EntityGraph(attributePaths = {"garage", "client", "vehicle", "items"})
  Optional<Invoice> findByIdAndGarageId(UUID id, UUID garageId);

  long countByGarageIdAndStatusIn(UUID garageId, Collection<InvoiceStatus> statuses);

  @Query(
      "select coalesce(sum(i.total), 0) from Invoice i where i.garage.id = :garageId and i.issueDate >= :from and i.issueDate < :to and i.status <> be.garagepilot.model.InvoiceStatus.CANCELLED")
  BigDecimal sumTotal(
      @Param("garageId") UUID garageId, @Param("from") LocalDate from, @Param("to") LocalDate to);

  @Query(
      "select coalesce(sum(i.balanceDue), 0) from Invoice i where i.garage.id = :garageId and i.status in :statuses")
  BigDecimal sumBalanceDue(
      @Param("garageId") UUID garageId, @Param("statuses") Collection<InvoiceStatus> statuses);

  @Query(
      "select i.status, count(i), coalesce(sum(i.total), 0) from Invoice i where i.garage.id = :garageId group by i.status")
  List<Object[]> summarizeByStatus(@Param("garageId") UUID garageId);

  @Query(
      "select year(i.issueDate), month(i.issueDate), coalesce(sum(i.total), 0), coalesce(sum(i.amountPaid), 0) from Invoice i where i.garage.id = :garageId and i.issueDate >= :from and i.status <> be.garagepilot.model.InvoiceStatus.CANCELLED group by year(i.issueDate), month(i.issueDate) order by year(i.issueDate), month(i.issueDate)")
  List<Object[]> summarizeMonthly(@Param("garageId") UUID garageId, @Param("from") LocalDate from);

  long countByGarageId(UUID garageId);

  @Query(
      "select coalesce(sum(i.total), 0) from Invoice i where i.garage.id = :garageId and i.status <> be.garagepilot.model.InvoiceStatus.CANCELLED")
  BigDecimal sumLifetimeRevenue(@Param("garageId") UUID garageId);

  @Query(
      "select coalesce(sum(i.total), 0) from Invoice i where i.status <> be.garagepilot.model.InvoiceStatus.CANCELLED")
  BigDecimal sumPlatformRevenue();

  @Query("select coalesce(sum(i.balanceDue), 0) from Invoice i where i.status in :statuses")
  BigDecimal sumPlatformBalanceDue(@Param("statuses") Collection<InvoiceStatus> statuses);

  @Query(
      """
      select i.garage.id,
             count(i),
             coalesce(sum(case when i.status <> be.garagepilot.model.InvoiceStatus.CANCELLED then i.total else 0 end), 0),
             coalesce(sum(case when i.status in :openStatuses then i.balanceDue else 0 end), 0)
      from Invoice i
      where i.garage.id in :garageIds
      group by i.garage.id
      """)
  List<Object[]> summarizeByGarageIds(
      @Param("garageIds") Collection<UUID> garageIds,
      @Param("openStatuses") Collection<InvoiceStatus> openStatuses);
}
