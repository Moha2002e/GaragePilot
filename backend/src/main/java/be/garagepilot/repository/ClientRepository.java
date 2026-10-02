package be.garagepilot.repository;

import be.garagepilot.model.Client;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, UUID> {
  Optional<Client> findByIdAndGarageId(UUID id, UUID garageId);

  @Query(
      """
    select c from Client c
    where (:garageId is null or c.garage.id = :garageId)
      and c.active = true
      and (
        :search = ''
        or lower(coalesce(c.firstName, '')) like lower(concat('%', :search, '%'))
        or lower(coalesce(c.lastName, '')) like lower(concat('%', :search, '%'))
        or lower(coalesce(c.companyName, '')) like lower(concat('%', :search, '%'))
        or lower(coalesce(c.email, '')) like lower(concat('%', :search, '%'))
        or lower(coalesce(c.phone, '')) like lower(concat('%', :search, '%'))
        or lower(coalesce(c.city, '')) like lower(concat('%', :search, '%'))
      )
    """)
  Page<Client> searchActive(
      @Param("garageId") UUID garageId, @Param("search") String search, Pageable pageable);

  long countByGarageId(UUID garageId);

  @Query(
      "select c.garage.id, count(c) from Client c where c.garage.id in :garageIds group by c.garage.id")
  List<Object[]> countByGarageIds(@Param("garageIds") Collection<UUID> garageIds);
}
