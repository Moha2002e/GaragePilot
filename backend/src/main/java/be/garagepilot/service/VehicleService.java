package be.garagepilot.service;

import be.garagepilot.dto.vehicle.*;
import be.garagepilot.exception.*;
import be.garagepilot.model.*;
import be.garagepilot.repository.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VehicleService {
  private final VehicleRepository vehicles;
  private final GarageRepository garages;
  private final ClientService clientService;

  public VehicleService(
      VehicleRepository vehicles, GarageRepository garages, ClientService clientService) {
    this.vehicles = vehicles;
    this.garages = garages;
    this.clientService = clientService;
  }

  @Transactional(readOnly = true)
  public Page<VehicleResponse> findAll(
      UUID garageId, String search, UUID clientId, Pageable pageable) {
    return vehicles
        .searchActive(garageId, normalizeSearch(search), clientId, pageable)
        .map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public VehicleResponse findById(UUID garageId, UUID id) {
    return toResponse(findEntity(garageId, id));
  }

  public VehicleResponse create(UUID garageId, VehicleRequest request) {
    String plate = normalizePlate(request.licensePlate());
    if (vehicles.existsByGarageIdAndLicensePlateIgnoreCase(garageId, plate))
      throw new BusinessException("A vehicle with this license plate already exists");
    Vehicle vehicle = new Vehicle();
    vehicle.setGarage(garages.getReferenceById(garageId));
    apply(vehicle, request, garageId, plate);
    return toResponse(vehicles.save(vehicle));
  }

  public VehicleResponse update(UUID garageId, UUID id, VehicleRequest request) {
    Vehicle vehicle = findEntity(garageId, id);
    String plate = normalizePlate(request.licensePlate());
    if (!vehicle.getLicensePlate().equalsIgnoreCase(plate)
        && vehicles.existsByGarageIdAndLicensePlateIgnoreCase(garageId, plate))
      throw new BusinessException("A vehicle with this license plate already exists");
    apply(vehicle, request, garageId, plate);
    return toResponse(vehicles.save(vehicle));
  }

  public void deactivate(UUID garageId, UUID id) {
    Vehicle vehicle = findEntity(garageId, id);
    vehicle.deactivate();
    vehicles.save(vehicle);
  }

  private Vehicle findEntity(UUID garageId, UUID id) {
    return vehicles
        .findByIdAndGarageId(id, garageId)
        .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
  }

  private void apply(Vehicle v, VehicleRequest r, UUID garageId, String plate) {
    v.setClient(clientService.findEntity(garageId, r.clientId()));
    v.setLicensePlate(plate);
    v.setVin(r.vin());
    v.setBrand(r.brand());
    v.setModel(r.model());
    v.setVersion(r.version());
    v.setFuelType(r.fuelType());
    v.setFirstRegistrationDate(r.firstRegistrationDate());
    v.setMileage(r.mileage());
    v.setColor(r.color());
    v.setNotes(r.notes());
  }

  private VehicleResponse toResponse(Vehicle v) {
    return new VehicleResponse(
        v.getId(),
        v.getClient().getId(),
        v.getLicensePlate(),
        v.getVin(),
        v.getBrand(),
        v.getModel(),
        v.getVersion(),
        v.getFuelType(),
        v.getFirstRegistrationDate(),
        v.getMileage(),
        v.getColor(),
        v.getNotes(),
        v.isActive(),
        v.getCreatedAt(),
        v.getUpdatedAt());
  }

  private String normalizePlate(String plate) {
    return plate.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
  }

  private String normalizeSearch(String search) {
    return search == null ? "" : search.trim();
  }
}
