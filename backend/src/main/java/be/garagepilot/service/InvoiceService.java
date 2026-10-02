package be.garagepilot.service;

import be.garagepilot.dto.invoice.InvoiceItemRequest;
import be.garagepilot.dto.invoice.InvoiceRequest;
import be.garagepilot.dto.invoice.InvoiceResponse;
import be.garagepilot.exception.BusinessException;
import be.garagepilot.exception.ResourceNotFoundException;
import be.garagepilot.mapper.InvoiceMapper;
import be.garagepilot.model.Client;
import be.garagepilot.model.Invoice;
import be.garagepilot.model.InvoiceItem;
import be.garagepilot.model.InvoiceStatus;
import be.garagepilot.model.Vehicle;
import be.garagepilot.repository.GarageRepository;
import be.garagepilot.repository.InvoiceRepository;
import be.garagepilot.repository.VehicleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InvoiceService {

  private static final int MONEY_SCALE = 2;

  private final InvoiceRepository invoices;
  private final VehicleRepository vehicles;
  private final GarageRepository garages;
  private final ClientService clientService;
  private final InvoiceMapper mapper;

  public InvoiceService(
      InvoiceRepository invoices,
      VehicleRepository vehicles,
      GarageRepository garages,
      ClientService clientService,
      InvoiceMapper mapper) {
    this.invoices = invoices;
    this.vehicles = vehicles;
    this.garages = garages;
    this.clientService = clientService;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public Page<InvoiceResponse> findAll(
      UUID garageId, String search, InvoiceStatus status, Pageable pageable) {
    return invoices
        .search(garageId, normalizeSearch(search), status, pageable)
        .map(mapper::toResponse);
  }

  @Transactional(readOnly = true)
  public InvoiceResponse findById(UUID garageId, UUID invoiceId) {
    return mapper.toResponse(findEntity(garageId, invoiceId));
  }

  public InvoiceResponse create(UUID garageId, InvoiceRequest request) {
    validateDates(request);
    Invoice invoice = new Invoice();
    invoice.setGarage(garages.getReferenceById(garageId));
    invoice.setInvoiceNumber(generateNumber());
    invoice.setStatus(InvoiceStatus.DRAFT);
    apply(invoice, request, garageId);

    return mapper.toResponse(invoices.save(invoice));
  }

  public InvoiceResponse update(UUID garageId, UUID invoiceId, InvoiceRequest request) {
    validateDates(request);
    Invoice invoice = findEntity(garageId, invoiceId);
    if (invoice.getStatus() != InvoiceStatus.DRAFT) {
      throw new BusinessException("Only a draft invoice can be modified");
    }

    invoice.getItems().clear();
    apply(invoice, request, garageId);
    return mapper.toResponse(invoices.save(invoice));
  }

  Invoice findEntity(UUID garageId, UUID invoiceId) {
    return invoices
        .findByIdAndGarageId(invoiceId, garageId)
        .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
  }

  private InvoiceItem createItem(InvoiceItemRequest request) {
    InvoiceItem item = new InvoiceItem();
    item.setType(request.type());
    item.setDescription(request.description().trim());
    item.setQuantity(request.quantity());
    item.setUnitPrice(money(request.unitPrice()));
    item.setVatRate(request.vatRate());
    item.setTotalExcludingVat(money(request.quantity().multiply(request.unitPrice())));
    return item;
  }

  private void apply(Invoice invoice, InvoiceRequest request, UUID garageId) {
    Client client = clientService.findEntity(garageId, request.clientId());
    Vehicle vehicle =
        vehicles
            .findByIdAndGarageId(request.vehicleId(), garageId)
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

    if (!vehicle.getClient().getId().equals(client.getId())) {
      throw new BusinessException("The vehicle does not belong to the selected client");
    }

    invoice.setClient(client);
    invoice.setVehicle(vehicle);
    invoice.setWorkOrderId(request.workOrderId());
    invoice.setQuoteId(request.quoteId());
    invoice.setIssueDate(request.issueDate());
    invoice.setDueDate(request.dueDate());
    request.items().forEach(itemRequest -> invoice.addItem(createItem(itemRequest)));
    calculateTotals(invoice);
  }

  private void calculateTotals(Invoice invoice) {
    BigDecimal subtotal =
        invoice.getItems().stream()
            .map(InvoiceItem::getTotalExcludingVat)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal vatAmount =
        invoice.getItems().stream()
            .map(
                item ->
                    item.getTotalExcludingVat()
                        .multiply(item.getVatRate())
                        .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.HALF_UP))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    invoice.setSubtotal(money(subtotal));
    invoice.setVatAmount(money(vatAmount));
    invoice.setTotal(money(subtotal.add(vatAmount)));
    invoice.setAmountPaid(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
    invoice.setBalanceDue(invoice.getTotal());
  }

  private void validateDates(InvoiceRequest request) {
    if (request.dueDate() != null && request.dueDate().isBefore(request.issueDate())) {
      throw new BusinessException("The due date cannot be before the issue date");
    }
  }

  private String generateNumber() {
    return "FAC-"
        + Year.now().getValue()
        + "-"
        + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  private BigDecimal money(BigDecimal value) {
    return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
  }

  private String normalizeSearch(String search) {
    return search == null ? "" : search.trim();
  }
}
