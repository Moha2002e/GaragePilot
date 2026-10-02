package be.garagepilot.service;

import be.garagepilot.model.Invoice;
import be.garagepilot.model.InvoiceItem;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoicePdfService {

  private static final Color DARK = new Color(16, 24, 40);
  private static final Color GREEN = new Color(22, 122, 91);
  private static final Color LIGHT = new Color(242, 244, 247);
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final InvoiceService invoiceService;

  public InvoicePdfService(InvoiceService invoiceService) {
    this.invoiceService = invoiceService;
  }

  @Transactional(readOnly = true)
  public byte[] generate(UUID garageId, UUID invoiceId) {
    Invoice invoice = invoiceService.findEntity(garageId, invoiceId);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 42, 42, 42, 42);

    PdfWriter.getInstance(document, output);
    document.addTitle("Facture " + invoice.getInvoiceNumber());
    document.addAuthor(invoice.getGarage().getName());
    document.open();

    addHeader(document, invoice);
    addParties(document, invoice);
    addVehicle(document, invoice);
    addItems(document, invoice);
    addTotals(document, invoice);
    addFooter(document);

    document.close();
    return output.toByteArray();
  }

  private void addHeader(Document document, Invoice invoice) {
    PdfPTable header = new PdfPTable(new float[] {3, 2});
    header.setWidthPercentage(100);
    header.setSpacingAfter(24);

    PdfPCell garage = cell(false);
    garage.addElement(new Paragraph(invoice.getGarage().getName(), font(20, Font.BOLD, DARK)));
    garage.addElement(new Paragraph("Gestion automobile", font(9, Font.NORMAL, GREEN)));
    header.addCell(garage);

    PdfPCell title = cell(false);
    title.setHorizontalAlignment(Element.ALIGN_RIGHT);
    Paragraph invoiceTitle = new Paragraph("FACTURE", font(22, Font.BOLD, DARK));
    invoiceTitle.setAlignment(Element.ALIGN_RIGHT);
    title.addElement(invoiceTitle);
    Paragraph number = new Paragraph(invoice.getInvoiceNumber(), font(10, Font.BOLD, GREEN));
    number.setAlignment(Element.ALIGN_RIGHT);
    title.addElement(number);
    header.addCell(title);

    document.add(header);
  }

  private void addParties(Document document, Invoice invoice) {
    PdfPTable parties = new PdfPTable(new float[] {1, 1});
    parties.setWidthPercentage(100);
    parties.setSpacingAfter(18);

    parties.addCell(
        infoBox(
            "GARAGE",
            invoice.getGarage().getName(),
            invoice.getGarage().getStreet(),
            joinPostalCity(invoice.getGarage().getPostalCode(), invoice.getGarage().getCity()),
            "TVA : " + value(invoice.getGarage().getVatNumber()),
            value(invoice.getGarage().getEmail()),
            value(invoice.getGarage().getPhone())));

    String clientName = clientName(invoice);
    parties.addCell(
        infoBox(
            "FACTURÉ À",
            clientName,
            invoice.getClient().getStreet(),
            joinPostalCity(invoice.getClient().getPostalCode(), invoice.getClient().getCity()),
            invoice.getClient().getVatNumber() == null
                ? null
                : "TVA : " + invoice.getClient().getVatNumber(),
            invoice.getClient().getEmail(),
            invoice.getClient().getPhone()));

    document.add(parties);
  }

  private void addVehicle(Document document, Invoice invoice) {
    PdfPTable details = new PdfPTable(new float[] {1, 1, 1, 1});
    details.setWidthPercentage(100);
    details.setSpacingAfter(22);

    addDetail(details, "Date", invoice.getIssueDate().format(DATE_FORMAT));
    addDetail(
        details,
        "Échéance",
        invoice.getDueDate() == null ? "-" : invoice.getDueDate().format(DATE_FORMAT));
    addDetail(
        details,
        "Véhicule",
        invoice.getVehicle().getBrand() + " " + invoice.getVehicle().getModel());
    addDetail(details, "Plaque", invoice.getVehicle().getLicensePlate());
    addDetail(details, "VIN", value(invoice.getVehicle().getVin()));
    addDetail(
        details,
        "Kilométrage",
        invoice.getVehicle().getMileage() == null
            ? "-"
            : invoice.getVehicle().getMileage() + " km");
    addDetail(details, "Statut", statusLabel(invoice));
    addDetail(details, "Devise", "EUR");

    document.add(details);
  }

  private void addItems(Document document, Invoice invoice) {
    PdfPTable table = new PdfPTable(new float[] {4.2f, 0.8f, 1.2f, 0.8f, 1.3f});
    table.setWidthPercentage(100);
    table.setHeaderRows(1);
    table.setSpacingAfter(18);

    addHeaderCell(table, "Description", Element.ALIGN_LEFT);
    addHeaderCell(table, "Qté", Element.ALIGN_RIGHT);
    addHeaderCell(table, "Prix unit.", Element.ALIGN_RIGHT);
    addHeaderCell(table, "TVA", Element.ALIGN_RIGHT);
    addHeaderCell(table, "Montant", Element.ALIGN_RIGHT);

    for (InvoiceItem item : invoice.getItems()) {
      addBodyCell(table, item.getDescription(), Element.ALIGN_LEFT);
      addBodyCell(table, decimal(item.getQuantity()), Element.ALIGN_RIGHT);
      addBodyCell(table, money(item.getUnitPrice()), Element.ALIGN_RIGHT);
      addBodyCell(table, decimal(item.getVatRate()) + " %", Element.ALIGN_RIGHT);
      addBodyCell(table, money(item.getTotalExcludingVat()), Element.ALIGN_RIGHT);
    }

    document.add(table);
  }

  private void addTotals(Document document, Invoice invoice) {
    PdfPTable wrapper = new PdfPTable(new float[] {3, 2});
    wrapper.setWidthPercentage(100);
    wrapper.addCell(cell(false));

    PdfPTable totals = new PdfPTable(new float[] {1.4f, 1});
    addTotalRow(totals, "Sous-total", invoice.getSubtotal(), false);
    addTotalRow(totals, "TVA", invoice.getVatAmount(), false);
    addTotalRow(totals, "Total TTC", invoice.getTotal(), true);
    addTotalRow(totals, "Déjà payé", invoice.getAmountPaid(), false);
    addTotalRow(totals, "Solde dû", invoice.getBalanceDue(), true);
    wrapper.addCell(new PdfPCell(totals));

    document.add(wrapper);
  }

  private void addFooter(Document document) {
    Paragraph footer =
        new Paragraph(
            "Merci pour votre confiance. Veuillez mentionner le numéro de facture lors du paiement.",
            font(9, Font.NORMAL, Color.DARK_GRAY));
    footer.setSpacingBefore(28);
    footer.setAlignment(Element.ALIGN_CENTER);
    document.add(footer);
  }

  private PdfPCell infoBox(String title, String... lines) {
    PdfPCell box = cell(true);
    box.setPadding(12);
    box.addElement(new Paragraph(title, font(8, Font.BOLD, GREEN)));
    for (String line : lines) {
      if (line != null && !line.isBlank()) {
        box.addElement(new Paragraph(line, font(9, Font.NORMAL, DARK)));
      }
    }
    return box;
  }

  private void addDetail(PdfPTable table, String label, String value) {
    PdfPCell cell = cell(false);
    cell.setPadding(6);
    cell.addElement(new Paragraph(label.toUpperCase(), font(7, Font.BOLD, Color.GRAY)));
    cell.addElement(new Paragraph(value, font(9, Font.BOLD, DARK)));
    table.addCell(cell);
  }

  private void addHeaderCell(PdfPTable table, String text, int alignment) {
    PdfPCell cell = new PdfPCell(new Phrase(text, font(8, Font.BOLD, Color.WHITE)));
    cell.setBackgroundColor(DARK);
    cell.setPadding(8);
    cell.setHorizontalAlignment(alignment);
    table.addCell(cell);
  }

  private void addBodyCell(PdfPTable table, String text, int alignment) {
    PdfPCell cell = new PdfPCell(new Phrase(text, font(8, Font.NORMAL, DARK)));
    cell.setBorderColor(new Color(228, 231, 236));
    cell.setPadding(8);
    cell.setHorizontalAlignment(alignment);
    table.addCell(cell);
  }

  private void addTotalRow(PdfPTable table, String label, BigDecimal amount, boolean emphasized) {
    Font font = font(emphasized ? 10 : 9, emphasized ? Font.BOLD : Font.NORMAL, DARK);
    PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
    PdfPCell valueCell = new PdfPCell(new Phrase(money(amount), font));
    labelCell.setBackgroundColor(emphasized ? LIGHT : Color.WHITE);
    valueCell.setBackgroundColor(emphasized ? LIGHT : Color.WHITE);
    labelCell.setPadding(7);
    valueCell.setPadding(7);
    valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    labelCell.setBorderColor(new Color(228, 231, 236));
    valueCell.setBorderColor(new Color(228, 231, 236));
    table.addCell(labelCell);
    table.addCell(valueCell);
  }

  private PdfPCell cell(boolean border) {
    PdfPCell cell = new PdfPCell();
    cell.setBorder(border ? PdfPCell.BOX : PdfPCell.NO_BORDER);
    cell.setBorderColor(new Color(228, 231, 236));
    return cell;
  }

  private Font font(float size, int style, Color color) {
    return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
  }

  private String clientName(Invoice invoice) {
    if (invoice.getClient().getCompanyName() != null
        && !invoice.getClient().getCompanyName().isBlank()) {
      return invoice.getClient().getCompanyName();
    }
    return (value(invoice.getClient().getFirstName())
            + " "
            + value(invoice.getClient().getLastName()))
        .trim();
  }

  private String joinPostalCity(String postalCode, String city) {
    return (value(postalCode) + " " + value(city)).trim();
  }

  private String value(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }

  private String money(BigDecimal value) {
    DecimalFormat format =
        new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.FRANCE));
    return format.format(value.setScale(2, RoundingMode.HALF_UP)) + " EUR";
  }

  private String decimal(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }

  private String statusLabel(Invoice invoice) {
    return switch (invoice.getStatus()) {
      case DRAFT -> "Brouillon";
      case ISSUED -> "Émise";
      case PARTIALLY_PAID -> "Partiellement payée";
      case PAID -> "Payée";
      case OVERDUE -> "En retard";
      case CANCELLED -> "Annulée";
    };
  }
}
