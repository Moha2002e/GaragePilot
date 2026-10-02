package be.garagepilot.service;

import be.garagepilot.dto.workorder.WorkOrderItemRequest;
import be.garagepilot.dto.workorder.WorkOrderItemResponse;
import be.garagepilot.dto.workorder.WorkOrderRequest;
import be.garagepilot.dto.workorder.WorkOrderResponse;
import be.garagepilot.exception.BusinessException;
import be.garagepilot.exception.ResourceNotFoundException;
import be.garagepilot.model.Appointment;
import be.garagepilot.model.Client;
import be.garagepilot.model.Vehicle;
import be.garagepilot.model.WorkOrder;
import be.garagepilot.model.WorkOrderItem;
import be.garagepilot.model.WorkOrderStatus;
import be.garagepilot.repository.AppointmentRepository;
import be.garagepilot.repository.GarageRepository;
import be.garagepilot.repository.VehicleRepository;
import be.garagepilot.repository.WorkOrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkOrderService {

  private static final int MONEY_SCALE = 2;
  private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> ALLOWED_TRANSITIONS =
      createAllowedTransitions();

  private final WorkOrderRepository workOrders;
  private final VehicleRepository vehicles;
  private final AppointmentRepository appointments;
  private final GarageRepository garages;
  private final ClientService clients;

  public WorkOrderService(
      WorkOrderRepository workOrders,
      VehicleRepository vehicles,
      AppointmentRepository appointments,
      GarageRepository garages,
      ClientService clients) {
    this.workOrders = workOrders;
    this.vehicles = vehicles;
    this.appointments = appointments;
    this.garages = garages;
    this.clients = clients;
  }

  @Transactional(readOnly = true)
  public Page<WorkOrderResponse> findAll(
      UUID garageId, String search, WorkOrderStatus status, Pageable pageable) {
    return workOrders
        .search(garageId, normalizeSearch(search), status, pageable)
        .map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public WorkOrderResponse findById(UUID garageId, UUID id) {
    return toResponse(findEntity(garageId, id));
  }

  public WorkOrderResponse create(UUID garageId, WorkOrderRequest request) {
    WorkOrder workOrder = new WorkOrder();
    workOrder.setGarage(garages.getReferenceById(garageId));
    workOrder.setStatus(WorkOrderStatus.DRAFT);
    apply(workOrder, request, garageId);
    return toResponse(workOrders.save(workOrder));
  }

  public WorkOrderResponse update(UUID garageId, UUID id, WorkOrderRequest request) {
    WorkOrder workOrder = findEntity(garageId, id);
    if (EnumSet.of(WorkOrderStatus.INVOICED, WorkOrderStatus.CANCELLED)
        .contains(workOrder.getStatus())) {
      throw new BusinessException("An invoiced or cancelled work order cannot be modified");
    }
    workOrder.getItems().clear();
    apply(workOrder, request, garageId);
    return toResponse(workOrders.save(workOrder));
  }

  public WorkOrderResponse updateStatus(UUID garageId, UUID id, WorkOrderStatus nextStatus) {
    WorkOrder workOrder = findEntity(garageId, id);
    WorkOrderStatus currentStatus = workOrder.getStatus();
    if (currentStatus == nextStatus) {
      return toResponse(workOrder);
    }
    if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(nextStatus)) {
      throw new BusinessException(
          "Work order status cannot change from " + currentStatus + " to " + nextStatus);
    }
    workOrder.setStatus(nextStatus);
    if (nextStatus == WorkOrderStatus.IN_PROGRESS && workOrder.getStartedAt() == null) {
      workOrder.setStartedAt(Instant.now());
    }
    if (nextStatus == WorkOrderStatus.COMPLETED) {
      validateMileage(workOrder.getMileageIn(), workOrder.getMileageOut());
      workOrder.setCompletedAt(Instant.now());
      updateVehicleMileage(workOrder);
    }
    return toResponse(workOrders.save(workOrder));
  }

  WorkOrder findEntity(UUID garageId, UUID id) {
    return workOrders
        .findByIdAndGarageId(id, garageId)
        .orElseThrow(() -> new ResourceNotFoundException("Work order not found"));
  }

  private void apply(WorkOrder workOrder, WorkOrderRequest request, UUID garageId) {
    Client client = clients.findEntity(garageId, request.clientId());
    Vehicle vehicle =
        vehicles
            .findByIdAndGarageId(request.vehicleId(), garageId)
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
    if (!vehicle.getClient().getId().equals(client.getId())) {
      throw new BusinessException("The vehicle does not belong to the selected client");
    }
    validateMileage(request.mileageIn(), request.mileageOut());
    validateAppointment(request.appointmentId(), garageId, client, vehicle);

    workOrder.setClient(client);
    workOrder.setVehicle(vehicle);
    workOrder.setAppointmentId(request.appointmentId());
    workOrder.setMileageIn(request.mileageIn());
    workOrder.setMileageOut(request.mileageOut());
    workOrder.setDescription(request.description().trim());
    workOrder.setTechnicianNotes(request.technicianNotes());
    workOrder.setCustomerNotes(request.customerNotes());
    request.items().forEach(item -> workOrder.addItem(createItem(item, workOrder)));
  }

  private WorkOrderItem createItem(WorkOrderItemRequest request, WorkOrder workOrder) {
    WorkOrderItem item = new WorkOrderItem();
    item.setGarage(workOrder.getGarage());
    item.setType(request.type());
    item.setDescription(request.description().trim());
    item.setQuantity(request.quantity());
    item.setUnitPrice(money(request.unitPrice()));
    item.setVatRate(request.vatRate());
    item.setTotalExcludingVat(money(request.quantity().multiply(request.unitPrice())));
    return item;
  }

  private void validateAppointment(
      UUID appointmentId, UUID garageId, Client client, Vehicle vehicle) {
    if (appointmentId == null) {
      return;
    }
    Appointment appointment =
        appointments
            .findByIdAndGarageId(appointmentId, garageId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    if (!appointment.getClient().getId().equals(client.getId())
        || !appointment.getVehicle().getId().equals(vehicle.getId())) {
      throw new BusinessException("The appointment does not match the selected client and vehicle");
    }
  }

  private void validateMileage(Long mileageIn, Long mileageOut) {
    if (mileageIn != null && mileageOut != null && mileageOut < mileageIn) {
      throw new BusinessException("Mileage out cannot be lower than mileage in");
    }
  }

  private void updateVehicleMileage(WorkOrder workOrder) {
    Long mileage =
        workOrder.getMileageOut() == null ? workOrder.getMileageIn() : workOrder.getMileageOut();
    Vehicle vehicle = workOrder.getVehicle();
    if (mileage != null && (vehicle.getMileage() == null || mileage > vehicle.getMileage())) {
      vehicle.setMileage(mileage);
    }
  }

  private WorkOrderResponse toResponse(WorkOrder workOrder) {
    BigDecimal subtotal =
        workOrder.getItems().stream()
            .map(WorkOrderItem::getTotalExcludingVat)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal vatAmount =
        workOrder.getItems().stream()
            .map(
                item ->
                    item.getTotalExcludingVat()
                        .multiply(item.getVatRate())
                        .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.HALF_UP))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    Client client = workOrder.getClient();
    Vehicle vehicle = workOrder.getVehicle();
    return new WorkOrderResponse(
        workOrder.getId(),
        client.getId(),
        clientName(client),
        vehicle.getId(),
        vehicle.getLicensePlate(),
        vehicle.getBrand() + " " + vehicle.getModel(),
        workOrder.getAppointmentId(),
        workOrder.getStatus(),
        workOrder.getMileageIn(),
        workOrder.getMileageOut(),
        workOrder.getDescription(),
        workOrder.getTechnicianNotes(),
        workOrder.getCustomerNotes(),
        workOrder.getStartedAt(),
        workOrder.getCompletedAt(),
        money(subtotal),
        money(vatAmount),
        money(subtotal.add(vatAmount)),
        workOrder.getItems().stream().map(this::toItemResponse).toList(),
        workOrder.getCreatedAt(),
        workOrder.getUpdatedAt());
  }

  private WorkOrderItemResponse toItemResponse(WorkOrderItem item) {
    return new WorkOrderItemResponse(
        item.getId(),
        item.getType(),
        item.getDescription(),
        item.getQuantity(),
        item.getUnitPrice(),
        item.getVatRate(),
        item.getTotalExcludingVat());
  }

  private String clientName(Client client) {
    if (client.getCompanyName() != null && !client.getCompanyName().isBlank()) {
      return client.getCompanyName();
    }
    return String.join(
            " ",
            client.getFirstName() == null ? "" : client.getFirstName(),
            client.getLastName() == null ? "" : client.getLastName())
        .trim();
  }

  private BigDecimal money(BigDecimal value) {
    return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
  }

  private String normalizeSearch(String search) {
    return search == null ? "" : search.trim();
  }

  private static Map<WorkOrderStatus, Set<WorkOrderStatus>> createAllowedTransitions() {
    Map<WorkOrderStatus, Set<WorkOrderStatus>> transitions = new EnumMap<>(WorkOrderStatus.class);
    transitions.put(
        WorkOrderStatus.DRAFT,
        EnumSet.of(
            WorkOrderStatus.WAITING_FOR_APPROVAL,
            WorkOrderStatus.APPROVED,
            WorkOrderStatus.IN_PROGRESS,
            WorkOrderStatus.CANCELLED));
    transitions.put(
        WorkOrderStatus.WAITING_FOR_APPROVAL,
        EnumSet.of(WorkOrderStatus.APPROVED, WorkOrderStatus.CANCELLED));
    transitions.put(
        WorkOrderStatus.APPROVED,
        EnumSet.of(
            WorkOrderStatus.IN_PROGRESS,
            WorkOrderStatus.WAITING_FOR_PARTS,
            WorkOrderStatus.CANCELLED));
    transitions.put(
        WorkOrderStatus.IN_PROGRESS,
        EnumSet.of(
            WorkOrderStatus.WAITING_FOR_PARTS,
            WorkOrderStatus.COMPLETED,
            WorkOrderStatus.CANCELLED));
    transitions.put(
        WorkOrderStatus.WAITING_FOR_PARTS,
        EnumSet.of(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.CANCELLED));
    transitions.put(WorkOrderStatus.COMPLETED, EnumSet.of(WorkOrderStatus.INVOICED));
    return transitions;
  }
}
