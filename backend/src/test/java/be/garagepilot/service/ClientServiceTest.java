package be.garagepilot.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import be.garagepilot.exception.ResourceNotFoundException;
import be.garagepilot.repository.ClientRepository;
import be.garagepilot.repository.GarageRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

  @Mock private ClientRepository clientRepository;

  @Mock private GarageRepository garageRepository;

  @Test
  void doesNotReturnAClientOwnedByAnotherGarage() {
    UUID garageAId = UUID.randomUUID();
    UUID clientFromGarageBId = UUID.randomUUID();
    ClientService service = new ClientService(clientRepository, garageRepository);

    when(clientRepository.findByIdAndGarageId(clientFromGarageBId, garageAId))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> service.findById(garageAId, clientFromGarageBId));

    verify(clientRepository).findByIdAndGarageId(clientFromGarageBId, garageAId);
  }
}
