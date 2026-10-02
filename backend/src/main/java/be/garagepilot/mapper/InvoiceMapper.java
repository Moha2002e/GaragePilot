package be.garagepilot.mapper;

import be.garagepilot.dto.invoice.InvoiceItemResponse;
import be.garagepilot.dto.invoice.InvoiceResponse;
import be.garagepilot.model.Client;
import be.garagepilot.model.Invoice;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {

  public InvoiceResponse toResponse(Invoice invoice) {
    return new InvoiceResponse(
        invoice.getId(),
        invoice.getGarage().getName(),
        invoice.getGarage().getVatNumber(),
        invoice.getGarage().getEmail(),
        invoice.getGarage().getPhone(),
        address(
            invoice.getGarage().getStreet(),
            invoice.getGarage().getPostalCode(),
            invoice.getGarage().getCity()),
        invoice.getClient().getId(),
        clientName(invoice.getClient()),
        invoice.getClient().getVatNumber(),
        invoice.getClient().getEmail(),
        invoice.getClient().getPhone(),
        address(
            invoice.getClient().getStreet(),
            invoice.getClient().getPostalCode(),
            invoice.getClient().getCity()),
        invoice.getVehicle().getId(),
        invoice.getVehicle().getLicensePlate(),
        vehicleDescription(invoice),
        invoice.getVehicle().getVin(),
        invoice.getVehicle().getMileage(),
        invoice.getWorkOrderId(),
        invoice.getQuoteId(),
        invoice.getInvoiceNumber(),
        invoice.getStatus(),
        invoice.getIssueDate(),
        invoice.getDueDate(),
        invoice.getSubtotal(),
        invoice.getVatAmount(),
        invoice.getTotal(),
        invoice.getAmountPaid(),
        invoice.getBalanceDue(),
        invoice.getItems().stream()
            .map(
                item ->
                    new InvoiceItemResponse(
                        item.getId(),
                        item.getType(),
                        item.getDescription(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getVatRate(),
                        item.getTotalExcludingVat()))
            .toList(),
        invoice.getCreatedAt(),
        invoice.getUpdatedAt());
  }

  private String vehicleDescription(Invoice invoice) {
    String version =
        invoice.getVehicle().getVersion() == null ? "" : " " + invoice.getVehicle().getVersion();
    return invoice.getVehicle().getBrand() + " " + invoice.getVehicle().getModel() + version;
  }

  private String address(String street, String postalCode, String city) {
    String firstLine = street == null ? "" : street.trim();
    String secondLine =
        String.join(" ", postalCode == null ? "" : postalCode, city == null ? "" : city).trim();
    if (firstLine.isBlank()) return secondLine;
    if (secondLine.isBlank()) return firstLine;
    return firstLine + ", " + secondLine;
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
}
