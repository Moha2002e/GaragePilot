package be.garagepilot.repository;

import be.garagepilot.model.GarageService;
import be.garagepilot.model.ServiceCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GarageServiceRepository extends JpaRepository<GarageService, UUID> {
  List<GarageService> findByGarageIdOrderByCategoryAscNameAsc(UUID garageId);

  @Query(
      """
      select service from GarageService service
      where service.garage.id = :garageId
        and service.active = true
        and (:category is null or service.category = :category)
        and (
          :search = ''
          or lower(service.name) like lower(concat('%', :search, '%'))
          or lower(coalesce(service.description, '')) like lower(concat('%', :search, '%'))
        )
      order by service.category asc, service.name asc
      """)
  List<GarageService> searchActive(
      @Param("garageId") UUID garageId,
      @Param("search") String search,
      @Param("category") ServiceCategory category);

  Optional<GarageService> findByIdAndGarageId(UUID id, UUID garageId);

  boolean existsByGarageIdAndNameIgnoreCase(UUID garageId, String name);
}
