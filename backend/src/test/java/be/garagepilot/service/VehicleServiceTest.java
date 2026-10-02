package be.garagepilot.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import be.garagepilot.exception.ResourceNotFoundException;
import be.garagepilot.repository.GarageRepository;
import be.garagepilot.repository.VehicleRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VehicleServiceTest {

  @Test
  void doesNotReturnAVehicleOwnedByAnotherGarage() {
    VehicleRepository vehicles = mock(VehicleRepository.class);
    GarageRepository garages = mock(GarageRepository.class);
    ClientService clients = mock(ClientService.class);
    VehicleService service = new VehicleService(vehicles, garages, clients);
    UUID garageAId = UUID.randomUUID();
    UUID vehicleFromGarageBId = UUID.randomUUID();

    when(vehicles.findByIdAndGarageId(vehicleFromGarageBId, garageAId))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> service.findById(garageAId, vehicleFromGarageBId));

    verify(vehicles).findByIdAndGarageId(vehicleFromGarageBId, garageAId);
    verifyNoInteractions(garages, clients);
  }
}
