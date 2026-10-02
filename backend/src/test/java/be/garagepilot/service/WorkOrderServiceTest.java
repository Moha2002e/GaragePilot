package be.garagepilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import be.garagepilot.dto.workorder.WorkOrderItemRequest;
import be.garagepilot.dto.workorder.WorkOrderRequest;
import be.garagepilot.dto.workorder.WorkOrderResponse;
import be.garagepilot.exception.ResourceNotFoundException;
import be.garagepilot.model.Client;
import be.garagepilot.model.Garage;
import be.garagepilot.model.Vehicle;
import be.garagepilot.model.WorkOrder;
import be.garagepilot.model.WorkOrderItemType;
import be.garagepilot.repository.AppointmentRepository;
import be.garagepilot.repository.GarageRepository;
import be.garagepilot.repository.VehicleRepository;
import be.garagepilot.repository.WorkOrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkOrderServiceTest {

  @Test
  void doesNotReturnAWorkOrderOwnedByAnotherGarage() {
    WorkOrderRepository workOrders = mock(WorkOrderRepository.class);
    VehicleRepository vehicles = mock(VehicleRepository.class);
    AppointmentRepository appointments = mock(AppointmentRepository.class);
    GarageRepository garages = mock(GarageRepository.class);
    ClientService clients = mock(ClientService.class);
    WorkOrderService service =
        new WorkOrderService(workOrders, vehicles, appointments, garages, clients);
    UUID garageAId = UUID.randomUUID();
    UUID workOrderFromGarageBId = UUID.randomUUID();

    when(workOrders.findByIdAndGarageId(workOrderFromGarageBId, garageAId))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> service.findById(garageAId, workOrderFromGarageBId));

    verify(workOrders).findByIdAndGarageId(workOrderFromGarageBId, garageAId);
    verifyNoInteractions(vehicles, appointments, garages, clients);
  }

  @Test
  void calculatesWorkOrderAmountsOnTheBackend() {
    WorkOrderRepository workOrders = mock(WorkOrderRepository.class);
    VehicleRepository vehicles = mock(VehicleRepository.class);
    AppointmentRepository appointments = mock(AppointmentRepository.class);
    GarageRepository garages = mock(GarageRepository.class);
    ClientService clients = mock(ClientService.class);
    WorkOrderService service =
        new WorkOrderService(workOrders, vehicles, appointments, garages, clients);

    UUID garageId = UUID.randomUUID();
    UUID clientId = UUID.randomUUID();
    UUID vehicleId = UUID.randomUUID();
    Client client = mock(Client.class);
    Vehicle vehicle = mock(Vehicle.class);
    Garage garage = mock(Garage.class);

    when(client.getId()).thenReturn(clientId);
    when(client.getFirstName()).thenReturn("Sophie");
    when(client.getLastName()).thenReturn("Dubois");
    when(vehicle.getId()).thenReturn(vehicleId);
    when(vehicle.getClient()).thenReturn(client);
    when(vehicle.getLicensePlate()).thenReturn("1-ABC-123");
    when(vehicle.getBrand()).thenReturn("Peugeot");
    when(vehicle.getModel()).thenReturn("308");
    when(clients.findEntity(garageId, clientId)).thenReturn(client);
    when(vehicles.findByIdAndGarageId(vehicleId, garageId)).thenReturn(Optional.of(vehicle));
    when(garages.getReferenceById(garageId)).thenReturn(garage);
    when(workOrders.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

    WorkOrderRequest request =
        new WorkOrderRequest(
            clientId,
            vehicleId,
            null,
            120000L,
            null,
            "Entretien complet",
            null,
            null,
            List.of(
                new WorkOrderItemRequest(
                    WorkOrderItemType.LABOR,
                    "Main-d'œuvre",
                    new BigDecimal("2"),
                    new BigDecimal("60"),
                    new BigDecimal("21")),
                new WorkOrderItemRequest(
                    WorkOrderItemType.PART,
                    "Filtre à huile",
                    BigDecimal.ONE,
                    new BigDecimal("18"),
                    new BigDecimal("21"))));

    WorkOrderResponse response = service.create(garageId, request);

    assertEquals(new BigDecimal("138.00"), response.subtotal());
    assertEquals(new BigDecimal("28.98"), response.vatAmount());
    assertEquals(new BigDecimal("166.98"), response.total());
  }
}
