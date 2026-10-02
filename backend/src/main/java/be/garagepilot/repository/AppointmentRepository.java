package be.garagepilot.repository;

import be.garagepilot.model.Appointment;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

  @EntityGraph(attributePaths = {"client", "vehicle"})
  List<Appointment>
      findByGarageIdAndStartDateTimeLessThanAndEndDateTimeGreaterThanOrderByStartDateTimeAsc(
          UUID garageId, OffsetDateTime to, OffsetDateTime from);

  @EntityGraph(attributePaths = {"client", "vehicle"})
  Optional<Appointment> findByIdAndGarageId(UUID id, UUID garageId);

  long countByGarageIdAndStartDateTimeGreaterThanEqualAndStartDateTimeLessThan(
      UUID garageId, OffsetDateTime from, OffsetDateTime to);
}
