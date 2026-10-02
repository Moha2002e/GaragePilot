package be.garagepilot.repository;

import be.garagepilot.model.WorkOrder;
import be.garagepilot.model.WorkOrderStatus;
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

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {

  @EntityGraph(attributePaths = {"client", "vehicle"})
  @Query(
      """
      select w from WorkOrder w
      where w.garage.id = :garageId
        and (:status is null or w.status = :status)
        and (
          :search = ''
          or lower(w.vehicle.licensePlate) like lower(concat('%', :search, '%'))
          or lower(coalesce(w.client.firstName, '')) like lower(concat('%', :search, '%'))
          or lower(coalesce(w.client.lastName, '')) like lower(concat('%', :search, '%'))
          or lower(coalesce(w.client.companyName, '')) like lower(concat('%', :search, '%'))
          or lower(w.description) like lower(concat('%', :search, '%'))
        )
      """)
  Page<WorkOrder> search(
      @Param("garageId") UUID garageId,
      @Param("search") String search,
      @Param("status") WorkOrderStatus status,
      Pageable pageable);

  @EntityGraph(attributePaths = {"client", "vehicle", "items"})
  Optional<WorkOrder> findByIdAndGarageId(UUID id, UUID garageId);

  long countByGarageIdAndStatusIn(UUID garageId, Collection<WorkOrderStatus> statuses);

  @Query(
      "select w.status, count(w) from WorkOrder w where w.garage.id = :garageId group by w.status")
  List<Object[]> countByStatus(@Param("garageId") UUID garageId);
}
