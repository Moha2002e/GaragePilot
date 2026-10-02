package be.garagepilot.repository;

import be.garagepilot.model.Quote;
import be.garagepilot.model.QuoteStatus;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {
  long countByGarageIdAndStatusIn(UUID garageId, Collection<QuoteStatus> statuses);
}
