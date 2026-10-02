package be.garagepilot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import be.garagepilot.dto.invoice.InvoiceItemRequest;
import be.garagepilot.dto.invoice.InvoiceRequest;
import be.garagepilot.dto.invoice.InvoiceResponse;
import be.garagepilot.exception.ResourceNotFoundException;
import be.garagepilot.mapper.InvoiceMapper;
import be.garagepilot.model.Client;
import be.garagepilot.model.Garage;
import be.garagepilot.model.Invoice;
import be.garagepilot.model.InvoiceItemType;
import be.garagepilot.model.Vehicle;
import be.garagepilot.repository.GarageRepository;
import be.garagepilot.repository.InvoiceRepository;
import be.garagepilot.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvoiceServiceTest {

  @Test
  void doesNotReturnAnInvoiceOwnedByAnotherGarage() {
    InvoiceRepository invoices = mock(InvoiceRepository.class);
    VehicleRepository vehicles = mock(VehicleRepository.class);
    GarageRepository garages = mock(GarageRepository.class);
    ClientService clients = mock(ClientService.class);
    InvoiceMapper mapper = mock(InvoiceMapper.class);
    InvoiceService service = new InvoiceService(invoices, vehicles, garages, clients, mapper);
    UUID garageAId = UUID.randomUUID();
    UUID invoiceFromGarageBId = UUID.randomUUID();

    when(invoices.findByIdAndGarageId(invoiceFromGarageBId, garageAId))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> service.findById(garageAId, invoiceFromGarageBId));

    verify(invoices).findByIdAndGarageId(invoiceFromGarageBId, garageAId);
    verifyNoInteractions(vehicles, garages, clients, mapper);
  }

  @Test
  void calculatesInvoiceAmountsOnTheBackend() {
    InvoiceRepository invoices = mock(InvoiceRepository.class);
    VehicleRepository vehicles = mock(VehicleRepository.class);
    GarageRepository garages = mock(GarageRepository.class);
    ClientService clients = mock(ClientService.class);
    InvoiceService service =
        new InvoiceService(invoices, vehicles, garages, clients, new InvoiceMapper());

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
    when(clients.findEntity(garageId, clientId)).thenReturn(client);
    when(vehicles.findByIdAndGarageId(vehicleId, garageId)).thenReturn(Optional.of(vehicle));
    when(garages.getReferenceById(garageId)).thenReturn(garage);
    when(invoices.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

    InvoiceRequest request =
        new InvoiceRequest(
            clientId,
            vehicleId,
            null,
            null,
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            List.of(
                new InvoiceItemRequest(
                    InvoiceItemType.LABOR,
                    "Main-d'œuvre",
                    new BigDecimal("2"),
                    new BigDecimal("50"),
                    new BigDecimal("21"))));

    InvoiceResponse response = service.create(garageId, request);

    assertEquals(new BigDecimal("100.00"), response.subtotal());
    assertEquals(new BigDecimal("21.00"), response.vatAmount());
    assertEquals(new BigDecimal("121.00"), response.total());
    assertEquals(new BigDecimal("121.00"), response.balanceDue());
  }
}
