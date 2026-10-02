package be.garagepilot.repository;

import be.garagepilot.model.Garage;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GarageRepository extends JpaRepository<Garage, UUID> {

  @Query(
      """
      select g from Garage g
      where :search = ''
        or lower(g.name) like lower(concat('%', :search, '%'))
        or lower(coalesce(g.vatNumber, '')) like lower(concat('%', :search, '%'))
        or lower(g.email) like lower(concat('%', :search, '%'))
        or lower(coalesce(g.city, '')) like lower(concat('%', :search, '%'))
      """)
  Page<Garage> search(@Param("search") String search, Pageable pageable);

  long countByActiveTrue();
}
