package be.garagepilot.service;

import be.garagepilot.dto.appointment.AppointmentRequest;
import be.garagepilot.dto.appointment.AppointmentResponse;
import be.garagepilot.exception.BusinessException;
import be.garagepilot.exception.ResourceNotFoundException;
import be.garagepilot.model.Appointment;
import be.garagepilot.model.AppointmentStatus;
import be.garagepilot.model.Client;
import be.garagepilot.model.Vehicle;
import be.garagepilot.repository.AppointmentRepository;
import be.garagepilot.repository.GarageRepository;
import be.garagepilot.repository.VehicleRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppointmentService {

  private final AppointmentRepository appointments;
  private final VehicleRepository vehicles;
  private final GarageRepository garages;
  private final ClientService clients;

  public AppointmentService(
      AppointmentRepository appointments,
      VehicleRepository vehicles,
      GarageRepository garages,
      ClientService clients) {
    this.appointments = appointments;
    this.vehicles = vehicles;
    this.garages = garages;
    this.clients = clients;
  }

  @Transactional(readOnly = true)
  public List<AppointmentResponse> findRange(
      UUID garageId, OffsetDateTime from, OffsetDateTime to) {
    if (!from.isBefore(to)) {
      throw new BusinessException("The start of the period must be before its end");
    }
    return appointments
        .findByGarageIdAndStartDateTimeLessThanAndEndDateTimeGreaterThanOrderByStartDateTimeAsc(
            garageId, to, from)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public AppointmentResponse findById(UUID garageId, UUID id) {
    return toResponse(findEntity(garageId, id));
  }

  public AppointmentResponse create(UUID garageId, AppointmentRequest request) {
    Appointment appointment = new Appointment();
    appointment.setGarage(garages.getReferenceById(garageId));
    apply(appointment, request, garageId);
    return toResponse(appointments.save(appointment));
  }

  public AppointmentResponse update(UUID garageId, UUID id, AppointmentRequest request) {
    Appointment appointment = findEntity(garageId, id);
    apply(appointment, request, garageId);
    return toResponse(appointments.save(appointment));
  }

  public AppointmentResponse updateStatus(UUID garageId, UUID id, AppointmentStatus status) {
    Appointment appointment = findEntity(garageId, id);
    appointment.setStatus(status);
    return toResponse(appointments.save(appointment));
  }

  private Appointment findEntity(UUID garageId, UUID id) {
    return appointments
        .findByIdAndGarageId(id, garageId)
        .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
  }

  private void apply(Appointment appointment, AppointmentRequest request, UUID garageId) {
    if (!request.startDateTime().isBefore(request.endDateTime())) {
      throw new BusinessException("The appointment end must be after its start");
    }

    Client client = clients.findEntity(garageId, request.clientId());
    Vehicle vehicle =
        vehicles
            .findByIdAndGarageId(request.vehicleId(), garageId)
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
    if (!vehicle.getClient().getId().equals(client.getId())) {
      throw new BusinessException("The vehicle does not belong to the selected client");
    }

    appointment.setClient(client);
    appointment.setVehicle(vehicle);
    appointment.setStartDateTime(request.startDateTime());
    appointment.setEndDateTime(request.endDateTime());
    appointment.setReason(request.reason().trim());
    appointment.setNotes(request.notes());
    appointment.setStatus(request.status() == null ? AppointmentStatus.PLANNED : request.status());
  }

  private AppointmentResponse toResponse(Appointment appointment) {
    Client client = appointment.getClient();
    Vehicle vehicle = appointment.getVehicle();
    String clientName =
        client.getCompanyName() == null || client.getCompanyName().isBlank()
            ? String.join(
                    " ",
                    client.getFirstName() == null ? "" : client.getFirstName(),
                    client.getLastName() == null ? "" : client.getLastName())
                .trim()
            : client.getCompanyName();
    return new AppointmentResponse(
        appointment.getId(),
        client.getId(),
        clientName,
        vehicle.getId(),
        vehicle.getLicensePlate(),
        vehicle.getBrand() + " " + vehicle.getModel(),
        appointment.getStartDateTime(),
        appointment.getEndDateTime(),
        appointment.getReason(),
        appointment.getNotes(),
        appointment.getStatus());
  }
}
