package be.garagepilot.repository;

import be.garagepilot.model.Vehicle;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
  Optional<Vehicle> findByIdAndGarageId(UUID id, UUID garageId);

  @Query(
      """
      select v from Vehicle v
      where v.garage.id = :garageId
        and v.active = true
        and (:clientId is null or v.client.id = :clientId)
        and (
          :search = ''
          or lower(v.licensePlate) like lower(concat('%', :search, '%'))
          or lower(coalesce(v.vin, '')) like lower(concat('%', :search, '%'))
          or lower(v.brand) like lower(concat('%', :search, '%'))
          or lower(v.model) like lower(concat('%', :search, '%'))
        )
      """)
  Page<Vehicle> searchActive(
      @Param("garageId") UUID garageId,
      @Param("search") String search,
      @Param("clientId") UUID clientId,
      Pageable pageable);

  boolean existsByGarageIdAndLicensePlateIgnoreCase(UUID garageId, String plate);

  long countByGarageId(UUID garageId);

  @Query(
      "select v.garage.id, count(v) from Vehicle v where v.garage.id in :garageIds group by v.garage.id")
  List<Object[]> countByGarageIds(@Param("garageIds") Collection<UUID> garageIds);
}
