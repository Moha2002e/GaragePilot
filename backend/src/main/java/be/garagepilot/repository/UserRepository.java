package be.garagepilot.repository;

import be.garagepilot.model.User;
import java.util.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmailIgnoreCase(String email);

  @EntityGraph(attributePaths = "garage")
  Optional<User> findSecurityUserById(UUID id);

  boolean existsByEmailIgnoreCase(String email);

  Optional<User> findByIdAndGarageId(UUID id, UUID garageId);

  List<User> findByGarageIdOrderByLastNameAscFirstNameAsc(UUID garageId);

  long countByGarageId(UUID garageId);

  @Query(
      "select u.garage.id, count(u) from User u where u.garage.id in :garageIds group by u.garage.id")
  List<Object[]> countByGarageIds(@Param("garageIds") Collection<UUID> garageIds);
}
